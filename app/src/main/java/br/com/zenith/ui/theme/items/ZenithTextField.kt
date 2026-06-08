package br.com.zenith.ui.theme.items

import android.graphics.drawable.Icon
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    minLines: Int = 1,
    maxLines: Int = 1,
    leadingIcon: @Composable (() -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        prefix = {
            if (prefix != null) {
                Text(prefix)
            }
        },
        minLines = minLines,
        maxLines = maxLines,
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
        shape = RoundedCornerShape(size = 8.dp),
        leadingIcon = leadingIcon
    )
}