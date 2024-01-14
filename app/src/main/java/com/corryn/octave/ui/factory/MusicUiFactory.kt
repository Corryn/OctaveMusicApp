package com.corryn.octave.ui.factory

import com.corryn.octave.model.data.Artist
import com.corryn.octave.model.data.Song
import com.corryn.octave.model.dto.MusicUiDto
import com.corryn.octave.model.toDto

class MusicUiFactory {

    fun createArtistUiDtos(artists: List<Artist>): List<MusicUiDto.ArtistUiDto> {
        return artists.mapIndexed { index, artist -> artist.toDto(index) }
    }

    fun createSongUiDtos(songs: List<Song>): List<MusicUiDto.SongUiDto> {
        return songs.mapIndexed { index, song -> song.toDto(index) }
    }

}