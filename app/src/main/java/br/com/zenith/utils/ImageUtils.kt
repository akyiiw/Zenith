package br.com.zenith.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt

object ImageUtils {

    /**
     * Redimensiona e comprime uma imagem para evitar estouro de banda e espaço no Supabase.
     * @param context Contexto do app.
     * @param fileOriginal Arquivo original vindo da galeria ou câmera.
     * @param maxDimensao Tamanho máximo da largura ou altura (Ex: 400 para perfil, 1080 para banner).
     */
    fun comprimirImagem(context: Context, fileOriginal: File, maxDimensao: Int): File {
        // 1. Decodifica apenas os metadados (bordas) para checar o tamanho sem carregar o peso na memória RAM
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(fileOriginal.absolutePath, options)

        // 2. Calcula o fator de escala de encolhimento
        var scale = 1
        while (options.outWidth / scale / 2 >= maxDimensao && options.outHeight / scale / 2 >= maxDimensao) {
            scale *= 2
        }

        // 3. Decodifica a imagem aplicando o fator de escala calculado
        val decodeOptions = BitmapFactory.Options().apply { inSampleSize = scale }
        val bitmapRedimensionado = BitmapFactory.decodeFile(fileOriginal.absolutePath, decodeOptions)

        // 4. Cria o arquivo final temporário na pasta de cache do app
        val arquivoComprimido = File(context.cacheDir, "zenith_upload_${System.currentTimeMillis()}.jpg")
        val outputStream = FileOutputStream(arquivoComprimido)

        // Comprime para JPEG reduzindo a qualidade para 75% (muda quase nada a olho nu, mas diminui 90% do peso)
        bitmapRedimensionado.compress(Bitmap.CompressFormat.JPEG, 75, outputStream)
        outputStream.flush()
        outputStream.close()

        return arquivoComprimido
    }

    fun recortarEComprimirImagem(
        context: Context,
        fileOriginal: File,
        maxDimensao: Int,
        aspectRatio: Float,
        focusX: Float,
        focusY: Float
    ): File {
        val bitmapOriginal = BitmapFactory.decodeFile(fileOriginal.absolutePath)
            ?: throw IllegalArgumentException("Imagem inválida")

        val sourceWidth = bitmapOriginal.width
        val sourceHeight = bitmapOriginal.height
        val sourceRatio = sourceWidth.toFloat() / sourceHeight.toFloat()

        val cropWidth: Int
        val cropHeight: Int

        if (sourceRatio > aspectRatio) {
            cropHeight = sourceHeight
            cropWidth = (sourceHeight * aspectRatio).roundToInt().coerceAtMost(sourceWidth)
        } else {
            cropWidth = sourceWidth
            cropHeight = (sourceWidth / aspectRatio).roundToInt().coerceAtMost(sourceHeight)
        }

        val normalizedFocusX = focusX.coerceIn(-1f, 1f)
        val normalizedFocusY = focusY.coerceIn(-1f, 1f)
        val maxLeft = (sourceWidth - cropWidth).coerceAtLeast(0)
        val maxTop = (sourceHeight - cropHeight).coerceAtLeast(0)
        val left = ((normalizedFocusX + 1f) / 2f * maxLeft).roundToInt().coerceIn(0, maxLeft)
        val top = ((normalizedFocusY + 1f) / 2f * maxTop).roundToInt().coerceIn(0, maxTop)

        val croppedBitmap = Bitmap.createBitmap(bitmapOriginal, left, top, cropWidth, cropHeight)
        if (croppedBitmap != bitmapOriginal) {
            bitmapOriginal.recycle()
        }

        val largestSide = maxOf(croppedBitmap.width, croppedBitmap.height)
        val finalBitmap = if (largestSide > maxDimensao) {
            val scale = maxDimensao.toFloat() / largestSide.toFloat()
            Bitmap.createScaledBitmap(
                croppedBitmap,
                (croppedBitmap.width * scale).roundToInt().coerceAtLeast(1),
                (croppedBitmap.height * scale).roundToInt().coerceAtLeast(1),
                true
            )
        } else {
            croppedBitmap
        }

        if (finalBitmap != croppedBitmap) {
            croppedBitmap.recycle()
        }

        val arquivoComprimido = File(context.cacheDir, "zenith_upload_${System.currentTimeMillis()}.jpg")
        FileOutputStream(arquivoComprimido).use { outputStream ->
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, 82, outputStream)
        }
        finalBitmap.recycle()

        return arquivoComprimido
    }
}
