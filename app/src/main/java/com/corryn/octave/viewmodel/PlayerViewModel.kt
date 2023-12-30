package com.corryn.octave.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.corryn.octave.R
import com.corryn.octave.model.Song
import com.corryn.octave.model.SongUiDto
import com.corryn.octave.ui.factory.AlbumBitmapFactory
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

    private var repeat = false
    private var shuffle = false

    var isSearching = false

    // Emitting a value of null indicates there was an error trying to play the song.
    private val _nowPlayingBar: MutableSharedFlow<SongUiDto?> = MutableSharedFlow()
    val nowPlayingBar: SharedFlow<SongUiDto?> = _nowPlayingBar.asSharedFlow()

    private val _nowPlayingMessage: MutableSharedFlow<SongUiDto?> = MutableSharedFlow()
    val nowPlayingMessage: SharedFlow<SongUiDto?> = _nowPlayingMessage.asSharedFlow()

    private val _upNext: MutableSharedFlow<SongUiDto?> = MutableSharedFlow()
    val upNext: SharedFlow<SongUiDto?> = _upNext.asSharedFlow()

    private val _albumArt: MutableSharedFlow<Bitmap?> = MutableSharedFlow()
    val albumArt: SharedFlow<Bitmap?> = _albumArt.asSharedFlow()

    // Expects a string resource.
    private val _errorMessage: MutableSharedFlow<Int> = MutableSharedFlow()
    val errorMessage: SharedFlow<Int> = _errorMessage.asSharedFlow()

    fun preparePlayer(context: Context?) {
        player.setOnCompletionListener {
            nextSong(context)
        }
    }

    fun updateNowPlayingAndUpNext() {
        val currentSongInfo = songNowPlaying?.let {
            SongUiDto(it.title, it.artist)
        }
        val nextSongInfo = playlistNext()?.let {
            SongUiDto(it.title, it.artist)
        }

        viewModelScope.launch {
            _nowPlayingBar.emit(currentSongInfo)
            _upNext.emit(nextSongInfo)
        }
    }

    fun pauseSong() {
        player.pause()
    }

    fun unpauseSong() {
        player.start()
    }

    val isPaused: Boolean
        get() = !player.isPlaying

    val selectedSong: Song?
        get() = if (selected != -1) {
            songList[selected]
        } else null

    fun nextSong(context: Context?) {
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

    fun prevSong(context: Context?) {
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
        updateNowPlayingAndUpNext()
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

    fun shuffle(context: Context?) {
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

    fun setSong(songIndex: Int, context: Context?) {
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
            player.setDataSource(context ?: throw IOException(), uri)
            player.prepare()
            player.start()
            if (repeat) {
                player.isLooping = true
            }
            nowPlayingIndex = songIndex
            songNowPlaying = temp

            val song = activeList!![songIndex]!!
            val currentSongInfo = SongUiDto(song.title, song.artist)
            val nextSongInfo = playlistNext()?.let {
                SongUiDto(it.title, it.artist)
            }

            viewModelScope.launch {
                _nowPlayingBar.emit(currentSongInfo)
                _nowPlayingMessage.emit(currentSongInfo)
                _upNext.emit(nextSongInfo)
            }
        } catch (e: IOException) {
            viewModelScope.launch {
                _nowPlayingBar.emit(null)
                _upNext.emit(null)
                _errorMessage.emit(R.string.file_not_found_error)
            }
        }
    }

    private fun setSong(s: Song?, context: Context?) {
        val uri: Uri? = try {
            Uri.parse(fileUriPrefix + s!!.data)
        } catch (e: Exception) {
            return
        }
        try {
            player.reset()
            player.setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build())
            player.setDataSource(context ?: throw IOException(), uri ?: throw IOException())
            player.prepare()
            player.start()
            if (repeat) {
                player.isLooping = true
            }
            nowPlayingIndex = activeList!!.indexOf(s)
            songNowPlaying = s

            val currentSongInfo = SongUiDto(s.title, s.artist)
            val nextSongInfo = playlistNext()?.let {
                SongUiDto(it.title, it.artist)
            }

            viewModelScope.launch {
                _nowPlayingBar.emit(currentSongInfo)
                _nowPlayingMessage.emit(currentSongInfo)
                _upNext.emit(nextSongInfo)
            }
        } catch (e: IOException) {
            viewModelScope.launch {
                _nowPlayingBar.emit(null)
                _upNext.emit(null)
                _errorMessage.emit(R.string.file_not_found_error)
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

    fun getAlbumArt(song: Song?, context: Context?) {
        context ?: return // Can't do much with a null context...

        val roundedAlbumArt = song?.let {
            val albumArtBitmap = factory.getAlbumArt(context, song.albumId)
            return@let factory.getRoundedCornerBitmap(albumArtBitmap, 50)
        } ?: BitmapFactory.decodeResource(context.resources, R.drawable.octave) // Default to the app logo if null.

        viewModelScope.launch {
            _albumArt.emit(roundedAlbumArt)
        }
    }

    companion object {
        private const val fileUriPrefix = "file:///"
    }

}