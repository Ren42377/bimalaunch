package com.ren42377.bimalaunch.clawd

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.SystemClock
import androidx.compose.ui.geometry.Rect
import java.io.Closeable
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.Tensor
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.gpu.GpuDelegate

class ClawdModelDetector(private val context: Context) : Closeable {

    data class Detection(val labelKey: String, val confidence: Float, val rect: Rect)

    data class Result(
        val boxes: Map<String, Rect>,
        val detections: List<Detection>,
        val latencyMs: Long,
        val runtime: ClawdRuntime,
        val usedCpuFallback: Boolean
    )

    private var holder: InterpreterHolder? = null

    @Synchronized
    fun detect(bitmap: Bitmap, config: ClawdDetectorConfig): Result {
        return runCatching {
            detectWithConfig(bitmap, config, false)
        }.getOrElse { error ->
            if (config.runtime != ClawdRuntime.GPU) throw error
            closeInterpreter()
            detectWithConfig(bitmap, config.copy(runtime = ClawdRuntime.CPU), true)
        }
    }

    @Synchronized
    override fun close() {
        closeInterpreter()
    }

    private fun detectWithConfig(bitmap: Bitmap, config: ClawdDetectorConfig, fallback: Boolean): Result {
        val startedAt = SystemClock.elapsedRealtime()
        val model = getInterpreter(config)
        val inputTensor = model.getInputTensor(0)
        val outputTensor = model.getOutputTensor(0)
        val inputLayout = TensorLayout.fromInputShape(inputTensor.shape())
        val outputLayout = TensorLayout.fromOutputShape(outputTensor.shape(), config.labels.size)
        val preparedInput = prepareInput(bitmap, inputLayout, inputTensor)
        val preparedOutput = tensorBufferFor(outputTensor, false)
        model.run(preparedInput.buffer, preparedOutput.buffer)
        preparedOutput.buffer.rewind()
        val detections = decodeOutput(preparedOutput, outputLayout, config, bitmap.width, bitmap.height, preparedInput.letterbox)
        val suppressed = detections.suppressGlobalDuplicates(config).keepSingleTargetLabels()
        val filtered = config.labels.mapNotNull { label ->
            suppressed.filter { it.labelKey == label }.maxByOrNull { it.confidence }?.let { label to it.rect }
        }.toMap()
        return Result(filtered, suppressed, SystemClock.elapsedRealtime() - startedAt, config.runtime, fallback)
    }

    private fun getInterpreter(config: ClawdDetectorConfig): Interpreter {
        holder?.takeIf { it.config == config }?.let { return it.interpreter }
        closeInterpreter()
        val delegate = createGpuDelegate(config)
        val options = Interpreter.Options().apply {
            if (delegate != null) {
                addDelegate(delegate)
            } else {
                setNumThreads(config.cpuThreads)
                setUseXNNPACK(true)
            }
        }
        val interpreter = Interpreter(readModelBuffer(config.modelAsset), options)
        holder = InterpreterHolder(config, interpreter, delegate)
        return interpreter
    }

    private fun createGpuDelegate(config: ClawdDetectorConfig): GpuDelegate? {
        if (config.runtime != ClawdRuntime.GPU) return null
        val compatibilityList = CompatibilityList()
        return try {
            if (compatibilityList.isDelegateSupportedOnThisDevice) {
                GpuDelegate(compatibilityList.bestOptionsForThisDevice)
            } else {
                null
            }
        } finally {
            compatibilityList.close()
        }
    }

    private fun readModelBuffer(assetPath: String): ByteBuffer {
        val bytes = context.assets.open(assetPath).use { it.readBytes() }
        val buffer = ByteBuffer.allocateDirect(bytes.size).order(ByteOrder.nativeOrder())
        buffer.put(bytes)
        buffer.rewind()
        return buffer
    }

