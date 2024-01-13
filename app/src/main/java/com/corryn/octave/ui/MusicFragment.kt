package com.corryn.octave.ui

import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.corryn.octave.R
import com.corryn.octave.databinding.FragmentMusicBinding
import com.corryn.octave.model.MusicUiDto
import com.corryn.octave.model.consts.PlayerAction
import com.corryn.octave.ui.base.BaseFragment
import com.corryn.octave.viewmodel.PlayerViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

// TODO Album level view below artist (+ "all songs" meta-album)
class MusicFragment : BaseFragment<FragmentMusicBinding>(), TextView.OnEditorActionListener {

    override val viewBindingInflater: (LayoutInflater, ViewGroup?, Boolean) -> FragmentMusicBinding
        get() = FragmentMusicBinding::inflate

    private val vM: PlayerViewModel by activityViewModels()

    private val musicAdapter = MusicUiDtoAdapter(::onMusicItemClicked)

    private var viewingSongs = false

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
            setOnEditorActionListener(this@MusicFragment)
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                    val searchString = s.toString().trim { it <= ' ' }
                    vM.filterUi(searchString)
                    vM.isSearching = searchString != ""
                }

                override fun afterTextChanged(s: Editable) {}
            })
        }

        clearSearch.setOnClickListener { v ->
            val animation: Animation = AlphaAnimation(0.3f, 1.0f)
            animation.duration = 500
            v.startAnimation(animation)
            if (searchBar.text.toString() != "") {
                vM.isSearching = false
                searchBar.text?.clear()
            }
        }
    }

    private fun returnToArtistList() = with(binding) {
        searchBar.text?.clear()
        vM.isSearching = false

        val animation: Animation = AlphaAnimation(0.3f, 1.0f)
        animation.duration = 500
        playerMenuList.startAnimation(animation)

        vM.showArtists()
        setAlbumArt(null)
        viewingSongs = false
    }

    private fun onArtistClicked(artistId: Long) {
        val animation: Animation = AlphaAnimation(0.3f, 1.0f)
        animation.duration = 500
        binding.playerMenuList.startAnimation(animation)

        binding.searchBar.text.clear()

        vM.selectArtist(artistId)

        viewingSongs = true
    }

    private fun onSongClicked(songId: Long) {
        vM.selectSongById(songId)
    }

    // TODO These behaviors should be internalized to the viewmodel
    private fun onSongPlayClicked(songId: Long) {
        when {
            vM.isSearching -> {
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

    override fun onEditorAction(v: TextView, actionId: Int, event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_ENTER) {
            val imm = activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager

            // NOTE: In the author's example, he uses an identifier
            // called searchBar. If setting this code on your EditText
            // then use v.getWindowToken() as a reference to your
            // EditText is passed into this callback as a TextView
            imm?.hideSoftInputFromWindow(v.applicationWindowToken, InputMethodManager.HIDE_NOT_ALWAYS)
            // Must return true here to consume event
            return true
        }

        if (actionId == EditorInfo.IME_ACTION_NEXT || event.keyCode == KeyEvent.KEYCODE_ENTER) {
            return false
        } else {
            val searchString = v.text.toString().trim { it <= ' ' }
            vM.filterUi(searchString)
        }

        return true
    }

}