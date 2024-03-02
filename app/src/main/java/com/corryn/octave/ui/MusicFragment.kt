package com.corryn.octave.ui

import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isNotEmpty
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.corryn.octave.R
import com.corryn.octave.databinding.FragmentMusicBinding
import com.corryn.octave.model.consts.PlayerAction
import com.corryn.octave.model.dto.MusicUiDto
import com.corryn.octave.ui.base.BaseFragment
import com.corryn.octave.viewmodel.PlayerViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

// TODO Figure out why landscape layout works but only when navigating to the fragment, not when changing orientation on the fragment
// TODO Album level view below artist (+ "all songs" meta-album)
class MusicFragment : BaseFragment<FragmentMusicBinding>() {

    override val viewBindingInflater: (LayoutInflater, ViewGroup?, Boolean) -> FragmentMusicBinding
        get() = FragmentMusicBinding::inflate

    private val vM: PlayerViewModel by activityViewModels()

    private val musicAdapter = MusicUiDtoAdapter(::onMusicItemClicked)

    private var viewingSongs = false
    private fun isSearching() = binding.searchBar.isNotEmpty()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeBackPressListener {
            handleBackPress()
        }

        setUpMenuInteractions()

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.CREATED) {
                launch {
                    vM.uiItems.collectLatest {
                        musicAdapter.submitList(it)
                    }
                }

                launch {
                    vM.selectedSong.collectLatest {
                        vM.getAlbumArt(context, it)
                    }
                }

                launch {
                    vM.albumArt.collectLatest {
                        setAlbumArt(it)
                    }
                }

                vM.showArtists()
            }
        }
    }

    private fun handleBackPress() {
        if (viewingSongs) {
            returnToArtistList()
        } else {
            parentFragmentManager.popBackStack()
        }
    }

    private fun onMusicItemClicked(item: MusicUiDto, action: PlayerAction) {
        when (item) {
            is MusicUiDto.ArtistUiDto -> {
                when (action) {
                    PlayerAction.TAP -> {
                        onArtistClicked(item.id)
                    }
                    else -> throw UnsupportedOperationException("Artist items do not support this action yet")
                }
            }

            is MusicUiDto.SongUiDto -> {
                when (action) {
                    PlayerAction.TAP -> {
                        onSongClicked(item.id)
                    }
                    PlayerAction.PLAY -> {
                        onSongPlayClicked(item.id)
                    }
                    PlayerAction.ADD -> {
                        onSongAddClicked(item.id)
                    }
                }
            }
        }
    }

    private fun setUpMenuInteractions() = with(binding) {
        playerMenuList.apply {
            layoutManager = LinearLayoutManager(context)
            addItemDecoration(DividerItemDecoration(context, LinearLayoutManager.VERTICAL).apply {
                ContextCompat.getDrawable(context, R.drawable.blank_divider)?.let {
                    setDrawable(it)
                }
            })
            adapter = musicAdapter
        }

        searchBar.apply {
            editText?.doAfterTextChanged { text ->
                val searchString = text.toString().trim { it <= ' ' }
                vM.filterUi(searchString)
            }
        }
    }

    private fun returnToArtistList() = with(binding) {
        searchBar.editText?.text?.clear()

        vM.showArtists()
        setAlbumArt(null)
        viewingSongs = false
    }

    private fun onArtistClicked(artistId: Long) {
        binding.searchBar.editText?.text?.clear()

        vM.selectArtist(artistId)

        viewingSongs = true
    }

    private fun onSongClicked(songId: Long) {
        vM.selectSongById(songId)
    }

    // TODO Some of these behaviors should be internalized to the viewmodel
    private fun onSongPlayClicked(songId: Long) {
        when {
            isSearching() -> {
                vM.setSong(context, songId)
            }

            vM.playlistIsEmpty().not() -> {
                vM.setSong(context, songId)
            }

            else -> {
                vM.activeList = musicAdapter.currentList
                vM.setSong(context, songId)
            }
        }
    }

    private fun onSongAddClicked(songId: Long) {
        vM.activeList = musicAdapter.currentList
        vM.addToPlaylist(songId)
    }

    private fun setAlbumArt(bitmap: Bitmap?) {
        binding.playerMenuArt?.setImageBitmap(bitmap)
    }

}