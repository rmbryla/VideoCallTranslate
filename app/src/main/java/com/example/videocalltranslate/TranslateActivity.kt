package com.example.videocalltranslate

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread

class TranslateActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_translate)

        val sourceLang : String = "en"
        val targetLang : String = "pt"

        val inputText : EditText = findViewById(R.id.translate_input_text)
        val outputText : EditText = findViewById(R.id.translate_output_text)

        val myTranslater : APIInteracter = APIInteracter(object : Done{
            override fun onDone(text: String) {
                outputText.setText(text)
            }
        })

        val button : Button = findViewById(R.id.translate_translate_button)

        button.setOnClickListener(object : View.OnClickListener{
            override fun onClick(v: View?) {
                var translation : String
                    myTranslater.initialize(inputText.text.toString(), sourceLang, targetLang)
                    myTranslater.start()
            }
        })

    }
}