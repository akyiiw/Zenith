package br.com.zenith.ui.components.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import br.com.zenith.ui.theme.Inter
import br.com.zenith.ui.theme.TextFieldGreen

data class ZenithFilterOption<T>(
    val value: T,
    val label: String
)

@Composable
fun <T> ZenithFilterBar(
    options: List<ZenithFilterOption<T>>,
    selectedValue: T,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 0.dp),
) {
    LazyRow(
        modifier = modifier,
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(options) { option ->
            ZenithFilterPill(
                label = option.label,
                selected = option.value == selectedValue,
                onClick = { onSelected(option.value) }
            )
        }
    }
}

@Composable
fun ZenithFilterPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) Color(0xFFEAF5EA) else Color.White)
            .border(
                width = 1.dp,
                color = if (selected) TextFieldGreen else Color(0xFFD6DED6),
                shape = RoundedCornerShape(999.dp)
            )
            .clickable(onClick = onClick)
            .heightIn(min = 38.dp)
            .padding(horizontal = 15.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = Inter,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) TextFieldGreen else Color(0xFF4E5D4F)
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
