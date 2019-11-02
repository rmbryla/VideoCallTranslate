package com.example.videocalltranslate

import com.google.cloud.translate.Translate
import com.google.cloud.translate.TranslateOptions
import com.google.cloud.translate.Translation

class APIInteracter {
    fun getApiConnection(){
        val translate : Translate = TranslateOptions.getDefaultInstance().getService()
        val text : String = "test text"

        val translation : Translation = translate.translate(text,
                                                    Translate.TranslateOption.sourceLanguage("en"),
                                                    Translate.TranslateOption.targetLanguage("pt"))

    }

}