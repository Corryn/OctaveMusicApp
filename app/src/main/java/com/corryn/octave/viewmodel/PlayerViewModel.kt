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
import com.corryn.octave.model.data.Artist
import com.corryn.octave.model.data.Song
import com.corryn.octave.model.dto.MusicUiDto
import com.corryn.octave.model.toDto
import com.corryn.octave.repository.MusicRepository
import com.corryn.octave.ui.factory.AlbumBitmapFactory
import com.corryn.octave.ui.factory.MusicUiFactory
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

// TODO Should separate out MusicUiDtos and flow-used data-passing dtos (MusicUiDto for UI, something else for data passing)
// TODO Separate error flow for error messages
// TODO Option to limit scope of shuffling?
class PlayerViewModel : ViewModel() {

    private val player = MediaPlayer()

    private val repository = MusicRepository()
    private val uiFactory = MusicUiFactory()
    private val albumArtFactory = AlbumBitmapFactory()

    private var songList: Map<Long, Song> = HashMap()
    private var artists: List<Artist> = emptyList()
    private var artistToSongsMap: Map<Long, List<Song>> = HashMap()
    private val playlist = LinkedList<Song>()

    var activeList: List<MusicUiDto>? = null

    private val isPaused: Boolean
        get() = !player.isPlaying

    private var repeat = false
    private var shuffle = false

    // region Flows

    private val _uiItems: MutableStateFlow<List<MusicUiDto>> = MutableStateFlow(emptyList())
    val uiItems: StateFlow<List<MusicUiDto>> = _uiItems.asStateFlow()

    private val _playingState: MutableStateFlow<Boolean> = MutableStateFlow(true)
    val playingState: StateFlow<Boolean> = _playingState.asStateFlow()

    private val _selectedSong: MutableStateFlow<MusicUiDto.SongUiDto?> = MutableStateFlow(null)
    val selectedSong: StateFlow<MusicUiDto.SongUiDto?> = _selectedSong.asStateFlow()

    private val _currentSong: MutableStateFlow<MusicUiDto.SongUiDto?> = MutableStateFlow(null)
    val currentSong: StateFlow<MusicUiDto.SongUiDto?> = _currentSong.asStateFlow()

    private val _nextSong: MutableStateFlow<MusicUiDto.SongUiDto?> = MutableStateFlow(null)
    val nextSong: StateFlow<MusicUiDto.SongUiDto?> = _nextSong.asStateFlow()

    private val _nowPlayingMessage: MutableSharedFlow<MusicUiDto.SongUiDto?> = MutableSharedFlow()
    val nowPlayingMessage: SharedFlow<MusicUiDto.SongUiDto?> = _nowPlayingMessage.asSharedFlow()

    private val _playlistUpdatedMessage: MutableSharedFlow<MusicUiDto.SongUiDto> = MutableSharedFlow()
    val playlistUpdatedMessage: SharedFlow<MusicUiDto.SongUiDto> = _playlistUpdatedMessage.asSharedFlow()

    private val _albumArt: MutableSharedFlow<Bitmap?> = MutableSharedFlow()
    val albumArt: SharedFlow<Bitmap?> = _albumArt.asSharedFlow()

    // endregion

    // Expects a string resource.
    private val _errorMessage: MutableSharedFlow<Int> = MutableSharedFlow()
    val errorMessage: SharedFlow<Int> = _errorMessage.asSharedFlow()

    fun preparePlayer(context: Context?) {
        artistToSongsMap = repository.createArtistSongHashMap(context).also { songMap ->
            artists = makeArtistList(songMap).sortedBy { it.name }
            songList = songMap.values.flatten().associateBy { it.id }
        }

        player.setOnCompletionListener {
            nextSong(context)
        }
    }

    // TODO Temporary, remove the need for rebuilding an artist list when we already kinda do it during song list build
    private fun makeArtistList(songMap: Map<Long, List<Song>>): List<Artist> {
        val artists = mutableListOf<Artist>()

        for (artist in songMap) {
            artists.add(Artist(artist.key, artist.value.first().artist))
        }

        return artists
    }

    // Indicates selection, but not necessarily playing, of the song.
    fun selectSongById(id: Long) {
        viewModelScope.launch {
            _selectedSong.emit(songList[id]?.toDto())
        }
    }

    fun showArtists() {
        viewModelScope.launch {
            uiFactory.createArtistUiDtos(artists).also {
                activeList = it
                _uiItems.emit(it)
            }

        }
    }

