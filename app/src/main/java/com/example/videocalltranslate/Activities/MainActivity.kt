package com.example.videocalltranslate.Activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.example.videocalltranslate.R
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*
import android.provider.ContactsContract
import android.util.Log
import androidx.core.app.ActivityCompat
import com.example.videocalltranslate.Dialogs.SettingsDialog
import kotlinx.android.synthetic.main.item_contact.view.*


class MainActivity : AppCompatActivity() {

    val speechRecognizer: SpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
    val mSpeechRecognizerIntent: Intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
    var spokenText: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkPermission()

        mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        speechRecognizer.setRecognitionListener(object: RecognitionListener{
            override fun onReadyForSpeech(params: Bundle?) {}

            override fun onRmsChanged(rmsdB: Float) {}

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onPartialResults(partialResults: Bundle?) {}

            override fun onEvent(eventType: Int, params: Bundle?) {}

            override fun onBeginningOfSpeech() {}

            override fun onEndOfSpeech() {}

            override fun onError(error: Int) {}

            override fun onResults(results: Bundle?) {
                val arr = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                spokenText = arr?.get(0) ?: ""
                goToSendText()
            }

        })

        make_a_call.setOnClickListener(object: View.OnClickListener{
            override fun onClick(v: View?) {
                val intent = Intent(this@MainActivity, VideoActivity::class.java)
                startActivity(intent)
                finish()
            }
        })

        talk_button.setOnTouchListener(object : View.OnTouchListener{
            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                if(event?.action == MotionEvent.ACTION_DOWN){
                    speechRecognizer.startListening(mSpeechRecognizerIntent)
                }
                else if(event?.action == MotionEvent.ACTION_UP){
                    speechRecognizer.stopListening()
                }
                return false
            }
        })

        settings_button.setOnClickListener(object : View.OnClickListener{
            override fun onClick(v: View?) {
                val intent = Intent(this@MainActivity, SettingsActivity::class.java)
                startActivity(intent)
                finish()
            }
        })



    }

    private fun openSettingsDialog() {
        val settingsDialog : SettingsDialog = SettingsDialog()
        settingsDialog.show(supportFragmentManager, "Settings")
    }


    private fun goToSendText(){
        val intent = Intent(this, SendTextActivity::class.java)
        intent.putExtra("SpokenText", spokenText)
        startActivity(intent)
        finish()
    }

    private fun checkPermission(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED){
                val permissions = arrayOf(Manifest.permission.RECORD_AUDIO)
                ActivityCompat.requestPermissions(this, permissions, 1)
            }
        }
    }

}
