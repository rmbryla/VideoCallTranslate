package com.example.videocalltranslate.Utils

object StoredData {

    lateinit var targetLanguage : String
    var phoneNumber : String =""
    var contacts = ArrayList<ContactCard>()

    fun getLangMap() : HashMap<String, String> {
        val map = HashMap<String, String>()
        map.put("Portuguese", "pt")
        map.put("English", "en")
        map.put("Spanish", "es")
        return map
    }

    public fun addContact(name : String, num : String) {
        contacts.add(ContactCard(name, num))
    }

}