    private fun prepareInput(bitmap: Bitmap, layout: TensorLayout, tensor: Tensor): PreparedInput {
        val inputBitmap = Bitmap.createBitmap(layout.width, layout.height, Bitmap.Config.ARGB_8888)
        val scale = min(layout.width.toFloat() / bitmap.width, layout.height.toFloat() / bitmap.height)
        val scaledWidth = bitmap.width * scale
        val scaledHeight = bitmap.height * scale
        val padX = (layout.width - scaledWidth) / 2f
        val padY = (layout.height - scaledHeight) / 2f
        val canvas = Canvas(inputBitmap)
        canvas.drawColor(Color.rgb(114, 114, 114))
        val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)
        canvas.drawBitmap(bitmap, null, RectF(padX, padY, padX + scaledWidth, padY + scaledHeight), paint)
        val pixels = IntArray(layout.width * layout.height)
        inputBitmap.getPixels(pixels, 0, layout.width, 0, 0, layout.width, layout.height)
        inputBitmap.recycle()
        val buffer = tensorBufferFor(tensor, true)
        buffer.buffer.clear()
        if (layout.channelsFirst) {
            for (channel in 0 until CHANNEL_COUNT) {
                for (pixel in pixels) buffer.put(pixel.channelValue(channel))
            }
        } else {
            for (pixel in pixels) {
                buffer.put(pixel.channelValue(0))
                buffer.put(pixel.channelValue(1))
                buffer.put(pixel.channelValue(2))
            }
        }
        buffer.buffer.rewind()
        return PreparedInput(buffer.buffer, Letterbox(layout.width, layout.height, scale, padX, padY))
    }

    private fun decodeOutput(
        output: TensorBuffer,
        layout: TensorLayout,
        config: ClawdDetectorConfig,
        sourceWidth: Int,
        sourceHeight: Int,
        letterbox: Letterbox
    ): List<Detection> {
        val raw = mutableListOf<Detection>()
        repeat(layout.anchorCount) { anchor ->
            val box = layout.readBox(output, anchor)
            val scores = FloatArray(config.labels.size) { index ->
                if (layout.channelsFirst) output.valueAt(0, index + 4, anchor) else output.valueAt(0, anchor, index + 4)
            }
            val classIndex = scores.indices.maxByOrNull { scores[it] } ?: return@repeat
            val confidence = scores[classIndex]
            if (confidence < config.confidenceThreshold) return@repeat
            val label = config.labels.getOrNull(classIndex) ?: return@repeat
            box.toSourceRect(sourceWidth, sourceHeight, letterbox)?.let { rect ->
                raw += Detection(label, confidence, rect)
            }
        }
        return raw.groupBy { it.labelKey }.values.flatMap { it.nms(config.nmsIouThreshold) }
    }

    private fun TensorLayout.readBox(output: TensorBuffer, anchor: Int): ModelBox {
        return if (channelsFirst) {
            ModelBox(output.valueAt(0, 0, anchor), output.valueAt(0, 1, anchor), output.valueAt(0, 2, anchor), output.valueAt(0, 3, anchor))
        } else {
            ModelBox(output.valueAt(0, anchor, 0), output.valueAt(0, anchor, 1), output.valueAt(0, anchor, 2), output.valueAt(0, anchor, 3))
        }
    }

    private fun ModelBox.toSourceRect(sourceWidth: Int, sourceHeight: Int, letterbox: Letterbox): Rect? {
        val normalized = centerX <= 2f && centerY <= 2f && width <= 2f && height <= 2f
        val scaledCenterX = if (normalized) centerX * letterbox.width else centerX
        val scaledCenterY = if (normalized) centerY * letterbox.height else centerY
        val scaledWidth = if (normalized) width * letterbox.width else width
        val scaledHeight = if (normalized) height * letterbox.height else height
        val left = ((scaledCenterX - scaledWidth / 2f) - letterbox.padX) / letterbox.scale
        val top = ((scaledCenterY - scaledHeight / 2f) - letterbox.padY) / letterbox.scale
        val right = ((scaledCenterX + scaledWidth / 2f) - letterbox.padX) / letterbox.scale
        val bottom = ((scaledCenterY + scaledHeight / 2f) - letterbox.padY) / letterbox.scale
        val rect = Rect(
            left = left.coerceIn(0f, sourceWidth.toFloat()),
            top = top.coerceIn(0f, sourceHeight.toFloat()),
            right = right.coerceIn(0f, sourceWidth.toFloat()),
            bottom = bottom.coerceIn(0f, sourceHeight.toFloat())
        )
        if (rect.width < 2f || rect.height < 2f) return null
        return rect
    }

    private fun List<Detection>.nms(iouThreshold: Float): List<Detection> {
        val sorted = sortedByDescending { it.confidence }.toMutableList()
        val kept = mutableListOf<Detection>()
        while (sorted.isNotEmpty()) {
            val current = sorted.removeAt(0)
            kept += current
            sorted.removeAll { current.rect.iou(it.rect) > iouThreshold }
        }
        return kept
    }

    private fun List<Detection>.keepSingleTargetLabels(): List<Detection> {
        return groupBy { it.labelKey }.flatMap { (labelKey, detections) ->
            if (labelKey in MULTI_TARGET_LABELS) detections else listOfNotNull(detections.maxByOrNull { it.confidence })
        }.sortedByDescending { it.confidence }
    }

    private fun List<Detection>.suppressGlobalDuplicates(config: ClawdDetectorConfig): List<Detection> {
        val kept = mutableListOf<Detection>()
        sortedByDescending { it.confidence }.forEach { candidate ->
            if (kept.none { it.rect.isNearlySameAs(candidate.rect) || it.rect.iou(candidate.rect) > config.globalDuplicateIouThreshold }) {
                kept += candidate
            }
        }
        return kept
    }

    private fun Rect.isNearlySameAs(other: Rect): Boolean {
        val centerDistanceX = abs(center.x - other.center.x)
        val centerDistanceY = abs(center.y - other.center.y)
        val maxWidth = max(width, other.width).coerceAtLeast(1f)
        val maxHeight = max(height, other.height).coerceAtLeast(1f)
        return centerDistanceX <= maxWidth * 0.12f &&
            centerDistanceY <= maxHeight * 0.12f &&
            abs(width - other.width) / maxWidth <= 0.24f &&
            abs(height - other.height) / maxHeight <= 0.24f
    }

    private fun Rect.iou(other: Rect): Float {
        val left = max(this.left, other.left)
        val top = max(this.top, other.top)
        val right = min(this.right, other.right)
        val bottom = min(this.bottom, other.bottom)
        val intersection = max(0f, right - left) * max(0f, bottom - top)
        val union = this.width * this.height + other.width * other.height - intersection
        if (union <= 0f) return 0f
        return intersection / union
    }

    private fun tensorBufferFor(tensor: Tensor, input: Boolean): TensorBuffer {
        val quantization = Quantization(tensor.quantizationParams().scale, tensor.quantizationParams().zeroPoint)
        return TensorBuffer(
            shape = tensor.shape(),
            dataType = tensor.dataType(),
            quantization = quantization,
            buffer = ByteBuffer.allocateDirect(tensor.numBytes()).order(ByteOrder.nativeOrder()),
            input = input
        )
    }

    private fun Int.channelValue(channel: Int): Float {
        val value = when (channel) {
            0 -> Color.red(this)
            1 -> Color.green(this)
            else -> Color.blue(this)
        }
        return value / 255f
    }

    private fun closeInterpreter() {
        holder?.delegate?.close()
        holder?.interpreter?.close()
        holder = null
    }

    private data class InterpreterHolder(val config: ClawdDetectorConfig, val interpreter: Interpreter, val delegate: GpuDelegate?)
    private data class PreparedInput(val buffer: ByteBuffer, val letterbox: Letterbox)
    private data class Letterbox(val width: Int, val height: Int, val scale: Float, val padX: Float, val padY: Float)
    private data class ModelBox(val centerX: Float, val centerY: Float, val width: Float, val height: Float)
    private data class Quantization(val scale: Float, val zeroPoint: Int)

    private class TensorBuffer(
        val shape: IntArray,
        val dataType: DataType,
        val quantization: Quantization,
        val buffer: ByteBuffer,
        val input: Boolean
    ) {
        fun put(value: Float) {
            when (dataType) {
                DataType.FLOAT32 -> buffer.putFloat(value)
                DataType.UINT8 -> {
                    val scale = quantization.scale.takeIf { it > 0f } ?: 1f
                    val raw = (value / scale + quantization.zeroPoint).toInt()
                    buffer.put(raw.coerceIn(0, 255).toByte())
                }
                DataType.INT8 -> {
                    val scale = quantization.scale.takeIf { it > 0f } ?: 1f
                    val raw = (value / scale + quantization.zeroPoint).toInt()
                    buffer.put(raw.coerceIn(-128, 127).toByte())
                }
                else -> buffer.putFloat(value)
            }
        }

        fun valueAt(index0: Int, index1: Int, index2: Int): Float {
            val bytes = dataType.bytes()
            val index = ((index0 * shape[1] * shape[2]) + (index1 * shape[2]) + index2) * bytes
            return when (dataType) {
                DataType.FLOAT32 -> buffer.getFloat(index)
                DataType.UINT8 -> ((buffer.get(index).toInt() and 0xFF) - quantization.zeroPoint) * quantization.scale
                DataType.INT8 -> (buffer.get(index).toInt() - quantization.zeroPoint) * quantization.scale
                else -> buffer.getFloat(index)
            }
        }
    }

    private data class TensorLayout(
        val width: Int,
        val height: Int,
        val channelsFirst: Boolean,
        val anchorCount: Int = 0
    ) {
        companion object {
            fun fromInputShape(shape: IntArray): TensorLayout {
                return if (shape[1] == CHANNEL_COUNT) {
                    TensorLayout(width = shape[3], height = shape[2], channelsFirst = true)
                } else {
                    TensorLayout(width = shape[2], height = shape[1], channelsFirst = false)
                }
            }

            fun fromOutputShape(shape: IntArray, classCount: Int): TensorLayout {
                val channelsFirst = shape[1] == classCount + 4
                val anchorCount = if (channelsFirst) shape[2] else shape[1]
                return TensorLayout(width = 0, height = 0, channelsFirst = channelsFirst, anchorCount = anchorCount)
            }
        }
    }

    private companion object {
        const val CHANNEL_COUNT = 3
        val MULTI_TARGET_LABELS = setOf(
            "question_list_button",
            "question_number",
            "question_number_answered",
            "question_number_unanswered",
            "question_number_current"
        )
    }
}

private fun DataType.bytes(): Int {
    return when (this) {
        DataType.FLOAT32 -> 4
        DataType.INT32 -> 4
        DataType.UINT8 -> 1
        DataType.INT8 -> 1
        DataType.INT64 -> 8
        DataType.BOOL -> 1
        else -> 4
    }
}
