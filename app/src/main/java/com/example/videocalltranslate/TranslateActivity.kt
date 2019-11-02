package com.example.videocalltranslate

import android.os.Bundle
import android.os.StrictMode
import android.view.View
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.translate.Translate
import com.google.cloud.translate.TranslateOptions
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import java.io.IOException



class TranslateActivity : AppCompatActivity() {

    lateinit var translate : Translate

    lateinit var inputText : EditText
    lateinit var outputText: EditText


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_translate)

        getTranslateService()

        val sourceLang : String = "en"
        val targetLang : String = "pt"

        inputText  = findViewById(R.id.translate_input_text)
        outputText  = findViewById(R.id.translate_output_text)


        val button : Button = findViewById(R.id.translate_translate_button)

        button.setOnClickListener(object : View.OnClickListener{
            override fun onClick(v: View?) {
                translate(targetLang)
            }
        })

    }

    fun translate(targetLang : String) {

        //Get input text to be translated:
        var originalText = inputText.getText().toString()
        val translation = translate.translate(
            originalText,
            Translate.TranslateOption.targetLanguage(targetLang),
            Translate.TranslateOption.model("base")
        )
        var translatedText = translation.getTranslatedText()

        //Translated text and original text are set to TextViews:
        outputText.setText(translatedText)

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