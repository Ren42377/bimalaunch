package com.ren42377.bimalaunch.practice

import android.content.Context
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.OutputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object PracticeLocalServer {
    private const val ASSET_ROOT = "practice_site"
    private const val RUNTIME_ROOT = "practice_site"
    private const val VERSION_FILE = "site-version.txt"
    private const val DEFAULT_PORT = 8080
    private const val MAX_BODY_BYTES = 10 * 1024 * 1024

    private val lock = Any()
    private var activeServer: LocalHttpServer? = null

    fun start(context: Context): String {
        synchronized(lock) {
            activeServer?.let { if (it.isRunning) return it.baseUrl }
            val root = prepareSite(context)
            val server = openServer(root)
            activeServer = server
            server.start()
            return server.baseUrl
        }
    }

    fun stop() {
        synchronized(lock) {
            activeServer?.stop()
            activeServer = null
        }
    }

    fun applyConfig(context: Context, config: PracticeConfig) {
        val root = prepareSite(context)
        val db = PracticeDatabase.loadDb(root)
        val normalized = config.normalized()
        val code = normalized.subjectCode

        val questions = questionsToSiteJson(normalized.questions)
        val mapelConfig = JSONObject().apply {
            put("id", code)
            put("kode", code)
            put("tanggal", if (normalized.testDate != "0000-00-00") normalized.testDate else "")
            put("waktu", if (normalized.testTime != "00:00") normalized.testTime else "")
            put("nama", normalized.subject)
            put("subtest", normalized.subtest.ifBlank { normalized.subject })
            put("alokasi", normalized.durationMinutes.toString())
            put("jumlahsoal", normalized.questions.size.toString())
            put("dikerjakan", normalized.questions.size.toString())
            put("status", "Aktif")
            put("shuffle", "0")
            put("shuffle2", "0")
            put("grouping", JSONObject())
            put("locking", JSONObject())
            put("autotoken", if (normalized.useToken) 1 else 0)
            put("token", normalized.sessionToken)
        }

        PracticeDatabase.setDoc(db, listOf("datatest", "datatest", "main"), JSONObject().apply {
            put("id", "main")
            put("mapel", JSONObject().put(code, mapelConfig))
            put("dateTime", PracticeDatabase.now())
        }, merge = true)
        PracticeDatabase.setDoc(db, listOf("mapel", "mapel", code), PracticeDatabase.cloneObject(mapelConfig), merge = false)

        db.optJSONObject("soal")?.put(code, questions) ?: db.put("soal", JSONObject().put(code, questions))
        PracticeDatabase.setDoc(db, listOf("datakunci", "datakunci", code), defaultAnswerKey(code, normalized.questions.size), merge = false)
        PracticeDatabase.setDoc(db, listOf("datasiswa", "datasiswa", "$code-${normalized.username.uppercase()}"), defaultStudent(code, normalized), merge = false)

        db.optJSONObject("meta")?.put("dashboardAdmin", normalized.dashboardAdmin)
        PracticeDatabase.saveDb(root, db)
    }

    private fun openServer(root: File): LocalHttpServer {
        for (port in DEFAULT_PORT..(DEFAULT_PORT + 20)) {
            val socket = runCatching {
                ServerSocket(port, 50, InetAddress.getByName("127.0.0.1"))
            }.getOrNull()
            if (socket != null) {
                return LocalHttpServer(root, socket)
            }
        }
        throw IllegalStateException("Unable to start practice server")
    }

    private fun prepareSite(context: Context): File {
        val root = File(context.filesDir, RUNTIME_ROOT)
        val assetVersion = readAssetText(context, "$ASSET_ROOT/$VERSION_FILE").trim()
        val runtimeVersion = runCatching { File(root, VERSION_FILE).readText().trim() }.getOrDefault("")
        val shouldCopy = !root.exists() || assetVersion.isBlank() ||
            runtimeVersion != assetVersion || !File(root, "index.html").exists()
        if (shouldCopy) {
            root.deleteRecursively()
            root.mkdirs()
            copyAssetTree(context, ASSET_ROOT, root)
        }
        PracticeDatabase.ensureDatabase(root)
        return root
    }

    private fun copyAssetTree(context: Context, assetPath: String, target: File) {
        val children = context.assets.list(assetPath)
        if (children.isNullOrEmpty()) {
            target.parentFile?.mkdirs()
            context.assets.open(assetPath).use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            }
            return
        }
        target.mkdirs()
        for (child in children) {
            copyAssetTree(context, "$assetPath/$child", File(target, child))
        }
    }

    private fun readAssetText(context: Context, assetPath: String): String {
        return runCatching {
            context.assets.open(assetPath).use { it.readBytes().toString(Charsets.UTF_8) }
        }.getOrDefault("")
    }

    private fun questionsToSiteJson(questions: List<PracticeQuestion>): JSONArray {
        val labels = listOf("A", "B", "C", "D", "E")
        val array = JSONArray()
        questions.take(PracticeConfig.MAX_QUESTIONS).forEachIndexed { index, question ->
            val obj = JSONObject()
            obj.put("noasli", index + 1)
            obj.put("soal", question.text.toHtmlParagraph())
            val options = JSONArray()
            for (i in labels.indices) {
                val optionObj = JSONObject()
                optionObj.put("optionasli", labels[i])
                optionObj.put("answer", question.options.getOrElse(i) { "" })
                options.put(optionObj)
            }
            obj.put("options", options)
            array.put(obj)
        }
        return array
    }

    private fun defaultAnswerKey(code: String, count: Int): JSONObject {
        val weight = if (count > 0) (100 / count.coerceAtLeast(1)).toString() else "0"
        val kunci = JSONObject()
        val bobot = JSONObject()
        for (n in 1..count.coerceAtLeast(1)) {
            kunci.put(n.toString(), "A")
            bobot.put(n.toString(), weight)
        }
        return JSONObject().apply {
            put("id", code)
            put("kunci", kunci)
            put("bobot", bobot)
        }
    }

    private fun defaultStudent(code: String, config: PracticeConfig): JSONObject {
        val user = config.username.ifBlank { "user" }.uppercase()
        return JSONObject().apply {
            put("id", "$code-$user")
            put("mapel", code)
            put("indexkey", "0:$code:$user")
            put("indexmapel", "0:$code")
            put("nilai", 0)
            put("nama", config.fullName.ifBlank { "Student" })
            put("nik", "LOCAL")
            put("nik2", "LOCAL")
            put("nis", "0001")
            put("pass", "pass")
            put("password", "pass")
            put("server", "SRV-LOCAL")
            put("sesi", "1")
            put("user", user)
            put("username", user)
            put("sisawaktu", config.durationMinutes)
            put("status", 0)
            put("jawaban", "{}")
            put("timestamp", "[]")
        }
    }

    private fun String.toHtmlParagraph(): String {
        if (contains("<") && contains(">")) return trim()
        return lines().joinToString("") { line ->
            if (line.isBlank()) "<p></p>" else "<p>${escapeXml(line)}</p>"
        }
    }

    private fun escapeXml(value: String): String {
        return value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#039;")
    }

    private data class HttpRequest(val method: String, val target: String, val body: ByteArray) {
        fun jsonBody(): JSONObject {
            if (body.isEmpty()) return JSONObject()
            return runCatching { JSONObject(body.toString(StandardCharsets.UTF_8)) }.getOrDefault(JSONObject())
        }
    }

    private data class HttpResponse(
        val status: Int,
        val contentType: String,
        val body: ByteArray,
        val extraHeaders: Map<String, String> = emptyMap()
    )

    private class RequestTarget(target: String) {
        val path: String
        val query: Map<String, String>
        val parts: List<String>

        init {
            val questionIndex = target.indexOf('?')
            val rawPath = if (questionIndex >= 0) target.substring(0, questionIndex) else target
            path = decode(rawPath).ifBlank { "/" }
            query = if (questionIndex >= 0) {
                target.substring(questionIndex + 1)
                    .split("&")
                    .filter { it.isNotBlank() }
                    .associate { pair ->
                        val eq = pair.indexOf('=')
                        if (eq >= 0) decode(pair.substring(0, eq)) to decode(pair.substring(eq + 1))
                        else decode(pair) to ""
                    }
            } else {
                emptyMap()
            }
            parts = path.split("/").filter { it.isNotBlank() }
        }

        private fun decode(value: String): String =
            runCatching { URLDecoder.decode(value, "UTF-8") }.getOrDefault(value)
    }

    private class LocalHttpServer(private val root: File, private val serverSocket: ServerSocket) {
        val baseUrl = "http://127.0.0.1:${serverSocket.localPort}"

        @Volatile
        var isRunning = true
            private set

        fun start() {
            val thread = Thread({
                while (isRunning) {
                    val socket = runCatching { serverSocket.accept() }.getOrNull() ?: break
                    Thread({ handleClient(socket) }, "PracticeLocalRequest").apply { isDaemon = true }.start()
                }
            }, "PracticeLocalServer")
            thread.isDaemon = true
            thread.start()
        }

        fun stop() {
            isRunning = false
            runCatching { serverSocket.close() }
        }

        private fun handleClient(socket: Socket) {
            socket.use {
                runCatching {
                    val input = BufferedInputStream(socket.getInputStream())
                    val output = socket.getOutputStream()
                    handleConnection(input, output)
                }.onFailure {
                    runCatching {
                        writeResponse(socket.getOutputStream(), HttpResponse(500, "text/plain", "Server error".toByteArray()))
                    }
                }
            }
        }

        private fun handleConnection(input: BufferedInputStream, output: OutputStream) {
            val requestLine = input.readAsciiLine()
            if (requestLine.isBlank()) return
            val parts = requestLine.split(" ")
            if (parts.size < 2) {
                writeResponse(output, HttpResponse(400, "text/plain", "Bad request".toByteArray()))
                return
            }
            val method = parts[0]
            val target = parts[1]

            val headers = mutableMapOf<String, String>()
            while (true) {
                val line = input.readAsciiLine()
                if (line.isBlank()) break
                val colon = line.indexOf(':')
                if (colon > 0) {
                    headers[line.substring(0, colon).trim().lowercase()] = line.substring(colon + 1).trim()
                }
            }

            val contentLength = (headers["content-length"]?.toIntOrNull() ?: 0).coerceAtMost(MAX_BODY_BYTES)
            val body = ByteArray(contentLength)
            var readTotal = 0
            while (readTotal < contentLength) {
                val readCount = input.read(body, readTotal, contentLength - readTotal)
                if (readCount < 0) break
                readTotal += readCount
            }

            val request = HttpRequest(method.uppercase(), target, body.copyOf(readTotal))
            writeResponse(output, handleRequest(request))
        }

        private fun handleRequest(request: HttpRequest): HttpResponse {
            if (request.method == "OPTIONS") return jsonResponse(204, JSONObject())
            return runCatching {
                val target = RequestTarget(request.target)
                when {
                    target.path == "/api/admin" -> handleAdmin(request, target)
                    target.path == "/api/db" -> handleDb(request)
                    target.path == "/api/upload" -> handleUpload(request, target)
                    target.parts.size >= 2 && target.parts[0] == "api" && target.parts[1] == "soal" -> handleSoal(request, target)
                    target.parts.firstOrNull() == "avatar" -> handleAvatar(target)
                    else -> serveStatic(target)
                }
            }.getOrElse { error ->
                jsonResponse(500, JSONObject().put("err", error.message ?: "Server error"))
            }
        }

        private fun handleAdmin(request: HttpRequest, target: RequestTarget): HttpResponse {
            val db = PracticeDatabase.loadDb(root)
            val body = request.jsonBody()
            val data = body.optJSONObject("data") ?: body
            val action = target.query["_"] ?: ""

            val response = when (action) {
                "/datatest" -> datatest(db)
                "/datatest/add" -> {
                    val existing = PracticeDatabase.readCollectionOrDoc(db, listOf("datatest", "datatest", "main")).firstOrNull() ?: JSONObject()
                    PracticeDatabase.mergeInto(existing, data)
                    PracticeDatabase.setDoc(db, listOf("datatest", "datatest", "main"), existing, merge = true)
                    PracticeDatabase.saveDb(root, db)
                    JSONObject().put("ok", true)
                }
                "/user/loginAdmin" -> {
                    val admin = db.optJSONObject("admin") ?: JSONObject()
                    val username = data.optString("username")
                    val password = data.optString("password")
                    if (username == admin.optString("username", PracticeConfig.DEFAULT_ADMIN_USERNAME) &&
                        password == admin.optString("password", PracticeConfig.DEFAULT_ADMIN_PASSWORD)
                    ) {
                        JSONObject().apply {
                            put("status", "ok")
                            put("token", makeToken(JSONObject().put("hakAkses", "admin").put("username", username).put("idserver", "ALL")))
                            put("examkey", "LOCAL")
                            put("appsignature", "local-practice")
                            put("useragent", "local-practice")
                        }
                    } else {
                        JSONObject().put("status", "err").put("err", "Username or password is incorrect")
                    }
                }
                "/token" -> JSONObject().put("token", "LOCAL").put("aktif", PracticeDatabase.now())
                "/verifyToken" -> JSONObject().put("valid", true)
                "/broadcast/get" -> db.optJSONObject("broadcast") ?: JSONObject().put("message", "").put("updatedAt", "")
                "/broadcast/set" -> {
                    val broadcast = JSONObject().put("message", data.optString("message")).put("updatedAt", PracticeDatabase.now())
                    db.put("broadcast", broadcast)
                    PracticeDatabase.saveDb(root, db)
                    PracticeDatabase.cloneObject(broadcast).put("ok", true)
                }
                "/resetdatabase" -> {
                    val fresh = PracticeDatabase.seedDatabase()
                    PracticeDatabase.saveDb(root, fresh)
                    JSONObject().put("ok", true)
                }
                else -> JSONObject().put("ok", true).put("action", action)
            }
            return jsonResponse(200, response)
        }

        private fun handleDb(request: HttpRequest): HttpResponse {
            val db = PracticeDatabase.loadDb(root)
            val body = request.jsonBody()
            val path = body.optString("path", "").split("/").filter { it.isNotBlank() }
            return when (body.optString("action")) {
                "read" -> {
                    val docs = PracticeDatabase.readCollectionOrDoc(db, path)
                    jsonResponse(200, JSONArray(docs.map { it }))
                }
                "set" -> {
                    val merge = body.optBoolean("merge", true)
                    val data = body.optJSONObject("data") ?: JSONObject()
                    val result = PracticeDatabase.setDoc(db, path, data, merge)
                    PracticeDatabase.saveDb(root, db)
                    jsonResponse(200, result)
                }
                "delete" -> {
                    val deleted = PracticeDatabase.deleteDoc(db, path)
                    PracticeDatabase.saveDb(root, db)
                    jsonResponse(200, JSONObject().put("deleted", deleted))
                }
                else -> jsonResponse(400, JSONObject().put("err", "Unknown db action"))
            }
        }

        private fun handleUpload(request: HttpRequest, target: RequestTarget): HttpResponse {
            if (request.method != "POST") return jsonResponse(405, JSONObject().put("err", "Method not allowed"))
            val uploadsDir = File(root, "uploads")
            uploadsDir.mkdirs()
            val requested = target.query["filekey"] ?: "upload-${System.currentTimeMillis()}"
            val safeName = File(requested).name.replace(Regex("[^a-zA-Z0-9._-]"), "_").ifBlank { "upload" }
            File(uploadsDir, safeName).writeBytes(request.body)
            return jsonResponse(200, JSONObject().put("ok", true).put("file", safeName).put("url", "/uploads/$safeName"))
        }

        private fun handleSoal(request: HttpRequest, target: RequestTarget): HttpResponse {
            val db = PracticeDatabase.loadDb(root)
            val code = target.parts.getOrNull(3) ?: return jsonResponse(404, JSONObject().put("err", "Not found"))
            return if (request.method == "GET") {
                val soal = db.optJSONObject("soal")?.optJSONArray(code) ?: JSONArray()
                jsonResponse(200, JSONObject().put("success", true).put("data", JSONObject().put("kode", code).put("soal", soal.toString())))
            } else {
                val body = request.jsonBody()
                val soalValue = body.opt("soal")
                val soal = when (soalValue) {
                    is String -> runCatching { JSONArray(soalValue) }.getOrDefault(JSONArray())
                    is JSONArray -> soalValue
                    else -> JSONArray()
                }
                val soalMap = db.optJSONObject("soal") ?: JSONObject().also { db.put("soal", it) }
                soalMap.put(code, soal)
                PracticeDatabase.saveDb(root, db)
                jsonResponse(200, JSONObject().put("success", true))
            }
        }

        private fun handleAvatar(target: RequestTarget): HttpResponse {
            val seed = target.parts.drop(1).joinToString("/").ifBlank { "Student" }
            val colors = listOf("#0d6efd", "#198754", "#6f42c1", "#d63384", "#fd7e14", "#0f766e")
            var hash = 0
            for (c in seed) hash = hash * 31 + c.code
            val color = colors[(hash and Int.MAX_VALUE) % colors.size]
            val words = seed.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
            val initials = when {
                words.size > 1 -> "${words.first().first()}${words.last().first()}"
                words.isNotEmpty() -> words.first().take(2)
                else -> "S"
            }.uppercase()
            val svg = """<svg xmlns="http://www.w3.org/2000/svg" width="120" height="120" viewBox="0 0 120 120">
                <rect width="120" height="120" rx="60" fill="$color"/>
                <text x="60" y="68" font-size="38" font-weight="700" fill="#ffffff" text-anchor="middle" font-family="sans-serif">${escapeXml(initials)}</text>
                </svg>""".trimIndent()
            return HttpResponse(200, "image/svg+xml; charset=utf-8", svg.toByteArray(StandardCharsets.UTF_8))
        }

        private fun serveStatic(target: RequestTarget): HttpResponse {
            val relative = target.path.removePrefix("/").ifBlank { "index.html" }
            val file = File(root, relative).canonicalFile
            if (!file.path.startsWith(root.canonicalPath)) {
                return HttpResponse(403, "text/plain", "Forbidden".toByteArray())
            }
            val resolved = if (!file.exists() || file.isDirectory) {
                val lastSegment = relative.substringAfterLast('/')
                if (lastSegment.contains(".")) return HttpResponse(404, "text/plain", "Not found".toByteArray())
                File(root, "index.html")
            } else {
                file
            }
            if (!resolved.exists()) return HttpResponse(404, "text/plain", "Not found".toByteArray())
            return HttpResponse(200, contentType(resolved.name), resolved.readBytes())
        }

        private fun datatest(db: JSONObject): JSONObject {
            val doc = PracticeDatabase.readCollectionOrDoc(db, listOf("datatest", "datatest", "main")).firstOrNull()
                ?: JSONObject()
            doc.put("dateTime", PracticeDatabase.now())
            return doc
        }

        private fun jsonResponse(status: Int, body: Any): HttpResponse {
            val text = body.toString()
            return HttpResponse(status, "application/json; charset=utf-8", text.toByteArray(StandardCharsets.UTF_8))
        }

        private fun makeToken(payload: JSONObject): String {
            payload.put("iat", System.currentTimeMillis() / 1000)
            payload.put("exp", System.currentTimeMillis() / 1000 + 7 * 24 * 3600)
            val header = JSONObject().put("alg", "none").put("typ", "JWT")
            return "${base64Url(header)}.${base64Url(payload)}.local"
        }

        private fun base64Url(value: JSONObject): String {
            return Base64.encodeToString(
                value.toString().toByteArray(StandardCharsets.UTF_8),
                Base64.NO_WRAP or Base64.URL_SAFE or Base64.NO_PADDING
            )
        }

        private fun contentType(name: String): String {
            val ext = name.substringAfterLast('.', "").lowercase()
            return when (ext) {
                "html" -> "text/html; charset=utf-8"
                "js" -> "text/javascript; charset=utf-8"
                "css" -> "text/css; charset=utf-8"
                "csv" -> "text/csv; charset=utf-8"
                "json" -> "application/json; charset=utf-8"
                "xls" -> "application/vnd.ms-excel"
                "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                "png" -> "image/png"
                "jpg", "jpeg" -> "image/jpeg"
                "webp" -> "image/webp"
                "gif" -> "image/gif"
                "ico" -> "image/x-icon"
                "svg" -> "image/svg+xml; charset=utf-8"
                "ttf" -> "font/ttf"
                "woff" -> "font/woff"
                "woff2" -> "font/woff2"
                else -> "application/octet-stream"
            }
        }

        private fun writeResponse(output: OutputStream, response: HttpResponse) {
            val statusText = when (response.status) {
                200 -> "OK"
                204 -> "No Content"
                400 -> "Bad Request"
                403 -> "Forbidden"
                404 -> "Not Found"
                405 -> "Method Not Allowed"
                else -> "Internal Server Error"
            }
            val headers = LinkedHashMap<String, String>()
            headers["Content-Type"] = response.contentType
            headers["Content-Length"] = response.body.size.toString()
            headers["Cache-Control"] = "no-store"
            headers["Access-Control-Allow-Origin"] = "*"
            headers["Access-Control-Allow-Headers"] = "Content-Type, Authorization"
            headers["Access-Control-Allow-Methods"] = "GET, POST, OPTIONS"
            headers.putAll(response.extraHeaders)

            val builder = StringBuilder()
            builder.append("HTTP/1.1 ${response.status} $statusText\r\n")
            for ((key, value) in headers) {
                builder.append("$key: $value\r\n")
            }
            builder.append("\r\n")
            output.write(builder.toString().toByteArray(StandardCharsets.ISO_8859_1))
            output.write(response.body)
            output.flush()
        }
    }
}

private fun BufferedInputStream.readAsciiLine(): String {
    val buffer = ByteArrayOutputStream()
    while (true) {
        val byte = read()
        if (byte == -1 || byte == '\n'.code) break
        if (byte != '\r'.code) buffer.write(byte)
    }
    return buffer.toString(StandardCharsets.ISO_8859_1.name())
}
