package com.corryn.octave.ui.dialog

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.corryn.octave.R

class StoragePermissionRationaleDialog : DialogFragment() {

    interface RationaleDialogListener {
        fun onRationalePositive()
    }

    private var listener: RationaleDialogListener? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            AlertDialog.Builder(it).apply {
                setTitle(R.string.storage_permission_rationale_dialog_title)
                setMessage(R.string.storage_permission_rationale_dialog_message)
                setPositiveButton(R.string.storage_permission_rationale_dialog_positive) { dialog, id ->
                    dismiss()
                    listener?.onRationalePositive()
                }
            }.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        // Verify that the host activity implements the callback interface.
        try {
            listener = context as RationaleDialogListener
        } catch (e: ClassCastException) {
            // The activity doesn't implement the interface. Throw exception.
            throw ClassCastException("$context must implement RationaleDialogListener")
        }
    }

    companion object {
        const val fragmentTag = "storageRationaleDialog"
    }

}
