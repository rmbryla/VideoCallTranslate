package com.example.videocalltranslate.Dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import com.example.videocalltranslate.R
import com.example.videocalltranslate.Services.OverlayService

class SettingsDialog : DialogFragment() {

    lateinit var startOverlayButton: Button
    var overlayOn = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val builder : AlertDialog.Builder = AlertDialog.Builder(activity)

        val inflater : LayoutInflater = activity!!.layoutInflater
        val view : View = inflater.inflate(R.layout.dialog_settings,null)

        startOverlayButton = view.findViewById(R.id.start_overlay_button)


        var canDraw = true
        var intent : Intent? = null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
            canDraw = Settings.canDrawOverlays(context)
            if (!canDraw && intent != null) {
                startActivity(intent)
            }
        }


        startOverlayButton.setOnClickListener(object : View.OnClickListener{
            override fun onClick(v: View?) {
                val service = Intent(context, OverlayService::class.java)

            }
        })

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