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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.corryn.octave.databinding.FragmentPlayerBinding
import com.corryn.octave.model.MusicUiDto
import com.corryn.octave.ui.base.BaseFragment
import com.corryn.octave.viewmodel.PlayerViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.abs

// TODO Update the now playing bar when navigating back to this screen; maybe make the current song a stateflow?
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

        binding.downarrow.setOnClickListener { openMenu() }
        setUpTouchInteractions()

        setMainArtBackgroundResource(this.resources.configuration)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.CREATED) {
                launch {
                    vM.currentSong.collectLatest {
                        setNowPlaying(it)
                    }
                }

                launch {
                    vM.nextSong.collectLatest {
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

    private fun setUpTouchInteractions() = with(binding) {
        playerTitleBar.setOnTouchListener(swipeListener)
        mainart.setOnTouchListener(swipeListener)
    }

    private val swipeListener = View.OnTouchListener { view, event ->
        handleTouchEvent(event)
        true
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

                    abs(deltaX) > MIN_DISTANCE && x1 < x2 -> vM.prevSong(requireContext()) // Swipe right
                    abs(deltaX) > MIN_DISTANCE && x1 > x2 -> vM.nextSong(requireContext()) // Swipe left
                    else -> { // Neutral tap
                        when {
                            vM.getNowPlaying() != null -> vM.pause()
                            vM.playlistIsEmpty().not() -> vM.nextSong(requireContext())
                        }
                    }
                }
            }
        }

        return true
    }

    private fun openMenu() {
        findNavController().navigate(PlayerFragmentDirections.actionPlayerFragmentToMusicFragment())
    }

    private fun setNowPlaying(song: MusicUiDto.SongUiDto?) {
        binding.playerNowPlaying.text = if (song != null) {
            getString(R.string.now_playing, song.songName, song.artistName)
        } else {
            getString(R.string.now_playing_default)
        }
    }

    private fun setUpNext(song: MusicUiDto.SongUiDto?) {
        binding.playlistUpNext.apply {
            text = if (song != null) getString(R.string.up_next, song.songName, song.artistName) else ""
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