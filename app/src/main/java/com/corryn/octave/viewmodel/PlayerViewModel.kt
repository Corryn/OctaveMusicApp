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
import com.corryn.octave.model.Artist
import com.corryn.octave.model.Song
import com.corryn.octave.model.SongUiDto
import com.corryn.octave.repository.MusicRepository
import com.corryn.octave.ui.factory.AlbumBitmapFactory
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.LinkedList
import java.util.Random

// TODO Separate error flow for error messages
class PlayerViewModel : ViewModel() {

    private val player = MediaPlayer()

    private val repository = MusicRepository()
    private val factory = AlbumBitmapFactory()

    private var songList: List<Song> = emptyList()
    var artistList: List<Artist> = emptyList()
    var byArtistList: Map<Long, List<Song>> = HashMap()
    var activeList: List<Song?>? = null
    var viewedList: List<Song>? = null

    private val playlist = LinkedList<Song>()

    private var nowPlayingIndex = -1
    var selected = -1

    private val isPaused: Boolean
        get() = !player.isPlaying

    private var repeat = false
    private var shuffle = false

    var isSearching = false

    // region Flows

    private val _playingState: MutableStateFlow<Boolean> = MutableStateFlow(true)
    val playingState: StateFlow<Boolean> = _playingState.asStateFlow()

    private val _currentSong: MutableStateFlow<SongUiDto?> = MutableStateFlow(null)
    val currentSong: StateFlow<SongUiDto?> = _currentSong.asStateFlow()

    private val _nextSong: MutableStateFlow<SongUiDto?> = MutableStateFlow(null)
    val nextSong: StateFlow<SongUiDto?> = _nextSong.asStateFlow()

    private val _nowPlayingMessage: MutableSharedFlow<SongUiDto?> = MutableSharedFlow()
    val nowPlayingMessage: SharedFlow<SongUiDto?> = _nowPlayingMessage.asSharedFlow()

    private val _albumArt: MutableSharedFlow<Bitmap?> = MutableSharedFlow()
    val albumArt: SharedFlow<Bitmap?> = _albumArt.asSharedFlow()

    // endregion

    // Expects a string resource.
    private val _errorMessage: MutableSharedFlow<Int> = MutableSharedFlow()
    val errorMessage: SharedFlow<Int> = _errorMessage.asSharedFlow()

    fun preparePlayer(context: Context?) {
        byArtistList = repository.createArtistSongHashMap(context).also {
            artistList = makeArtistList(it).sortedBy { artist -> artist.name }
            songList = it.values.flatten()
        }

        player.setOnCompletionListener {
            nextSong(context)
        }
    }

    // TODO Temporary, remove the need for rebuilding an artist list when we already kinda do it during song list build
    private fun makeArtistList(songMap: Map<Long, List<Song>>): List<Artist> {
        val artists = mutableListOf<Artist>()

        for(artist in songMap) {
            artists.add(Artist(artist.key, artist.value.first().artist))
        }

        return artists
    }

    // The current song is always set by playing it, but the next song can be set without playing it immediately.
    // Therefore, we can't just rely on it being updated by the setSong method.
    fun updateNowPlayingAndUpNext() {
        val currentSongInfo = _currentSong.value
        val nextSongInfo = getNextSongInfo()

        viewModelScope.launch {
            _playingState.emit(isPaused.not())
            _currentSong.emit(currentSongInfo)
            _nextSong.emit(nextSongInfo)
        }
    }

    fun pause() {
        if (isPaused) {
            player.start()
        } else {
            player.pause()
        }

        viewModelScope.launch {
            _playingState.emit(isPaused.not())
        }
    }

    val selectedSong: Song?
        get() = if (selected != -1) {
            songList[selected]
        } else null

    fun nextSong(context: Context?) {
        val song = playlist.removeFirstOrNull()

        when {
            song != null -> setSong(context, song)
            shuffle -> shuffle(context)
            nowPlayingIndex + 1 < activeList!!.size -> setSong(context, ++nowPlayingIndex)
            else -> {
                nowPlayingIndex = 0
                setSong(context, nowPlayingIndex)
            }
        }
    }

    fun prevSong(context: Context?) {
        if (playlist.isEmpty()) {
            when {
                shuffle -> shuffle(context)
                nowPlayingIndex - 1 >= 0 -> setSong(context, --nowPlayingIndex)
                else -> {
                    nowPlayingIndex = activeList!!.size - 1
                    setSong(context, nowPlayingIndex)
                }
            }
        }
    }

    fun playlistIsEmpty(): Boolean {
        return playlist.isEmpty()
    }

    fun addToPlaylist(s: Song) {
        playlist.add(s)

        val nextSongInfo = getNextSongInfo()

        viewModelScope.launch {
            _nextSong.emit(nextSongInfo)
        }
    }

    private fun shuffle(context: Context?) {
        val random: Int
        val r = Random()
        random = r.nextInt(activeList!!.size)
        setSong(context, random)
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

    // TODO Handle null song selection?
    fun setSong(context: Context?, songIndex: Int) {
        val song = activeList?.getOrNull(songIndex) ?: return
        playSong(context, song, songIndex)
    }

    // TODO Handle null song selection?
    private fun setSong(context: Context?, song: Song) {
        val songIndex = activeList?.indexOf(song) ?: return
        playSong(context, song, songIndex)
    }

    private fun playSong(context: Context?, song: Song, index: Int) {
        val uri = Uri.parse(fileUriPrefix + song.data)

        try {
            player.apply {
                reset()

                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                setDataSource(context ?: throw IOException(), uri)

                prepare()
                start()

                if (repeat) {
                    isLooping = true
                }
            }

            nowPlayingIndex = index

            val currentSongInfo = SongUiDto(song.id, song.title, song.artist)
            val nextSongInfo = getNextSongInfo()

            viewModelScope.launch {
                _playingState.emit(isPaused.not())
                _currentSong.emit(currentSongInfo)
                _nowPlayingMessage.emit(currentSongInfo)
                _nextSong.emit(nextSongInfo)
            }
        } catch (e: IOException) {
            viewModelScope.launch {
                _playingState.emit(false)
                _currentSong.emit(null)
                _nextSong.emit(null)
                _errorMessage.emit(R.string.file_not_found_error)
            }
        }
    }

    fun filterSongs(query: String): List<Song> {
        return viewedList!!.filter { it.title.contains(query, ignoreCase = true) }
    }

    fun getNowPlaying(): SongUiDto? {
        return _currentSong.value
    }

    private fun getNextSongInfo(): SongUiDto? {
        return playlist.peek()?.let {
            SongUiDto(it.id, it.title, it.artist)
        }
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