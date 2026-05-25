package br.com.zenith.ui.components.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.vanniktech.blurhash.BlurHash

@Composable
fun BetterProfileImage(
    imageUrl: String,
    blurHash: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    // Transforma a String do BlurHash num Bitmap desfocado em memória
    val placeholderBitmap = remember(blurHash) {
        if (!blurHash.isNullOrBlank()) {
            try {
                // Renderiza o gradiente numa escala leve de 32x32 para economizar RAM
                BlurHash.decode(blurHash, 32, 32)
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    AsyncImage(
        model = imageUrl,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = ContentScale.Crop,
        // Aplica o bitmap do BlurHash convertido em Painter do Compose
        placeholder = placeholderBitmap?.let { BitmapPainter(it.asImageBitmap()) },
        error = placeholderBitmap?.let { BitmapPainter(it.asImageBitmap()) }
    )
}