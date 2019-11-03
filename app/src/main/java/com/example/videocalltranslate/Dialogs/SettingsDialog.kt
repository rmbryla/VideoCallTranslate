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
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import com.example.videocalltranslate.R
import com.example.videocalltranslate.Services.OverlayService
import com.example.videocalltranslate.Utils.Callback
import com.example.videocalltranslate.Utils.StoredData
import kotlinx.android.synthetic.main.dialog_settings.*
import kotlin.coroutines.coroutineContext

class SettingsDialog : DialogFragment{

    var done : Callback

    constructor(done : Callback) {
        this.done = done

    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val builder : AlertDialog.Builder = AlertDialog.Builder(activity)

        val inflater : LayoutInflater = activity!!.layoutInflater
        val view : View = inflater.inflate(R.layout.dialog_settings,null)

        val spinner : Spinner= view.findViewById(R.id.settings_language_spinner)
        val phoneNumberText : EditText = view.findViewById(R.id.settings_phone_number)

        var canDraw = true
        var intent : Intent? = null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
            canDraw = Settings.canDrawOverlays(context)
            if (!canDraw && intent != null) {
                startActivity(intent)
            }
        }

        phoneNumberText.setText(StoredData.phoneNumber)

        ArrayAdapter.createFromResource(context, R.array.languages, android.R.layout.simple_spinner_item).also{ adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinner.adapter=adapter
        }
        var languageMap = StoredData.getLangMap()

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {
            }

            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                var target : String = languageMap[spinner.selectedItem.toString()] ?: "en"
                StoredData.targetLanguage=target
            }
        }







        builder.setView(view).setTitle("Settings").setNegativeButton("Cancel", object : DialogInterface.OnClickListener {
            override fun onClick(dialog: DialogInterface?, which: Int) {

            }
        })
            .setPositiveButton("Ok", object : DialogInterface.OnClickListener {
                override fun onClick(dialog: DialogInterface?, which: Int) {
                    StoredData.targetLanguage = languageMap[spinner.selectedItem.toString()] ?:"en"
                    StoredData.phoneNumber = phoneNumberText.text.toString()
                    done.onDone()
                }
            })

        return builder.create()
    }

}