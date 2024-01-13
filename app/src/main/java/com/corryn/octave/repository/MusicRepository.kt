package com.corryn.octave.repository

import android.content.Context
import android.database.Cursor
import android.os.Environment
import android.provider.MediaStore
import com.corryn.octave.model.Song

class MusicRepository {

    private val isExternalStorageReadable: Boolean
        get() {
            val state = Environment.getExternalStorageState()
            return Environment.MEDIA_MOUNTED == state || Environment.MEDIA_MOUNTED_READ_ONLY == state
        }

    private val musicProjection = arrayOf(
        MediaStore.Audio.Media._ID,
        MediaStore.Audio.Media.ARTIST_ID,
        MediaStore.Audio.Media.ALBUM_ID,
        MediaStore.Audio.Media.TITLE,
        MediaStore.Audio.Media.ARTIST,
        MediaStore.Audio.Media.ALBUM,
        MediaStore.Audio.Media.DATA
    )

    private val allMusicSelection: String = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
    private val artistSelection: String = "$allMusicSelection AND ${MediaStore.Audio.Media.ARTIST_ID} = ?"
    private val albumSelection: String = "$allMusicSelection AND ${MediaStore.Audio.Media.ALBUM_ID} = ?"

    fun createArtistSongHashMap(context: Context?): Map<Long, List<Song>> {
        return if (isExternalStorageReadable) {
            getSongsBySelection(context, allMusicSelection).groupBy { it.artistId }
        } else {
            // TODO Dialog with warning? Means to retry?
            HashMap()
        }
    }

    fun getSongsForArtist(context: Context?, artistId: Long): List<Song> {
        return if (isExternalStorageReadable) {
            getSongsBySelection(context, artistSelection, arrayOf(artistId.toString()))
        } else {
            emptyList()
        }
    }

    fun getSongsForAlbum(context: Context?, albumId: Long): List<Song> {
        return if (isExternalStorageReadable) {
            getSongsBySelection(context, albumSelection, arrayOf(albumId.toString()))
        } else {
            emptyList()
        }
    }

    private fun getSongsBySelection(context: Context?, selection: String, selectionArgs: Array<String> = emptyArray()): List<Song> {
        val songs = mutableListOf<Song>()

        context?.contentResolver?.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            musicProjection,
            selection,
            null,
            null
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                processSong(cursor).also {
                    songs.add(it)
                }
            }
        }

        return songs
    }

    // TODO Find a better way to do the indices?
    private fun processSong(cursor: Cursor): Song {
        val songId = cursor.getLong(0)
        val artistId = cursor.getLong(1)
        val albumId = cursor.getLong(2)
        val title = cursor.getString(3)
        val artist = cursor.getString(4)
        val album = cursor.getString(5)
        val data = cursor.getString(6)

        return Song(songId, artistId, albumId, title, artist, album, data)
    }

}