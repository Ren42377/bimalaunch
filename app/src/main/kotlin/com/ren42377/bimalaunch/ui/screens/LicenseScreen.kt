package com.ren42377.bimalaunch.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ren42377.bimalaunch.R
import com.ren42377.bimalaunch.ui.theme.CloudMedium
import com.ren42377.bimalaunch.ui.theme.EditorialTitle

private class LicenseKeyVisualTransformation(
    private val placeholderColor: Color
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val trimmed = text.text.take(16)
        val output = buildAnnotatedString {
            for (i in 0 until 16) {
                if (i < trimmed.length) {
                    append(trimmed[i].toString())
                } else {
                    withStyle(SpanStyle(color = placeholderColor)) { append("X") }
                }
                if (i % 4 == 3 && i != 15) {
                    if (i >= trimmed.length) {
                        withStyle(SpanStyle(color = placeholderColor)) { append("-") }
                    } else {
                        append("-")
                    }
                }
            }
        }
        val mapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                val safe = offset.coerceIn(0, trimmed.length)
                return when {
                    safe <= 3 -> safe
                    safe <= 7 -> safe + 1
                    safe <= 11 -> safe + 2
                    safe <= 16 -> safe + 3
                    else -> 19
                }
            }

            override fun transformedToOriginal(offset: Int): Int {
                val original = when {
                    offset <= 4 -> offset
                    offset <= 9 -> offset - 1
                    offset <= 14 -> offset - 2
                    offset <= 19 -> offset - 3
                    else -> 16
                }
                return original.coerceIn(0, trimmed.length)
            }
        }
        return TransformedText(output, mapping)
    }
}

@Composable
fun LicenseScreen(
    isLoading: Boolean,
    errorText: String?,
    isVerifying: Boolean,
    onVerifyClick: (String) -> Unit
) {
    var licenseDigits by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val iconTransition = rememberInfiniteTransition(label = "licenseIcon")
    val iconScale = iconTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "licenseIconScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { focusManager.clearFocus() }
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 560.dp)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Icon(
                        painter = painterResource(R.drawable.ic_shield),
                        contentDescription = null,
                        modifier = Modifier
                            .height(40.dp)
                            .graphicsLayer {
                                scaleX = iconScale.value
                                scaleY = iconScale.value
                            },
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(24.dp))
                    Text(
                        text = stringResource(
                            if (isLoading) R.string.license_loading_title else R.string.license_title
                        ),
                        style = EditorialTitle,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = stringResource(
                            if (isLoading) R.string.license_loading_desc else R.string.license_desc
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.82f)
                    )
                    if (isLoading) {
                        Spacer(Modifier.height(24.dp))
                        CircularProgressIndicator(
                            modifier = Modifier.height(28.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            if (!isLoading) {
                Spacer(Modifier.height(24.dp))
                val placeholderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                OutlinedTextField(
                    value = licenseDigits,
                    onValueChange = { value -> licenseDigits = value.filter(Char::isDigit).take(16) },
                    enabled = !isVerifying,
                    singleLine = true,
                    visualTransformation = remember(placeholderColor) {
                        LicenseKeyVisualTransformation(placeholderColor)
                    },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        textAlign = TextAlign.Center,
                        letterSpacing = 1.8.sp
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = {
                        focusManager.clearFocus()
                        onVerifyClick(licenseDigits.chunked(4).joinToString("-"))
                    }),
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = CloudMedium,
                        disabledBorderColor = CloudMedium
                    )
                )
                Spacer(Modifier.height(16.dp))
                val interactionSource = remember { MutableInteractionSource() }
                val isPressed by interactionSource.collectIsPressedAsState()
                val btnScale by animateFloatAsState(if (isPressed) 0.96f else 1f, label = "btnScale")
                Button(
                    onClick = {
                        focusManager.clearFocus()
                        onVerifyClick(licenseDigits.chunked(4).joinToString("-"))
                    },
                    enabled = !isVerifying,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .scale(btnScale),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    interactionSource = interactionSource
                ) {
                    if (isVerifying) {
                        CircularProgressIndicator(
                            modifier = Modifier.height(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.license_verify).uppercase(),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
                AnimatedVisibility(
                    visible = !errorText.isNullOrBlank(),
                    enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
                    exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top)
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Text(
                            text = errorText ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp)
                        )
                    }
                }
            }
        }
    }
}
