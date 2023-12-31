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

    fun createArtistSongHashMap(context: Context?): HashMap<String, MutableList<Song>> {
        if (isExternalStorageReadable) {
            val musicProjection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Albums.ARTIST,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.DATA
            )

            val musicSelection = MediaStore.Audio.Media.IS_MUSIC + " != 0"

            val songs = mutableListOf<Song>()

            context?.contentResolver?.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                musicProjection,
                musicSelection,
                null,
                null
            )?.use { musicCursor ->
                while (musicCursor.moveToNext()) {
                    songs.add(processSong(musicCursor))
                }
            }

            val map = HashMap<String, MutableList<Song>>()
            for (song in songs) {
                val songsByArtist = map[song.artist]

                if (songsByArtist == null) {
                    map[song.artist] = mutableListOf(song)
                } else {
                    songsByArtist.add(song)
                }
            }

            return map
        } else {
            // TODO Dialog with warning? Means to retry?
            return HashMap()
        }
    }

    private fun processSong(musicCursor: Cursor): Song {
        val thisId = musicCursor.getLong(0)
        val thisAlbumId = musicCursor.getLong(1)
        val thisArtist = musicCursor.getString(2)
        val thisTitle = musicCursor.getString(3)
        val thisData = musicCursor.getString(4)

        return Song(thisId, thisAlbumId, thisTitle, thisArtist, thisData)
    }

}