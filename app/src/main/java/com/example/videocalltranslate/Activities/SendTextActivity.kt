package com.example.videocalltranslate.Activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.example.videocalltranslate.R
import kotlinx.android.synthetic.main.activity_send_text.*

class SendTextActivity: AppCompatActivity(){
    lateinit var translatesText: EditText
    lateinit var originalText: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_send_text)

        translatesText = translated_text
        originalText = spoken_text
        getTextToSend()

        send_button.setOnClickListener(object : View.OnClickListener{
            override fun onClick(v: View?) {
                toShare()
            }
        })
    }

    fun getTextToSend(){
        originalText.setText(intent.getStringExtra("SpokenText"))
    }

    fun toShare(){
        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, originalText.text.toString())
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, null)
        startActivity(shareIntent)
    }
}