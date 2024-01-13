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
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.corryn.octave.R
import com.corryn.octave.databinding.FragmentMusicBinding
import com.corryn.octave.model.Song
import com.corryn.octave.ui.base.BaseFragment
import com.corryn.octave.viewmodel.PlayerViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MusicFragment: BaseFragment<FragmentMusicBinding>(), TextView.OnEditorActionListener {

    override val viewBindingInflater: (LayoutInflater, ViewGroup?, Boolean) -> FragmentMusicBinding
        get() = FragmentMusicBinding::inflate

    private val vM: PlayerViewModel by activityViewModels()

    private val artistAdapter: ArtistAdapter = ArtistAdapter(::onArtistClicked)
    private val songAdapter = SongAdapter(::onSongClicked, ::onPlayClicked, ::onAddClicked)

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
                    vM.albumArt.collectLatest {
                        setAlbumArt(it)
                    }
                }
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

    private fun setUpMenuInteractions() = with(binding) {
        playerMenuList.apply {
            layoutManager = LinearLayoutManager(context)
            addItemDecoration(DividerItemDecoration(context, LinearLayoutManager.VERTICAL).apply {
                ContextCompat.getDrawable(context, R.drawable.blank_divider)?.let {
                    setDrawable(it)
                }
            })
            adapter = artistAdapter.also {
                it.submitList(vM.artistList)
            }
        }

        searchBar.apply {
            setOnEditorActionListener(this@MusicFragment)
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                    val searchString = s.toString().trim { it <= ' ' }
                    playerMenuList.apply {
                        adapter = songAdapter.also {
                            it.submitList(vM.filterSongs(searchString))
                        }
                    }
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

    override fun onResume() {
        super.onResume()

        if (vM.selected != -1) {
            binding.playerMenuList.scrollToPosition(vM.selected)
        }
    }

    private fun returnToArtistList() = with(binding) {
        searchBar.text?.clear()
        vM.isSearching = false

        searchBar.visibility = View.INVISIBLE
        clearSearch.visibility = View.INVISIBLE
        artistLabel.visibility = View.VISIBLE

        vM.viewedList = null

        val animation: Animation = AlphaAnimation(0.3f, 1.0f)
        animation.duration = 500
        playerMenuList.startAnimation(animation)

        playerMenuList.adapter = artistAdapter
        songAdapter.submitList(emptyList())
        setAlbumArt(null)
        viewingSongs = false
    }

    private fun onArtistClicked(artistId: Long) {
        val animation: Animation = AlphaAnimation(0.3f, 1.0f)
        animation.duration = 500
        binding.playerMenuList.startAnimation(animation)

        val artistSongs: List<Song> = vM.byArtistList[artistId] ?: return

        binding.playerMenuList.adapter = songAdapter.also {
            it.submitList(artistSongs)
        }

        vM.viewedList = artistSongs

        with(binding) {
            artistLabel.visibility = View.INVISIBLE
            searchBar.visibility = View.VISIBLE
            clearSearch.visibility = View.VISIBLE
        }

        viewingSongs = true
    }

    private fun onSongClicked(song: Song, position: Int) {
        vM.selected = position
        vM.getAlbumArt(song, context)
    }

    private fun onPlayClicked(song: Song, activeList: List<Song>) {
        when {
            vM.isSearching -> {
                vM.activeList = vM.viewedList
                vM.setSong(vM.getSongIndex(song), context)
            }
            vM.playlistIsEmpty().not() -> {
                val temp: List<Song?>? = vM.activeList
                vM.activeList = activeList
                vM.setSong(vM.getSongIndex(song), context)
                vM.activeList = temp
            }
            else -> {
                vM.activeList = activeList
                vM.setSong(vM.getSongIndex(song), context)
            }
        }
    }

    private fun onAddClicked(song: Song, activeList: List<Song>) {
        if (vM.isSearching) {
            vM.activeList = vM.viewedList
        } else {
            vM.activeList = activeList
        }

        vM.addToPlaylist(song)

        val message = getString(R.string.added_to_queue, song.title)
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
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
            binding.playerMenuList.apply {
                adapter = songAdapter.also {
                    it.submitList(vM.filterSongs(searchString))
                }
            }
        }

        return true
    }

}