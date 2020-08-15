package com.grand.duke.elliot.kim.kotlin.redvelvetmvcollection

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import kotlinx.android.synthetic.main.fragment_exit_dialog.view.button_ok
import kotlinx.android.synthetic.main.fragment_notification_dialog.view.*

class NotificationDialogFragment(private val updateMessage: String,
                                 private val downloadLink: String,
                                 private val quitWhenNotUpdating: Boolean = false): DialogFragment() {

    @SuppressLint("InflateParams")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireContext())
        val view = requireActivity().layoutInflater.inflate(R.layout.fragment_notification_dialog, null)
        builder.setView(view)

        view.text_view_message.text = updateMessage

        view.button_cancel.setOnClickListener {
            if (quitWhenNotUpdating)
                (requireActivity() as MainActivity).finish()
            dismiss()
        }

        view.button_ok.setOnClickListener {
            // go to download.
            dismiss()
        }

        return builder.create()
    }
}