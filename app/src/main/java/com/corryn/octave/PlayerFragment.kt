package com.corryn.octave

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import android.widget.TextView.OnEditorActionListener
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.corryn.octave.databinding.FragmentPlayerBinding
import com.corryn.octave.model.Song
import com.corryn.octave.model.SongUiDto
import com.corryn.octave.ui.ArtistAdapter
import com.corryn.octave.ui.SongAdapter
import com.corryn.octave.ui.base.BaseFragment
import com.corryn.octave.viewmodel.PlayerViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.abs

// TODO Load music metadata on request per artist, album, etc.
// TODO Error dialog instead of error toast?
class PlayerFragment : BaseFragment<FragmentPlayerBinding>(), OnEditorActionListener {

    override val viewBindingInflater: (LayoutInflater, ViewGroup?, Boolean) -> FragmentPlayerBinding
        get() = FragmentPlayerBinding::inflate

    private val vM: PlayerViewModel by activityViewModels()

    private var viewingSongs = false

    private val songAdapter = SongAdapter(::onSongClicked, ::onPlayClicked, ::onAddClicked)
    private val artistAdapter: ArtistAdapter = ArtistAdapter(::onArtistClicked)

    private var x1 = 0f
    private var x2 = 0f
    private var y1 = 0f
    private var y2 = 0f

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeBackPressListener {
            handleBackPressed()
        }

        // Required for the text marquee to function.
        binding.playerNowPlaying.isSelected = true
        binding.playlistUpNext.isSelected = true

        setUpControlButtons()
        setUpMenuInteractions()
        setUpTouchInteractions()

