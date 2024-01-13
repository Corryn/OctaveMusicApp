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
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.corryn.octave.R
import com.corryn.octave.databinding.ActivityOctaveBinding
import com.corryn.octave.model.MusicUiDto
import com.corryn.octave.ui.base.BaseActivity
import com.corryn.octave.ui.dialog.StoragePermissionDeniedDialog
import com.corryn.octave.ui.dialog.StoragePermissionRationaleDialog
import com.corryn.octave.viewmodel.PlayerViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

// TODO Convert noSongPicked to be a VM flow emit?
// TODO Figure out how to set current song as active media (for car, etc.)
// TODO Notification media controls?
class OctaveActivity: BaseActivity<ActivityOctaveBinding>(), StoragePermissionRationaleDialog.RationaleDialogListener {

    override val viewBindingInflater: (LayoutInflater) -> ActivityOctaveBinding
        get() = ActivityOctaveBinding::inflate

    private val vM: PlayerViewModel by viewModels()

    private val storagePermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission(), ::onStoragePermissionRequestResult)

    private var currentDialog: DialogFragment? = null

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

                launch {
                    vM.playlistUpdatedMessage.collect {
                        showPlaylistUpdatedToast(it)
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
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        currentDialog?.dismiss()
    }

    // Returns a boolean indicating whether permission had to be requested.
    private fun askForExternalStoragePermission(skipRationale: Boolean = false): Boolean {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
            if (skipRationale.not() && shouldShowRequestPermissionRationale(permission)) {
                currentDialog?.dismiss()
                currentDialog = StoragePermissionRationaleDialog().also {
                    it.show(supportFragmentManager, StoragePermissionRationaleDialog.fragmentTag)
                }
            } else {
                storagePermissionLauncher.launch(permission)
            }

            return true
        }

        return false
    }

    private fun onStoragePermissionRequestResult(isGranted: Boolean) {
        if (isGranted) {
            onResume()
        } else {
            currentDialog?.dismiss()
            currentDialog = StoragePermissionDeniedDialog().also {
                it.show(supportFragmentManager, StoragePermissionDeniedDialog.fragmentTag)
            }
        }
    }

    override fun onRationalePositive() {
        askForExternalStoragePermission(skipRationale = true)
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

    private fun showNowPlayingToast(song: MusicUiDto.SongUiDto?) {
        val toastMessage = if (song != null) {
            getString(R.string.now_playing, song.songName, song.artistName)
        } else {
            null
        }

        toastMessage?.let {
            Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
        }
    }

    private fun showPlaylistUpdatedToast(song: MusicUiDto.SongUiDto) {
        val message = getString(R.string.added_to_queue, song.songName)
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

}