package br.com.zenith.ui.components.common

import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import br.com.zenith.ui.theme.Green

@Composable
fun zenithSwitchColors(): SwitchColors {
    return SwitchDefaults.colors(
        checkedThumbColor = Color.White,
        checkedTrackColor = Green,
        checkedBorderColor = Green,
        uncheckedThumbColor = Color.White,
        uncheckedTrackColor = Color(0xFFE6E6E6),
        uncheckedBorderColor = Color(0xFFBDBDBD),
        disabledCheckedThumbColor = Color.White.copy(alpha = 0.75f),
        disabledCheckedTrackColor = Green.copy(alpha = 0.45f),
        disabledUncheckedThumbColor = Color.White.copy(alpha = 0.75f),
        disabledUncheckedTrackColor = Color(0xFFE6E6E6).copy(alpha = 0.6f)
    )
}
