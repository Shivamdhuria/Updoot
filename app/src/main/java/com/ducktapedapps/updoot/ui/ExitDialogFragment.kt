package com.ducktapedapps.updoot.ui

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.ducktapedapps.updoot.R

class ExitDialogFragment : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog = AlertDialog.Builder(requireActivity())
            .setTitle(getString(R.string.exit_app))
            .setPositiveButton(getString(android.R.string.ok)) { _, _ -> requireActivity().finish() }
            .setNegativeButton(getString(android.R.string.cancel)) { dialogInterface, _ -> dialogInterface.dismiss() }
            .create()
}