package com.corryn.octave.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.corryn.octave.R
import com.corryn.octave.databinding.ActivityOctaveBinding
import com.corryn.octave.model.SongUiDto
import com.corryn.octave.ui.base.BaseActivity
import com.corryn.octave.viewmodel.PlayerViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

// TODO Convert noSongPicked to be a VM flow emit?
// TODO Permission request warning dialog if denied
// TODO Figure out how to set current song as active media (for car, etc.)
// TODO Notification media controls?
class OctaveActivity: BaseActivity<ActivityOctaveBinding>() {

    override val viewBindingInflater: (LayoutInflater) -> ActivityOctaveBinding
        get() = ActivityOctaveBinding::inflate

    private val vM: PlayerViewModel by viewModels()

    private val storagePermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission(), ::onStoragePermissionRequestResult)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        setUpControlButtons()

        askForExternalStoragePermission()

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                launch {
                    vM.playingState.collectLatest {
                        binding.controls.pause.isActivated = it
                    }
                }

                launch {
                    vM.nowPlayingMessage.collectLatest {
                        showNowPlayingToast(it)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        with(vM) {
            preparePlayer(this@OctaveActivity)
            updateNowPlayingAndUpNext()
            getAlbumArt(vM.selectedSong, this@OctaveActivity)
        }
    }

    // Returns a boolean indicating whether permission had to be requested.
    private fun askForExternalStoragePermission(): Boolean {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
            storagePermissionLauncher.launch(permission)
            return true
        }

        return false
    }

    private fun onStoragePermissionRequestResult(isGranted: Boolean) {
        if (isGranted) {
            onResume()
        } else {
            // TODO Warning dialog?
        }
    }

    private fun setUpControlButtons() = with(binding.controls) {
        pause.setOnClickListener {
            if (vM.getNowPlaying() != null) {
                vM.pause()
            } else if (!vM.playlistIsEmpty()) {
                vM.nextSong(this@OctaveActivity)
                pause.setImageResource(R.drawable.octavepause)
            } else {
                noSongPicked()
            }
        }

        next.setOnClickListener {
            if (vM.getNowPlaying() != null) {
                vM.nextSong(this@OctaveActivity)
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
                Toast.makeText(this@OctaveActivity, getString(R.string.repeat_on), Toast.LENGTH_SHORT).show()
                repeat.setImageResource(R.drawable.octaverepeatactive)
            } else {
                Toast.makeText(this@OctaveActivity, getString(R.string.repeat_off), Toast.LENGTH_SHORT).show()
                repeat.setImageResource(R.drawable.octaverepeat)
            }
        }

        shuffle.setOnClickListener {
            val res = vM.toggleShuffle()
            if (res) {
                Toast.makeText(this@OctaveActivity, getString(R.string.shuffle_on), Toast.LENGTH_SHORT).show()
                shuffle.setImageResource(R.drawable.octaveshuffleactive)
            } else {
                Toast.makeText(this@OctaveActivity, getString(R.string.shuffle_off), Toast.LENGTH_SHORT).show()
                shuffle.setImageResource(R.drawable.octaveshuffle)
            }
        }
    }

    private fun noSongPicked() {
        Toast.makeText(this, getString(R.string.no_song_active_message), Toast.LENGTH_SHORT).show()
    }

    private fun prevSongClick() {
        if (vM.playlistIsEmpty()) {
            vM.prevSong(this)
        } else {
            Toast.makeText(this, getString(R.string.next_prev_queue_message), Toast.LENGTH_SHORT).show()
        }
    }

    private fun showNowPlayingToast(song: SongUiDto?) {
        val toastMessage = if (song != null) {
            getString(R.string.now_playing, song.title, song.artist)
        } else {
            null
        }

        toastMessage?.let {
            Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
        }
    }

}