package com.corryn.octave.viewmodel

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.corryn.octave.AlbumBitmapFactory
import com.corryn.octave.model.NowPlayingInfo
import com.corryn.octave.model.Song
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.LinkedList
import java.util.Random

class PlayerViewModel: ViewModel() {

    val player = MediaPlayer()

    private val factory = AlbumBitmapFactory()

    val songList = mutableListOf<Song>()
    val artistList = mutableListOf<String>()
    val byArtistList = HashMap<String, MutableList<Song>>()
    var activeList: List<Song?>? = null
    var viewedList: List<Song>? = null

    private val playlist = LinkedList<Song>()

    private var nowPlayingIndex = -1
    private var songNowPlaying: Song? = null
    var selected = -1

    private var active = false
    private var repeat = false
    private var shuffle = false

    var playClicked = false
    var isSearching = false

    // Emitting a value of null indicates there was an error trying to play the song.
    private val _nowPlaying: MutableSharedFlow<NowPlayingInfo?> = MutableSharedFlow()
    val nowPlaying: SharedFlow<NowPlayingInfo?> = _nowPlaying.asSharedFlow()

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

    fun preparePlayer(context: Context) {
        player.setOnCompletionListener { nextSong(context) }
    }

    val selectedSong: Song?
        get() = if (selected != -1) {
            songList[selected]
        } else null

    fun nextSong(context: Context) {
        if (!playlist.isEmpty()) {
            setSong(removeFromPlaylist(), context)
        } else if (shuffle) {
            shuffle(context)
        } else if (nowPlayingIndex + 1 < activeList!!.size) {
            setSong(++nowPlayingIndex, context)
        } else {
            nowPlayingIndex = 0
            setSong(nowPlayingIndex, context)
        }
    }

    fun prevSong(context: Context) {
        if (playlist.isEmpty()) {
            when {
                shuffle -> {
                    shuffle(context)
                }
                nowPlayingIndex - 1 >= 0 -> {
                    setSong(--nowPlayingIndex, context)
                }
                else -> {
                    nowPlayingIndex = activeList!!.size - 1
                    setSong(nowPlayingIndex, context)
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

    fun shuffle(context: Context) {
        val random: Int
        val r = Random()
        random = r.nextInt(activeList!!.size)
        setSong(random, context)
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

    fun setSong(songIndex: Int, context: Context) {
        val uri: Uri?
        val temp: Song?
        try {
            temp = activeList!![songIndex]
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
            player.setDataSource(context, uri)
            player.prepare()
            player.start()
            if (repeat) {
                player.isLooping = true
            }
            nowPlayingIndex = songIndex
            songNowPlaying = temp

            val song = activeList!![songIndex]!!
            val info = NowPlayingInfo(song.title, song.artist)

            viewModelScope.launch {
                _nowPlaying.emit(info)
            }
        } catch (e: IOException) {
            viewModelScope.launch {
                _nowPlaying.emit(null)
            }
        }
    }

    private fun setSong(s: Song?, context: Context) {
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
            player.setDataSource(context, uri ?: throw IOException())
            player.prepare()
            player.start()
            if (repeat) {
                player.isLooping = true
            }
            nowPlayingIndex = activeList!!.indexOf(s)
            songNowPlaying = s

            val info = NowPlayingInfo(s.title, s.artist)

            viewModelScope.launch {
                _nowPlaying.emit(info)
            }
        } catch (e: IOException) {
            viewModelScope.launch {
                _nowPlaying.emit(null)
            }
        }
    }

    fun filterSongs(query: String): List<Song> {
        return viewedList!!.filter { it.title.contains(query, ignoreCase = true) }
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

}