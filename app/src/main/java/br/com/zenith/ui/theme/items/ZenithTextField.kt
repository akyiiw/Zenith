package br.com.zenith.ui.theme.items

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import br.com.zenith.ui.theme.Black
import br.com.zenith.ui.theme.TextFieldGreen

@Composable
fun ZenithTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier, // Movido para o parâmetro (padrão do Compose)
    enabled: Boolean = true, // Adicionado com padrão true
    prefix: String? = null,
    placeholder: String? = null,
    minLines: Int = 1,
    maxLines: Int = 1,
    singleLine: Boolean = maxLines == 1,
    readOnly: Boolean = false,
    isError: Boolean = false,
    supportingText: @Composable (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = {
            if (placeholder != null) {
                Text(placeholder)
            }
        },
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        readOnly = readOnly,
        singleLine = singleLine,
        prefix = {
            if (prefix != null) {
                Text(prefix)
            }
        },
        minLines = minLines,
        maxLines = maxLines,
        isError = isError,
        supportingText = supportingText,
        keyboardOptions = keyboardOptions,
        visualTransformation = visualTransformation,
        textStyle = MaterialTheme.typography.bodyLarge,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = TextFieldGreen,
            focusedTextColor = Black,
            focusedLabelColor = TextFieldGreen,

            unfocusedBorderColor = Color(0xAA515151),
            unfocusedTextColor = Color(0xEE515151),
            unfocusedLabelColor = Color(0xEE515151),

            cursorColor = TextFieldGreen,
            selectionColors = TextSelectionColors(
                handleColor = TextFieldGreen, // Cor da gotinha
                backgroundColor = TextFieldGreen.copy(alpha = 0.3f) // Cor do fundo do texto selecionado
            )
        ),
        shape = RoundedCornerShape(size = 10.dp),
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon
    )
}
