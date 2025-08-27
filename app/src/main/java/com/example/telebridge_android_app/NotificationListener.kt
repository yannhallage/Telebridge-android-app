package com.example.telebridge_android_app

import android.content.pm.ApplicationInfo
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import java.util.*

class NotificationListener : NotificationListenerService() {

    private val firebaseDatabase by lazy {
        FirebaseDatabase.getInstance("https://telebridge-fc798-default-rtdb.firebaseio.com/")
    }

    companion object {
        var userCode: String? = null
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageName = sbn.packageName

        // 🔎 Vérifier si c'est une app système
        if (isSystemApp(packageName)) {
            Log.d("NotificationListener", "⚠️ Ignoré (système): $packageName")
            return
        }

        val extras = sbn.notification.extras
        val title = extras.getString("android.title") ?: ""
        val text = extras.getCharSequence("android.text")?.toString() ?: ""

        // 🔎 Si notif vide, on ignore
        if (title.isBlank() && text.isBlank()) {
            Log.d("NotificationListener", "⚠️ Ignoré (vide) de $packageName")
            return
        }

        Log.d("NotificationListener", "🔔 $packageName: $title - $text")

        // 🚀 Envoi vers Firebase (mise à jour au lieu de simple écriture)
        userCode?.let { code ->
            val notifData = mapOf(
                "app" to packageName,
                "title" to title,
                "message" to text,
                "timestamp" to ServerValue.TIMESTAMP
            )
            val id = UUID.randomUUID().toString()

            val ref = firebaseDatabase.reference.child("users/$code/notifications/$id")

            ref.updateChildren(notifData)  // 🔄 Mise à jour
                .addOnSuccessListener {
                    Log.d("NotificationListener", "✅ Notif mise à jour sur Firebase")
                }
                .addOnFailureListener { e ->
                    Log.e("NotificationListener", "❌ Erreur maj Firebase: ${e.message}")
                }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        Log.d("NotificationListener", "❌ Notification retirée: ${sbn.packageName}")
    }

    // ✅ Détection app système
    private fun isSystemApp(packageName: String): Boolean {
        return try {
            val pm = applicationContext.packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0 ||
                    (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
        } catch (e: Exception) {
            false
        }
    }
}
