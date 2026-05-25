package br.com.zenith.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import java.io.FileOutputStream

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
}