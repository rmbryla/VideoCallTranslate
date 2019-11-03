package com.example.videocalltranslate.Activities

import android.content.Intent
import android.os.Bundle
import android.os.StrictMode
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import com.example.videocalltranslate.R
import com.example.videocalltranslate.Utils.StoredData
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.translate.Translate
import com.google.cloud.translate.TranslateOptions
import kotlinx.android.synthetic.main.activity_send_text.*
import java.io.IOException

class SendTextActivity: AppCompatActivity(){
    lateinit var translatedText: EditText
    lateinit var originalText: EditText
    lateinit var translate : Translate
    lateinit var languageSpinner : Spinner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_send_text)

        getTranslateService()

        val languageMap = StoredData.getLangMap()

        translatedText = translated_text
        originalText = spoken_text
        getTextToSend()

        languageSpinner = findViewById(R.id.send_text_language_spinner)
        ArrayAdapter.createFromResource(this, R.array.languages, android.R.layout.simple_spinner_item).also{ adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            languageSpinner.adapter=adapter
        }

        send_button.setOnClickListener(object : View.OnClickListener{
            override fun onClick(v: View?) {
                toShare()
            }
        })

        originalText.doAfterTextChanged {
            var target : String = languageMap.get(languageSpinner.selectedItem.toString()) ?: "en"
            translate(target)
        }

        languageSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {
            }

            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                var target : String = languageMap[languageSpinner.selectedItem.toString()] ?: "en"
                translate(target)
            }
        }

        close_btn.setOnClickListener(object: View.OnClickListener{
            override fun onClick(v: View?) {
                onBackPressed()
            }
        })

    }

    fun getTextToSend(){
        var text : String = intent.getStringExtra("SpokenText")
        originalText.setText(text)
        translate("pt")
    }

    fun toShare(){
        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, translatedText.text.toString())
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, null)
        startActivity(shareIntent)
    }


    /**
     * copy these two functions to use translate functionality
     */
    fun translate(targetLang : String) {

        //Get input text to be translated:
        var originalText = originalText.getText().toString()
        val translation = translate.translate(
            originalText,
            Translate.TranslateOption.targetLanguage(targetLang),
            Translate.TranslateOption.model("base")
        )
        var translatedText = translation.getTranslatedText()

        //Translated text and original text are set to TextViews:
        this.translatedText.setText(translatedText)

    }


    fun getTranslateService() {

        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        try {

            getResources().openRawResource(R.raw.a6c615c1be4e).use{ `is` ->

                //Get credentials:
                val myCredentials = GoogleCredentials.fromStream(`is`)

                //Set credentials and get translate service:
                val translateOptions =
                    TranslateOptions.newBuilder().setCredentials(myCredentials).build()
                translate = translateOptions.service

            }
        } catch (ioe: IOException) {
            ioe.printStackTrace()

        }

    }

}