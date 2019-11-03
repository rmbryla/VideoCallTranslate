package com.example.videocalltranslate.Activities

import android.content.Context
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
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.fragment.app.DialogFragment
import com.example.videocalltranslate.Dialogs.SettingsDialog
import com.example.videocalltranslate.Services.OverlayService
import com.example.videocalltranslate.Utils.Callback
import com.example.videocalltranslate.Utils.ContactCard
import com.example.videocalltranslate.Utils.StoredData
import kotlinx.android.synthetic.main.item_contact.view.*
import java.lang.Exception
import java.security.Permission
import java.util.jar.Manifest
import kotlin.collections.ArrayList


class MainActivity : AppCompatActivity(), SearchView.OnQueryTextListener {

    val speechRecognizer: SpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
    val mSpeechRecognizerIntent: Intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
    var spokenText: String = ""

    var overlayActive = false

    lateinit var adapter:ContactListAdapter
    lateinit var contactSearch: SearchView
    lateinit var contactList: ListView

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


        var canDraw = true
        var intent : Intent? = null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
            canDraw = Settings.canDrawOverlays(applicationContext)
            if (!canDraw && intent != null) {
                startActivity(intent)
            }
        }

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



        adapter=ContactListAdapter(getContactList(), applicationContext)
        contact_list.adapter = adapter
        adapter.notifyDataSetChanged()
        search_view.setOnQueryTextListener(this)
        try {
            getContactList()
        } catch (e : Exception) {}

        settings_button.setOnClickListener(object : View.OnClickListener{
            override fun onClick(v: View?) {
                openSettingsDialog()
            }
        })

    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        return true
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        adapter.filter(newText ?: "")
        return false
    }

    private fun openSettingsDialog() {
        val settingsDialog : SettingsDialog = SettingsDialog(object : Callback{
            override fun onDone() {
                val service = Intent(this@MainActivity, OverlayService::class.java)
                startService(service)
            }
        })
        settingsDialog.show(supportFragmentManager, "Settings")
    }

    private fun getContactList(): ArrayList<ContactCard> {
        val cr = contentResolver
        val cur = cr.query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null)
        var phoneNumValue = ""

        if (cur?.count ?: 0 > 0) {
            while (cur != null && cur.moveToNext()) {
                val id = cur.getString(
                    cur.getColumnIndex(ContactsContract.Contacts._ID)
                )
                val name = cur.getString(
                    cur.getColumnIndex(
                        ContactsContract.Contacts.DISPLAY_NAME
                    )
                )
                val number = cur.getString(cur.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)).toInt()
                if (number > 0) {
                    val cursorPhone = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID + "=?", arrayOf(id), null)
                    if (cursorPhone.count > 0) {
                        while(cursorPhone.moveToNext()) {
                            phoneNumValue = cursorPhone.getString(cursorPhone.getColumnIndex(
                                ContactsContract.CommonDataKinds.Phone.NUMBER
                            ))
                            break

                        }
                    }
                }
                StoredData.addContact(name, phoneNumValue)
                val contact = View.inflate(applicationContext, R.layout.item_contact, null)
                contact.setOnClickListener(object : View.OnClickListener {
                    override fun onClick(v: View?) {
                        StoredData.phoneNumber=phoneNumValue
                        Toast.makeText(this@MainActivity, phoneNumValue, Toast.LENGTH_LONG).show()
                    }
                })
                contact.contact_name.text = name
                contact.contact_number.text = phoneNumValue
            }
        }
        cur?.close()
        return StoredData.contacts
    }

    private fun goToSendText(){
        val intent = Intent(this, SendTextActivity::class.java)
        intent.putExtra("SpokenText", spokenText)
        startActivity(intent)
        finish()
    }

    private fun checkPermission(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            val permissions = arrayOf(android.Manifest.permission.RECORD_AUDIO,
                android.Manifest.permission.READ_CONTACTS,
                android.Manifest.permission.SEND_SMS)
            val permissionsToRequest = ArrayList<String>()
            for(permission in permissions) {
                if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                    permissionsToRequest.add(permission)
                }
            }
            if(permissionsToRequest.isNotEmpty()) {
                val sender = arrayOfNulls<String>(permissionsToRequest.size)
                permissionsToRequest.toArray(sender)
                ActivityCompat.requestPermissions(this, sender, 1)
            }
        }
    }
}

class ContactListAdapter(val items: ArrayList<ContactCard>, val context : Context): BaseAdapter() {
    val itemsDisplaying = ArrayList<ContactCard>()

    init {
        itemsDisplaying.addAll(items)
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val holder:ContactCardViewHolder
        val view:View

        if (convertView == null) {
            view = LayoutInflater.from(context).inflate(R.layout.item_contact, null)
            holder = ContactCardViewHolder(view)
            view.setTag(holder)
        } else {
            view = convertView
            holder = convertView.getTag() as ContactCardViewHolder
        }
        val item = itemsDisplaying[position]
        holder.name.text=item.name
        holder.number.text=item.number

        view.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                StoredData.phoneNumber=item.number
                Toast.makeText(context, item.number, Toast.LENGTH_LONG).show()
            }
        })

        return view
    }

    // Filter Class
    fun filter(charText : String) {
        itemsDisplaying.clear()
        if (charText.length == 0) {
            itemsDisplaying.addAll(items)
        } else {
            for (contact in items) {
                if (contact.name.toLowerCase().contains(charText.toLowerCase())) {
                    itemsDisplaying.add(contact)
                }
            }
        }
        notifyDataSetChanged()
    }

    override fun getItem(position: Int): Any {
        return itemsDisplaying[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getCount(): Int {
        return itemsDisplaying.size
    }
}

class ContactCardViewHolder(view : View) {
    val name : TextView
    val number : TextView
    init {
        name=view.findViewById(R.id.contact_name)
        number=view.findViewById(R.id.contact_number)
    }
}