    fun selectArtist(artistId: Long) {
        viewModelScope.launch {
            val songs = artistToSongsMap[artistId] ?: return@launch
            uiFactory.createSongUiDtos(songs).also {
                activeList = it
                _uiItems.emit(it)
            }
        }
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

    // TODO If a playlist ends and the active list contains that song, it can find the next one and continue.
    // TODO Consider ways to improve on this?
    fun nextSong(context: Context?) {
        val song = playlist.removeFirstOrNull()

        when {
            song != null -> setSong(context, song)
            shuffle -> shuffle(context)
            else -> { // Try to find the current song in the active list and play the next song. If we can't determine the next song, do nothing.
                val currentSongIndex = activeList?.indexOf(_currentSong.value ?: return) ?: return
                val nextSong = if (currentSongIndex + 1 == activeList?.size) { // Wrap check
                    activeList?.getOrNull(0)
                } else {
                    activeList?.getOrNull(currentSongIndex + 1)
                }

                // TODO Could support playing a whole album or artist I guess
                if (nextSong is MusicUiDto.SongUiDto) {
                    setSong(context, nextSong.id)
                }
            }
        }
    }

    fun prevSong(context: Context?) {
        if (playlist.isEmpty()) {
            when {
                shuffle -> shuffle(context)
                else -> { // Try to find the current song in the active list and play the previous song. If we can't determine the previous song, do nothing.
                    val currentSongIndex = activeList?.indexOf(_currentSong.value ?: return) ?: return
                    val prevSong = if (currentSongIndex - 1 == -1) { // Wrap check
                        val lastIndex = activeList?.lastIndex ?: -1
                        activeList?.getOrNull(lastIndex)
                    } else {
                        activeList?.getOrNull(currentSongIndex - 1)
                    }

                    // TODO Could support playing a whole album or artist I guess
                    if (prevSong is MusicUiDto.SongUiDto) {
                        setSong(context, prevSong.id)
                    }
                }
            }
        }
    }

    fun playlistIsEmpty(): Boolean {
        return playlist.isEmpty()
    }

    fun addToPlaylist(songId: Long) {
        val song = songList[songId] ?: return

        playlist.add(song)

        val addedSongInfo = song.toDto()
        val nextSongInfo = getNextSongInfo()

        viewModelScope.launch {
            _playlistUpdatedMessage.emit(addedSongInfo)
            _nextSong.emit(nextSongInfo)
        }
    }

    private fun shuffle(context: Context?) {
        val randomIndex = Random().nextInt(songList.size)
        setSong(context, randomIndex)
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
    fun setSong(context: Context?, songId: Long) {
        val song = songList[songId] ?: return
        playSong(context, song)
    }

    // TODO Handle null song selection?
    private fun setSong(context: Context?, songIndex: Int) {
        val song = songList.toList().getOrNull(songIndex)?.second ?: return
        playSong(context, song)
    }

    private fun setSong(context: Context?, song: Song) {
        playSong(context, song)
    }

    private fun playSong(context: Context?, song: Song) {
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

            val currentSongInfo = song.toDto()
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

    fun filterUi(query: String) {
        val filteredList = activeList?.filter {
            when (it) {
                is MusicUiDto.ArtistUiDto -> it.name.contains(query, ignoreCase = true)
                is MusicUiDto.SongUiDto -> it.songName.contains(query, ignoreCase = true)
            }
        } ?: return

        viewModelScope.launch {
            _uiItems.emit(filteredList)
        }
    }

    fun getNowPlaying(): MusicUiDto.SongUiDto? {
        return _currentSong.value
    }

    private fun getNextSongInfo(): MusicUiDto.SongUiDto? {
        return playlist.peek()?.toDto()
    }

    fun getAlbumArt(context: Context?, song: MusicUiDto.SongUiDto?) {
        context ?: return // Can't do much with a null context...

        val roundedAlbumArt = song?.let {
            val albumArtBitmap = albumArtFactory.getAlbumArt(context, song.albumId)
            return@let albumArtFactory.getRoundedCornerBitmap(albumArtBitmap, 50)
        } ?: BitmapFactory.decodeResource(context.resources, R.drawable.octave) // Default to the app logo if null.

        viewModelScope.launch {
            _albumArt.emit(roundedAlbumArt)
        }
    }

    companion object {
        private const val fileUriPrefix = "file:///"
    }

}