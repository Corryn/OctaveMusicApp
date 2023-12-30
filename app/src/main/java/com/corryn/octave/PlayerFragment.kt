package com.corryn.octave

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.corryn.octave.databinding.FragmentPlayerBinding
import com.corryn.octave.model.SongUiDto
import com.corryn.octave.ui.MusicFragment
import com.corryn.octave.ui.base.BaseFragment
import com.corryn.octave.viewmodel.PlayerViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.abs

// TODO Landscape version of music menu not working out of box, maybe because it isn't its own fragment?
// TODO Load music metadata on request per artist, album, etc.
// TODO Error dialog instead of error toast?
class PlayerFragment : BaseFragment<FragmentPlayerBinding>() {

    override val viewBindingInflater: (LayoutInflater, ViewGroup?, Boolean) -> FragmentPlayerBinding
        get() = FragmentPlayerBinding::inflate

    private val vM: PlayerViewModel by activityViewModels()

    private var x1 = 0f
    private var x2 = 0f
    private var y1 = 0f
    private var y2 = 0f

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Required for the text marquee to function.
        binding.playerNowPlaying.isSelected = true
        binding.playlistUpNext.isSelected = true

        setUpControlButtons()
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
        playerTitleBar.setOnTouchListener(swipeListener)
        mainart.setOnTouchListener(swipeListener)
    }

    private val swipeListener = View.OnTouchListener { view, event ->
        handleTouchEvent(event)
        true
    }

    private fun noSongPicked() {
        Toast.makeText(context, getString(R.string.no_song_active_message), Toast.LENGTH_SHORT).show()
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

                when {
                    abs(deltaY) > MIN_DISTANCE && y1 < y2 -> openMenu() // Swipe down
                    abs(deltaY) > MIN_DISTANCE && y1 > y2 -> { // Swipe up
                        // Do nothing
                    }

                    abs(deltaX) > MIN_DISTANCE && x1 < x2 -> prevSongClick() // Swipe right
                    abs(deltaX) > MIN_DISTANCE && x1 > x2 -> nextSongClick() // Swipe left
                    else -> { // Neutral tap
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
            binding.pause.isActivated = true
        } else {
            vM.pauseSong()
            binding.pause.isActivated = false
        }
    }

    private fun openMenu() {
        childFragmentManager.commit {
            setCustomAnimations(R.anim.slideinmenu, 0, 0, R.anim.slideoutmenu)
            addToBackStack(null)
            setReorderingAllowed(true)
            replace<MusicFragment>(binding.musicFragmentContainer.id)
        }
    }

    private fun nextSongClick() {
        if (vM.getNowPlaying() != null) {
            vM.nextSong(context)
            binding.pause.isActivated = true
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

    // Sets the play/pause button as well. TODO Could separate that out?
    private fun setNowPlaying(song: SongUiDto?) {
        binding.pause.isActivated = song != null
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

    private fun showErrorToast(@StringRes errorRes: Int) {
        Toast.makeText(context, errorRes, Toast.LENGTH_SHORT).show()
    }

    companion object {
        const val MIN_DISTANCE = 75
    }
}