        setMainArtBackgroundResource(this.resources.configuration)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.CREATED) {
                launch {
                    vM.nowPlayingBar.collectLatest {
                        setNowPlaying(it)
                    }
                }

                launch {
                    vM.nowPlayingMessage.collectLatest {
                        showNowPlayingToast(it)
                    }
                }

                launch {
                    vM.upNext.collectLatest {
                        setUpNext(it)
                    }
                }

                launch {
                    vM.albumArt.collectLatest {
                        setAlbumArt(it)
                    }
                }

                launch {
                    vM.errorMessage.collectLatest {
                        showErrorToast(it)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        with(vM) {
            preparePlayer(context)
            updateNowPlayingAndUpNext()
            getAlbumArt(vM.selectedSong, context)

            if (selected != -1) {
                binding.playerMenu.playerMenuList.scrollToPosition(vM.selected)
            }
        }
    }

    private fun isMenuOpen(): Boolean {
        return binding.playerMenu.root.isVisible
    }

    private fun setUpMenuInteractions() = with(binding.playerMenu) {
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
            setOnEditorActionListener(this@PlayerFragment)
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

    private fun setUpControlButtons() = with(binding) {
        downarrow.setOnClickListener { openMenu() }

        pause.setOnClickListener {
            if (vM.getNowPlaying() != null) {
                pause()
            } else if (!vM.playlistIsEmpty()) {
                vM.nextSong(context)
                pause.setImageResource(R.drawable.octavepause)
            } else {
                noSongPicked()
            }
        }

        next.setOnClickListener {
            if (vM.getNowPlaying() != null) {
                nextSongClick()
            } else {
                noSongPicked()
            }
        }

        previous.setOnClickListener {
            if (vM.getNowPlaying() != null) {
                prevSongClick()
            } else {
                noSongPicked()
            }
        }

        repeat.setOnClickListener {
            val res = vM.toggleRepeat()
            if (res) {
                Toast.makeText(context, getString(R.string.repeat_on), Toast.LENGTH_SHORT).show()
                repeat.setImageResource(R.drawable.octaverepeatactive)
            } else {
                Toast.makeText(context, getString(R.string.repeat_off), Toast.LENGTH_SHORT).show()
                repeat.setImageResource(R.drawable.octaverepeat)
            }
        }

        shuffle.setOnClickListener {
            val res = vM.toggleShuffle()
            if (res) {
                Toast.makeText(context, getString(R.string.shuffle_on), Toast.LENGTH_SHORT).show()
                shuffle.setImageResource(R.drawable.octaveshuffleactive)
            } else {
                Toast.makeText(context, getString(R.string.shuffle_off), Toast.LENGTH_SHORT).show()
                shuffle.setImageResource(R.drawable.octaveshuffle)
            }
        }
    }

    private fun setUpTouchInteractions() = with(binding) {
        root.setOnTouchListener(object : View.OnTouchListener {
            override fun onTouch(view: View?, event: MotionEvent?): Boolean {
                handleTouchEvent(event)

                return true
            }
        })

        mainart.setOnTouchListener { _, _ -> false }
    }

    private fun noSongPicked() {
        Toast.makeText(context, getString(R.string.no_song_active_message), Toast.LENGTH_SHORT).show()
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
            binding.playerMenu.playerMenuList.apply {
                adapter = songAdapter.also {
                    it.submitList(vM.filterSongs(searchString))
                }
            }
        }

        return true
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        // Checks the orientation of the screen
        setMainArtBackgroundResource(newConfig)
    }

    private fun setMainArtBackgroundResource(config: Configuration) = with(binding) {
        when (config.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> mainart.setImageResource(R.drawable.octavesplashlandscape)
            Configuration.ORIENTATION_PORTRAIT -> mainart.setImageResource(R.drawable.octavesplashportrait)
        }
    }

    private fun handleBackPressed() {
        if (isMenuOpen()) {
            if (viewingSongs) {
                returnToArtistList()
            } else {
                closeMenu()
            }
        } else {
            findNavController().popBackStack()
        }
    }

    private fun handleTouchEvent(event: MotionEvent?): Boolean {
        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                x1 = event.x
                y1 = event.y
            }

            MotionEvent.ACTION_UP -> {
                x2 = event.x
                y2 = event.y
                val deltaX = x2 - x1
                val deltaY = y2 - y1
                if (abs(deltaY) > MIN_DISTANCE && y1 < y2) {
                    openMenu()
                } else if (abs(deltaY) > MIN_DISTANCE && y1 > y2) {
                    closeMenu()
                } else if (abs(deltaX) > MIN_DISTANCE && x1 < x2) {
                    prevSongClick()
                } else if (abs(deltaX) > MIN_DISTANCE && x1 > x2) {
                    nextSongClick()
                } else {
                    if (isMenuOpen().not()) {
                        if (!vM.playlistIsEmpty()) {
                            nextSongClick()
                        } else if (vM.getNowPlaying() != null) {
                            pause()
                        }
                    }
                }
            }
        }

        return true
    }

    private fun pause() {
        if (vM.isPaused) {
            vM.unpauseSong()
            binding.pause.setImageResource(R.drawable.octavepause)
        } else {
            vM.pauseSong()
            binding.pause.setImageResource(R.drawable.octaveplay)
        }
    }

    private fun openMenu() {
        if (isMenuOpen().not()) {
            binding.playerMenu.root.isVisible = true

            val animationSlideIn = AnimationUtils.loadAnimation(context, R.anim.slideinmenu)
            binding.playerMenu.root.startAnimation(animationSlideIn)
        }
    }

    private fun closeMenu() {
        if (isMenuOpen()) {
            val animationSlideOut = AnimationUtils.loadAnimation(context, R.anim.slideoutmenu)

            binding.playerMenu.root.apply {
                startAnimation(animationSlideOut)
                isVisible = false
            }
        }
    }

    private fun returnToArtistList() = with(binding.playerMenu) {
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

    private fun nextSongClick() {
        if (vM.getNowPlaying() != null) {
            vM.nextSong(context)
            binding.pause.setImageResource(R.drawable.octavepause)
        }
    }

    private fun prevSongClick() {
        if (vM.getNowPlaying() != null) {
            if (vM.playlistIsEmpty()) {
                vM.prevSong(context)
                binding.pause.setImageResource(R.drawable.octavepause)
            } else {
                Toast.makeText(context, getString(R.string.next_prev_queue_message), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setNowPlaying(song: SongUiDto?) {
        binding.playerNowPlaying.text = if (song != null) {
            getString(R.string.now_playing, song.title, song.artist)
        } else {
            getString(R.string.now_playing_default)
        }
    }

    private fun showNowPlayingToast(song: SongUiDto?) {
        val toastMessage = if (song != null) {
            getString(R.string.now_playing, song.title, song.artist)
        } else {
            null
        }

        toastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
        }
    }

    private fun setUpNext(song: SongUiDto?) {
        binding.playlistUpNext.apply {
            text = if (song != null) getString(R.string.up_next, song.title, song.artist) else ""
            isVisible = text.isNotBlank()
        }
    }

    private fun setAlbumArt(bitmap: Bitmap?) {
        binding.playerMenu.playerMenuArt?.setImageBitmap(bitmap)
    }

    private fun onArtistClicked(artist: String) {
        val animation: Animation = AlphaAnimation(0.3f, 1.0f)
        animation.duration = 500
        binding.playerMenu.playerMenuList.startAnimation(animation)

        val artistSongs: List<Song> = vM.byArtistList[artist] ?: return

        binding.playerMenu.playerMenuList.adapter = songAdapter.also {
            it.submitList(artistSongs)
        }

        vM.viewedList = artistSongs

        with(binding.playerMenu) {
            artistLabel.visibility = View.INVISIBLE
            searchBar.visibility = View.VISIBLE
            clearSearch.visibility = View.VISIBLE
        }

        viewingSongs = true
    }

    private fun onSongClicked(song: Song, position: Int) {
        if (vM.playClicked) {
            binding.pause.setImageResource(R.drawable.octavepause)
            vM.playClicked = false
        }

        vM.selected = position
        vM.getAlbumArt(song, context)
    }

    private fun onPlayClicked(song: Song, activeList: List<Song>) {
        if (vM.isSearching) {
            vM.activeList = vM.viewedList
            vM.setSong(vM.getSongIndex(song), context)
        } else if (!vM.playlistIsEmpty()) {
            val temp: List<Song?>? = vM.activeList
            vM.activeList = activeList
            vM.setSong(vM.getSongIndex(song), context)
            vM.activeList = temp
        } else {
            vM.activeList = activeList
            vM.setSong(vM.getSongIndex(song), context)
        }

        vM.playClicked = true
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

    private fun showErrorToast(@StringRes errorRes: Int) {
        Toast.makeText(context, errorRes, Toast.LENGTH_SHORT).show()
    }

    companion object {
        const val MIN_DISTANCE = 75
    }
}