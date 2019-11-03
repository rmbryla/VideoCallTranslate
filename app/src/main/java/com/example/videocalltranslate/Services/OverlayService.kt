package com.example.videocalltranslate.Services

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.StrictMode
import android.service.carrier.CarrierMessagingService
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.telephony.SmsManager
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.example.videocalltranslate.Activities.SendTextActivity
import com.example.videocalltranslate.R
import com.example.videocalltranslate.Utils.StoredData
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.translate.Translate
import com.google.cloud.translate.TranslateOptions
import java.io.IOException
import java.util.*

class OverlayService : Service(), View.OnClickListener, View.OnTouchListener {


    private lateinit var  params: WindowManager.LayoutParams
    private lateinit var windowManager: WindowManager
    private lateinit var overlayButton:ImageButton

    val speechRecognizer: SpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
    val mSpeechRecognizerIntent: Intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
    var spokenText: String = ""

    lateinit var translate : Translate

    private var recording = false;

    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var moving = false

    @SuppressLint("ServiceCast")
    override fun onCreate() {
        super.onCreate()
        Toast.makeText(this,"Service Initialized", Toast.LENGTH_LONG).show()

        mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        speechRecognizer.setRecognitionListener(object: RecognitionListener {
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

                var translated = translate(StoredData.targetLanguage)
                if (translated != "") toShare(translated)
                else Toast.makeText(applicationContext,"Try Again", Toast.LENGTH_LONG)
            }

        })

        getTranslateService()

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        overlayButton = ImageButton(this)
        overlayButton.setImageResource(R.drawable.mic_button)
        overlayButton.setOnClickListener(this)
        overlayButton.setOnTouchListener(this)

        val layoutFlag = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }

        params = WindowManager.LayoutParams(
            200,
            200,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        params.gravity = Gravity.TOP or Gravity.START

        params.x = resources.displayMetrics.widthPixels
        params.y = 100

        windowManager.addView(overlayButton, params)


    }


    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onClick(v: View?) {
        if (moving){
            return
        }

        if (!recording) {
            speechRecognizer.startListening(mSpeechRecognizerIntent)
            overlayButton.setImageResource(R.drawable.recording_button)
        } else {
            speechRecognizer.stopListening()
            overlayButton.setImageResource(R.drawable.mic_button)

        }

        recording = !recording

    }


    fun toShare(translatedText : String){

        val intent : Intent = Intent(Intent.ACTION_VIEW)
        intent.putExtra(SmsManager.EXTRA_MMS_DATA, translatedText)
        intent.setType("vnd.android-dir/mms-sms")
        intent.putExtra("address", StoredData.phoneNumber);
        intent.putExtra("sms_body", translatedText)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        startActivity(intent)


//        val sendIntent: Intent = Intent().apply {
//            action = Intent.ACTION_SEND
//            putExtra(Intent.EXTRA_TEXT, translatedText)
//
//            type = "text/plain"
//        }
//        val shareIntent = Intent.createChooser(sendIntent, null)
//        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
//        //shareIntent.putExtra(Intent.EXTRA_CHOOSER_TARGETS, myChooserTargetArray)
//        startActivity(shareIntent)
    }


    override fun onTouch(view: View?, event: MotionEvent?): Boolean {
        view!!.performClick()

        when(event!!.action) {
            MotionEvent.ACTION_DOWN -> {
                initialX = params.x
                initialY = params.y
                initialTouchX = event.rawX
                initialTouchY = event.rawY
                moving=true
            }
            MotionEvent.ACTION_UP -> {
                moving = false
                if (params.y > resources.displayMetrics.heightPixels - 500) {
                    val service = Intent(applicationContext, OverlayService::class.java)
                    stopService(service)
                }
            }
            MotionEvent.ACTION_MOVE -> {
                params.x = initialX + (event!!.rawX - initialTouchX).toInt()
                params.y = initialY + (event!!.rawY - initialTouchY).toInt()
                windowManager.updateViewLayout(overlayButton, params)
            }
        }
        return true
    }


    /**
     * copy these two functions to use translate functionality
     */
    fun translate(targetLang : String) : String{

        //Get input text to be translated:
        var originalText = spokenText
        val translation = translate.translate(
            originalText,
            Translate.TranslateOption.targetLanguage(targetLang),
            Translate.TranslateOption.model("base")
        )
        var translatedText = translation.getTranslatedText()

        //Translated text and original text are set to TextViews:
        return translatedText

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