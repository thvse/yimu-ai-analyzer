package com.yimu.ai.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream

object ImageHelper {

    /**
     * 读取指定 Uri 的图片并压缩转为 Base64，同时返回缩放后的 Bitmap
     */
    fun processImageUri(
        context: Context,
        uri: Uri,
        maxDimension: Int = 1024
    ): Pair<Bitmap?, String?> {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null to null
            val original = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            if (original == null) return null to null

            val width = original.width
            val height = original.height
            val scale = if (width > height) {
                if (width > maxDimension) maxDimension.toFloat() / width else 1f
            } else {
                if (height > maxDimension) maxDimension.toFloat() / height else 1f
            }

            val scaled = if (scale < 1f) {
                Bitmap.createScaledBitmap(
                    original,
                    (width * scale).toInt().coerceAtLeast(1),
                    (height * scale).toInt().coerceAtLeast(1),
                    true
                )
            } else original

            val outputStream = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
            val bytes = outputStream.toByteArray()
            val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
            scaled to base64
        } catch (e: Exception) {
            e.printStackTrace()
            null to null
        }
    }

    /**
     * 从 Uri 安全加载 Bitmap（用于界面缩略图展示）
     */
    fun loadThumbnail(context: Context, uriString: String, maxDimension: Int = 512): Bitmap? {
        return try {
            val uri = Uri.parse(uriString)
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val original = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            if (original == null) return null

            val width = original.width
            val height = original.height
            val scale = if (width > height) {
                if (width > maxDimension) maxDimension.toFloat() / width else 1f
            } else {
                if (height > maxDimension) maxDimension.toFloat() / height else 1f
            }

            if (scale < 1f) {
                Bitmap.createScaledBitmap(
                    original,
                    (width * scale).toInt().coerceAtLeast(1),
                    (height * scale).toInt().coerceAtLeast(1),
                    true
                )
            } else original
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
