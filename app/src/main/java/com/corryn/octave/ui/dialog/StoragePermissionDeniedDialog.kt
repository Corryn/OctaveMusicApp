package com.corryn.octave.ui.dialog

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.corryn.octave.R

class StoragePermissionDeniedDialog : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            AlertDialog.Builder(it).apply {
                setTitle(R.string.storage_permission_denied_dialog_title)
                setMessage(R.string.storage_permission_denied_dialog_message)
                setPositiveButton(R.string.storage_permission_denied_dialog_positive) { dialog, id ->
                    dismiss()
                }
            }.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    companion object {
        const val fragmentTag = "storageDeniedDialog"
    }

}