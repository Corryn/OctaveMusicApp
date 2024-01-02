package com.corryn.octave.ui.dialog

import android.Manifest
import android.app.AlertDialog
import android.app.Dialog
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.corryn.octave.R
import com.corryn.octave.viewmodel.PlayerViewModel

// TODO Communicate dialog choice back to OctaveActivity and handle it from there? Eliminates code repetition
class StoragePermissionRationaleDialog : DialogFragment() {

    private val vM: PlayerViewModel by activityViewModels()

    private val storagePermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission(), ::onStoragePermissionRequestResult)

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            AlertDialog.Builder(it).apply {
                setTitle(R.string.storage_permission_rationale_dialog_title)
                setMessage(R.string.storage_permission_rationale_dialog_message)
                setPositiveButton(R.string.storage_permission_rationale_dialog_positive) { dialog, id ->
                    dismiss()
                    askForExternalStoragePermission()
                }
            }.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    private fun askForExternalStoragePermission() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        storagePermissionLauncher.launch(permission)
    }

    private fun onStoragePermissionRequestResult(isGranted: Boolean) {
        with(vM) {
            preparePlayer(requireContext())
            updateNowPlayingAndUpNext()
            getAlbumArt(vM.selectedSong, requireContext())
        }
    }

    companion object {
        const val fragmentTag = "storageRationaleDialog"
    }

}
