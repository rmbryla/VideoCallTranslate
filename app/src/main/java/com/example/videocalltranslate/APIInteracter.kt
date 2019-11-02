package com.example.videocalltranslate

import com.google.cloud.translate.Translate
import com.google.cloud.translate.TranslateOptions
import com.google.cloud.translate.Translation

class APIInteracter : Thread{
    lateinit var callback: Done

    lateinit var output : String

    lateinit var input : String
    lateinit var sourceLang: String
    lateinit var targetLang: String


    constructor(done : Done) {
        this.callback=done
    }

    lateinit var text : String

    fun initialize(input:String, sourceLang : String, targetLang : String) {
        this.input=input
        this.sourceLang=sourceLang
        this.targetLang=targetLang
    }

    fun translateText(){
        val translate : Translate = TranslateOptions.getDefaultInstance().getService()

        val translation : Translation = translate.translate(this.input,
                                                    Translate.TranslateOption.sourceLanguage(this.sourceLang),
                                                    Translate.TranslateOption.targetLanguage(this.targetLang))
        this.output =  translation.translatedText
    }


    override fun run() {
        translateText()
        this.callback.onDone(this.output)
    }



}