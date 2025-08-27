package com.example.telebridge_android_app

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Base64
import android.util.Log
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import java.io.ByteArrayOutputStream
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

        // ✅ Récupération du logo de l’app en Base64
        val logoBase64 = getAppLogoBase64(packageName)

        Log.d("NotificationListener", "🔔 $packageName: $title - $text")

        // 🚀 Envoi vers Firebase
        userCode?.let { code ->
            val notifData = mapOf(
                "app" to packageName,
                "title" to title,
                "message" to text,
                "timestamp" to ServerValue.TIMESTAMP,
                "logo" to logoBase64
            )
            val id = UUID.randomUUID().toString()

            val ref = firebaseDatabase.reference.child("users/$code/notifications/$id")

            ref.updateChildren(notifData)
                .addOnSuccessListener {
                    Log.d("NotificationListener", "✅ Notif envoyée avec logo")
                }
                .addOnFailureListener { e ->
                    Log.e("NotificationListener", "❌ Erreur Firebase: ${e.message}")
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

    // ✅ Récupération du logo et encodage en Base64
    private fun getAppLogoBase64(packageName: String): String? {
        return try {
            val pm: PackageManager = applicationContext.packageManager
            val drawable: Drawable = pm.getApplicationIcon(packageName)

            val bitmap = if (drawable is BitmapDrawable) {
                drawable.bitmap
            } else {
                val bmp = Bitmap.createBitmap(
                    drawable.intrinsicWidth,
                    drawable.intrinsicHeight,
                    Bitmap.Config.ARGB_8888
                )
                val canvas = android.graphics.Canvas(bmp)
                drawable.setBounds(0, 0, canvas.width, canvas.height)
                drawable.draw(canvas)
                bmp
            }

            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            val byteArray = stream.toByteArray()
            Base64.encodeToString(byteArray, Base64.NO_WRAP)

        } catch (e: Exception) {
            Log.e("NotificationListener", "⚠️ Impossible de récupérer le logo: $packageName", e)
            null
        }
    }
}
