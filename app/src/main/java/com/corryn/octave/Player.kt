package com.corryn.octave

import android.content.ContentResolver
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
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.widget.Toast
import com.corryn.octave.model.Song
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.util.LinkedList
import java.util.Random


object Player {

    val player = MediaPlayer()

    val songList = mutableListOf<Song>()
    val artistList = mutableListOf<String>()
    val byArtistList = HashMap<String, MutableList<Song>>()
    var activeList: List<Song?>? = null
    var viewedList: List<Song>? = null

    private val playlist = LinkedList<Song>()

    private var context: Context? = null

    private var nowPlaying = -1
    private var songNowPlaying: Song? = null
    var selected = -1

    private var active = false
    private var repeat = false
    private var shuffle = false

    var playClicked = false
    var isSearching = false

    fun setActive() {
        active = true
    }

    fun exists(): Boolean {
        return active
    }

    fun pauseSong() {
        player.pause()
    }

    fun unpauseSong() {
        player.start()
    }

    val isPaused: Boolean
        get() = !player.isPlaying

    fun preparePlayer() {
        player.setOnCompletionListener { nextSong() }
    }

    val selectedSong: Song?
        get() = if (selected != -1) {
            songList[selected]
        } else null

    fun nextSong() {
        if (!playlist.isEmpty()) {
            setSong(removeFromPlaylist())
        } else if (shuffle) {
            shuffle()
        } else if (nowPlaying + 1 < activeList!!.size) {
            setSong(++nowPlaying)
        } else {
            nowPlaying = 0
            setSong(nowPlaying)
        }
    }

    fun prevSong() {
        if (playlist.isEmpty()) {
            when {
                shuffle -> {
                    shuffle()
                }
                nowPlaying - 1 >= 0 -> {
                    setSong(--nowPlaying)
                }
                else -> {
                    nowPlaying = activeList!!.size - 1
                    setSong(nowPlaying)
                }
            }
        }
    }

    fun playlistIsEmpty(): Boolean {
        return playlist.isEmpty()
    }

    fun addToPlaylist(s: Song) {
        playlist.add(s)
    }

    fun removeFromPlaylist(): Song? {
        return if (!playlist.isEmpty()) {
            playlist.remove()
        } else {
            null
        }
    }

    fun playlistNext(): Song? {
        return if (!playlist.isEmpty()) {
            playlist.first
        } else null
    }

    fun shuffle() {
        val random: Int
        val r = Random()
        random = r.nextInt(activeList!!.size)
        setSong(random)
    }

    fun toggleRepeat(): Boolean {
        repeat = !repeat
        player.isLooping = repeat
        return repeat
    }

    fun toggleShuffle(): Boolean {
        shuffle = !shuffle
        return shuffle
    }

    fun updateContext(c: Context?) {
        context = c
    }

    fun setSong(s: Int) {
        val uri: Uri?
        val temp: Song?
        try {
            temp = activeList!![s]
            uri = Uri.parse("file:///" + temp!!.data)
        } catch (e: Exception) {
            return
        }
        try {
            player.reset()
            player.setAudioAttributes(
                    AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build())
            player.setDataSource(context!!, uri)
            player.prepare()
            player.start()
            if (repeat) {
                player.isLooping = true
            }
            nowPlaying = s
            songNowPlaying = temp
            Toast.makeText(context, "Now Playing: " + activeList!![s]!!.title + " - " + activeList!![s]!!.artist, Toast.LENGTH_SHORT).show()
        } catch (e: IOException) {
            Toast.makeText(context, "File not found!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setSong(s: Song?) {
        val uri: Uri? = try {
            Uri.parse("file:///" + s!!.data)
        } catch (e: Exception) {
            return
        }
        try {
            player.reset()
            player.setAudioAttributes(
                    AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build())
            player.setDataSource(context!!, uri ?: throw IOException())
            player.prepare()
            player.start()
            if (repeat) {
                player.isLooping = true
            }
            nowPlaying = activeList!!.indexOf(s)
            songNowPlaying = s
            Toast.makeText(context, "Now Playing: " + s.title + " - " + s.artist, Toast.LENGTH_SHORT).show()
        } catch (e: IOException) {
            Toast.makeText(context, "File not found!", Toast.LENGTH_SHORT).show()
        }
    }

    fun filterSongs(query: String): List<Song> {
        val result = ArrayList<Song>()
        for (s in viewedList!!) {
            if (s.title.toLowerCase().contains(query.toLowerCase())) {
                result.add(s)
            }
        }
        return result
    }

    fun getNowPlaying(): Song? {
        return songNowPlaying
    }

    fun getSongIndex(s: Song): Int {
        for (i in activeList!!.indices) {
            if (s == activeList!![i]) {
                return i
            }
        }
        return -1
    }

    fun getAlbumArt(cr: ContentResolver?, albumId: Long): Bitmap? {
        val inputStream: InputStream?
        val sArtworkUri = Uri.parse("content://media/external/audio/albumart")
        val uri = ContentUris.withAppendedId(sArtworkUri, albumId)
        inputStream = try {
            cr?.openInputStream(uri)
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

}