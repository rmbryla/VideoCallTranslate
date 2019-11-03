package com.example.videocalltranslate.Dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import androidx.fragment.app.DialogFragment
import com.example.videocalltranslate.R

class SettingsDialog : DialogFragment() {

    lateinit var startOverlayButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val builder : AlertDialog.Builder = AlertDialog.Builder(activity)

        val inflater : LayoutInflater = activity!!.layoutInflater
        val view : View = inflater.inflate(R.layout.dialog_settings,null)

        startOverlayButton = view.findViewById(R.id.start_overlay_button)


        builder.setView(view).setTitle("Settings").setNegativeButton("Cancel", object : DialogInterface.OnClickListener {
            override fun onClick(dialog: DialogInterface?, which: Int) {

            }
        })
            .setPositiveButton("Ok", object : DialogInterface.OnClickListener {
                override fun onClick(dialog: DialogInterface?, which: Int) {

                }
            })

        return builder.create()
    }

}