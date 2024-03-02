package com.corryn.octave.ui.factory

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.net.Uri
import java.io.FileNotFoundException
import java.io.InputStream

class AlbumBitmapFactory {

    fun getAlbumArt(context: Context?, albumId: Long): Bitmap? {
        val inputStream: InputStream?
        val sArtworkUri = Uri.parse(albumArtUri)
        val uri = ContentUris.withAppendedId(sArtworkUri, albumId)

        inputStream = try {
            context?.contentResolver?.openInputStream(uri)
        } catch (e: FileNotFoundException) {
            return null
        }

        return BitmapFactory.decodeStream(inputStream)
    }

    fun getRoundedCornerBitmap(bitmap: Bitmap?, pixels: Int): Bitmap? {
        if (bitmap != null) {
            val output = Bitmap.createBitmap(bitmap.width, bitmap
                .height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(output)
            val color = -0xbdbdbe
            val paint = Paint()
            val rect = Rect(0, 0, bitmap.width, bitmap.height)
            val rectF = RectF(rect)
            val roundPx = pixels.toFloat()
            paint.isAntiAlias = true
            canvas.drawARGB(0, 0, 0, 0)
            paint.color = color
            canvas.drawRoundRect(rectF, roundPx, roundPx, paint)
            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
            canvas.drawBitmap(bitmap, rect, rect, paint)
            return output
        }

        return null
    }

    companion object {
        private const val albumArtUri = "content://media/external/audio/albumart"
    }

}