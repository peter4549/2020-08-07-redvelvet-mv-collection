package com.grand.duke.elliot.kim.kotlin.redvelvetmvcollection

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import kotlinx.android.synthetic.main.fragment_exit_dialog.view.*

class ExitDialogFragment: DialogFragment() {
    @SuppressLint("InflateParams")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireContext())
        val view = requireActivity().layoutInflater.inflate(R.layout.fragment_exit_dialog, null)
        builder.setView(view)

        view.button_ok.setOnClickListener {
            (requireActivity() as MainActivity).finish()
            dismiss()
        }

        view.ad_view.loadAd(AdRequest.Builder().build())
        val adListener = object : AdListener() {
            @Suppress("DEPRECATION")
            override fun onAdFailedToLoad(p0: Int) {
                println("$TAG: onAdFailedToLoad")
                super.onAdFailedToLoad(p0)
            }

            override fun onAdLoaded() {
                super.onAdLoaded()
                println("$TAG: onAdLoaded")
            }
        }

        view.ad_view.adListener = adListener

        return builder.create()
    }

    companion object {
        private const val TAG = "ExitDialogFragment"
    }
}