package com.corryn.octave

import android.content.Context
import android.content.res.Configuration
import android.graphics.BitmapFactory
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.TypedValue
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
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.TextView.OnEditorActionListener
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.corryn.octave.databinding.FragmentPlayerBinding
import com.corryn.octave.model.Song
import com.corryn.octave.ui.ArtistAdapter
import com.corryn.octave.ui.SongAdapter
import com.corryn.octave.ui.base.BaseFragment
import kotlin.math.abs

class PlayerFragment : BaseFragment<FragmentPlayerBinding>(), OnEditorActionListener {

    override val viewBindingInflater: (LayoutInflater, ViewGroup?, Boolean) -> FragmentPlayerBinding
        get() = FragmentPlayerBinding::inflate

    private val player = Player

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

        setUpControlButtons()
        setUpMenuInteractions()
        setUpTouchInteractions()

        setSongListLayout(this.resources.configuration.orientation)
    }

    override fun onResume() {
        super.onResume()

        player.updateContext(requireContext())
        updateCompletionListener()

        if (player.getNowPlaying() != null) {
            setNowPlaying()
        }

        if (player.selected != -1) {
            binding.playerMenuList.scrollToPosition(player.selected)
        }

        updateAlbumArt(player.selectedSong)
    }

    private fun isMenuOpen(): Boolean {
        return binding.playerMenu.isVisible
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
                it.submitList(player.artistList)
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
                            it.submitList(player.filterSongs(searchString))
                        }
                    }
                    player.isSearching = searchString != ""
                }

                override fun afterTextChanged(s: Editable) {}
            })
        }

        clearSearch.setOnClickListener { v ->
            val animation: Animation = AlphaAnimation(0.3f, 1.0f)
            animation.duration = 500
            v.startAnimation(animation)
            if (searchBar.text.toString() != "") {
                player.isSearching = false
                searchBar.text?.clear()
            }
        }

        downarrow.setOnClickListener { openMenu() }
    }

    private fun setUpControlButtons() = with(binding) {
        pause.setOnClickListener {
            if (player.getNowPlaying() != null) {
                pause()
            } else if (!player.playlistIsEmpty()) {
                player.nextSong()
                pause.setImageResource(R.drawable.octavepause)
                setUpNext()
            } else {
                noSongPicked()
            }
        }

        next.setOnClickListener {
            if (player.getNowPlaying() != null) {
                nextSongClick()
            } else {
                noSongPicked()
            }
        }

        previous.setOnClickListener {
            if (player.getNowPlaying() != null) {
                prevSongClick()
            } else {
                noSongPicked()
            }
        }

        repeat.setOnClickListener {
            val res = player.toggleRepeat()
            if (res) {
                Toast.makeText(requireContext(), "Repeat on", Toast.LENGTH_SHORT).show()
                repeat.setImageResource(R.drawable.octaverepeatactive)
            } else {
                Toast.makeText(requireContext(), "Repeat off", Toast.LENGTH_SHORT).show()
                repeat.setImageResource(R.drawable.octaverepeat)
            }
        }

        shuffle.setOnClickListener {
            val res = player.toggleShuffle()
            if (res) {
                Toast.makeText(requireContext(), "Shuffle on", Toast.LENGTH_SHORT).show()
                shuffle.setImageResource(R.drawable.octaveshuffleactive)
            } else {
                Toast.makeText(requireContext(), "Shuffle off", Toast.LENGTH_SHORT).show()
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
        Toast.makeText(requireContext(), "Swipe down and pick a song first!", Toast.LENGTH_SHORT).show()
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
                    it.submitList(player.filterSongs(searchString))
                }
            }
        }

        return true
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        // Checks the orientation of the screen
        setSongListLayout(newConfig.orientation)
    }

    private fun setSongListLayout(config: Int) = with(binding) {
        if (config == Configuration.ORIENTATION_LANDSCAPE) {
            playerMenuList.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 0.7f)

            val params = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.3f)
            val resources = requireContext().resources
            val px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5f, resources.displayMetrics).toInt()

            params.setMargins(px, px, px, px)
            playerMenuArt.layoutParams = params
            mainart.setImageResource(R.drawable.octavesplashlandscape)
        } else if (config == Configuration.ORIENTATION_PORTRAIT) {
            playerMenuList.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
            playerMenuArt.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0f)
            mainart.setImageResource(R.drawable.octavesplashportrait)
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
                        if (!player.playlistIsEmpty()) {
                            nextSongClick()
                        } else if (player.getNowPlaying() != null) {
                            pause()
                        }
                    }
                }
            }
        }

        return true
    }

    private fun pause() {
        if (player.isPaused) {
            player.unpauseSong()
            binding.pause.setImageResource(R.drawable.octavepause)
        } else {
            player.pauseSong()
            binding.pause.setImageResource(R.drawable.octaveplay)
        }
    }

    private fun openMenu() {
        if (isMenuOpen().not()) {
            binding.playerMenu.isVisible = true

            val animationSlideIn = AnimationUtils.loadAnimation(requireContext(), R.anim.slideinmenu)
            binding.playerMenu.startAnimation(animationSlideIn)
        }
    }

    private fun closeMenu() {
        if (isMenuOpen()) {
            setNowPlaying()
            setUpNext()

            val animationSlideOut = AnimationUtils.loadAnimation(requireContext(), R.anim.slideoutmenu)
            binding.playerMenu.apply {
                startAnimation(animationSlideOut)
                isVisible = false
            }
        }
    }

    private fun returnToArtistList() = with(binding) {
        searchBar.text?.clear()
        player.isSearching = false

        searchBar.visibility = View.INVISIBLE
        clearSearch.visibility = View.INVISIBLE
        artistLabel.visibility = View.VISIBLE

        player.viewedList = null

        val animation: Animation = AlphaAnimation(0.3f, 1.0f)
        animation.duration = 500
        playerMenuList.startAnimation(animation)

        playerMenuList.adapter = artistAdapter
        songAdapter.submitList(emptyList())
        updateAlbumArt(null)
        viewingSongs = false
    }

    private fun updateCompletionListener() {
        player.player.setOnCompletionListener {
            player.nextSong()
            setNowPlaying()
            setUpNext()
        }
    }

    private fun nextSongClick() {
        if (player.getNowPlaying() != null) {
            player.nextSong()
            setNowPlaying()
            setUpNext()
            binding.pause.setImageResource(R.drawable.octavepause)
        }
    }

    private fun prevSongClick() {
        if (player.getNowPlaying() != null) {
            if (player.playlistIsEmpty()) {
                player.prevSong()
                setNowPlaying()
                binding.pause.setImageResource(R.drawable.octavepause)
            } else {
                Toast.makeText(requireContext(), "Disabled during queue playback.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setNowPlaying() {
        val song = player.getNowPlaying()
        if (song != null) {
            binding.playerNowPlaying.text = getString(R.string.now_playing, song.title, song.artist)
        }
    }

    private fun setUpNext() = with(binding) {
        val song = player.playlistNext()
        if (song != null) {
            binding.playlistUpNext.visibility = View.VISIBLE
            binding.playlistUpNext.text = getString(R.string.up_next, song.title, song.artist)
        } else {
            binding.playlistUpNext.visibility = View.GONE
        }
    }

    private fun updateAlbumArt(song: Song?) = with(binding) {
        if (song != null) {
            val albumArt = player.getAlbumArt(activity?.contentResolver, song.albumId)
            if (albumArt != null) {
                playerMenuArt.setImageBitmap(player.getRoundedCornerBitmap(player.getAlbumArt(activity?.contentResolver, song.albumId), 50))
            } else {
                val logo = BitmapFactory.decodeResource(
                    requireContext().resources,
                    R.drawable.octave
                )
                playerMenuArt.setImageBitmap(player.getRoundedCornerBitmap(logo, 50))
            }
        } else {
            val logo = BitmapFactory.decodeResource(
                requireContext().resources,
                R.drawable.octave
            )
            playerMenuArt.setImageBitmap(player.getRoundedCornerBitmap(logo, 50))
        }
    }

    private fun onArtistClicked(artist: String) {
        val animation: Animation = AlphaAnimation(0.3f, 1.0f)
        animation.duration = 500
        binding.playerMenuList.startAnimation(animation)

        val artistSongs: List<Song> = player.byArtistList[artist] ?: return

        binding.playerMenuList.adapter = songAdapter.also {
            it.submitList(artistSongs)
        }

        player.viewedList = artistSongs

        binding.artistLabel.visibility = View.INVISIBLE
        binding.searchBar.visibility = View.VISIBLE
        binding.clearSearch.visibility = View.VISIBLE

        viewingSongs = true
    }

    private fun onSongClicked(song: Song, position: Int) {
        if (player.playClicked) {
            binding.pause.setImageResource(R.drawable.octavepause)
            player.playClicked = false
        }

        player.selected = position
        updateAlbumArt(song)
    }

    private fun onPlayClicked(song: Song, activeList: List<Song>) {
        if (player.isSearching) {
            player.activeList = player.viewedList
            player.setSong(player.getSongIndex(song))
        } else if (!player.playlistIsEmpty()) {
            val temp: List<Song?>? = player.activeList
            player.activeList = activeList
            player.setSong(player.getSongIndex(song))
            player.activeList = temp
        } else {
            player.activeList = activeList
            player.setSong(player.getSongIndex(song))
        }

        player.playClicked = true
    }

    private fun onAddClicked(song: Song, activeList: List<Song>) {
        if (player.isSearching) {
            player.activeList = player.viewedList
        } else {
            player.activeList = activeList
        }

        player.addToPlaylist(song)
        Toast.makeText(requireContext(), song.title + " added to queue!", Toast.LENGTH_SHORT).show()
    }

    companion object {
        const val MIN_DISTANCE = 75
    }
}