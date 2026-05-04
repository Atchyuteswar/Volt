package com.kazexyt.volt.utils

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

object ShareUtils {
    fun shareBitmap(context: Context, bitmap: Bitmap) {
        try {
            // 1. Create the temporary folder and file
            val cachePath = File(context.cacheDir, "images")
            cachePath.mkdirs()
            val file = File(cachePath, "volt_share_${System.currentTimeMillis()}.png")

            // 2. Compress the Bitmap into the file
            val stream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.close()

            // 3. Generate the secure URI
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )

            // 4. Launch the Native Share Sheet
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                // Grants read permission to whatever app the user selects (e.g., Instagram)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(intent, "Share your Volt summary!"))

        } catch (e: Exception) {
            android.util.Log.e("VoltShare", "Sharing failed: ${e.message}")
        }
    }
}