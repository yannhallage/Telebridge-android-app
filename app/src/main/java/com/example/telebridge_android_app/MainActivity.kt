package com.example.telebridge_android_app

import android.Manifest
import android.content.ContentResolver
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import android.provider.Telephony
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.telebridge_android_app.ui.theme.TelebridgeandroidappTheme
import com.example.telebridge_android_app.utils.readCallLogs
import com.example.telebridge_android_app.utils.readContacts
import com.example.telebridge_android_app.utils.readSms
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import java.util.UUID

class MainActivity : ComponentActivity() {

    private val permissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        permissions.forEach { (perm, granted) ->
            Log.d("Permissions", "$perm granted=$granted")
        }
        // Si toutes les permissions sont accordées, démarrer la synchro automatique
        startAutoSync()
    }

    private val firebaseDatabase by lazy {
        FirebaseDatabase.getInstance("https://telebridge-fc798-default-rtdb.firebaseio.com/")
    }

    private val userId = "user_demo" // TODO: remplacer par ID unique utilisateur

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestPermissions()

        setContent {
            TelebridgeandroidappTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    HomeScreen(this, firebaseDatabase)
                }
            }
        }
    }

    private fun requestPermissions() {
        val requiredPermissions = arrayOf(
            Manifest.permission.READ_SMS,
            Manifest.permission.SEND_SMS,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.READ_PHONE_STATE
        )

        val permissionsToRequest = requiredPermissions.filter {
            checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (permissionsToRequest.isNotEmpty()) {
            permissionsLauncher.launch(permissionsToRequest)
        } else {
            // Permissions déjà accordées
            startAutoSync()
        }
    }

    // ------------------------- Synchronisation -------------------------

    private fun startAutoSync() {
        val resolver = contentResolver

        // Observer SMS
        resolver.registerContentObserver(
            Telephony.Sms.CONTENT_URI,
            true,
            object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) {
                    super.onChange(selfChange)
                    Log.d("Sync", "📩 Nouveau SMS détecté")
                    syncSms()
                }
            }
        )

        // Observer Contacts
        resolver.registerContentObserver(
            ContactsContract.Contacts.CONTENT_URI,
            true,
            object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) {
                    super.onChange(selfChange)
                    Log.d("Sync", "👥 Nouveau contact détecté")
                    syncContacts()
                }
            }
        )

        // Observer Appels
        resolver.registerContentObserver(
            android.provider.CallLog.Calls.CONTENT_URI,
            true,
            object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) {
                    super.onChange(selfChange)
                    Log.d("Sync", "📞 Nouvel appel détecté")
                    syncCalls()
                }
            }
        )
    }

    // ------------------------- Fonctions de synchro -------------------------

    private fun syncSms() {
        val smsList = readSms(this)
        val batchData = hashMapOf<String, Any>()
        smsList.forEach { sms ->
            val id = UUID.randomUUID().toString()
            batchData["/users/$userId/sms/$id"] = sms + mapOf("timestamp" to ServerValue.TIMESTAMP)
        }
        firebaseDatabase.reference.updateChildren(batchData)
            .addOnSuccessListener { Log.d("FirebaseUpload", "✅ SMS synchronisés") }
            .addOnFailureListener { e -> Log.e("FirebaseUpload", "❌ Erreur SMS", e) }
    }

    private fun syncContacts() {
        val contactsList = readContacts(this)
        val batchData = hashMapOf<String, Any>()
        contactsList.forEach { contact ->
            val id = UUID.randomUUID().toString()
            batchData["/users/$userId/contacts/$id"] =
                contact + mapOf("timestamp" to ServerValue.TIMESTAMP)
        }
        firebaseDatabase.reference.updateChildren(batchData)
            .addOnSuccessListener { Log.d("FirebaseUpload", "✅ Contacts synchronisés") }
            .addOnFailureListener { e -> Log.e("FirebaseUpload", "❌ Erreur Contacts", e) }
    }

    private fun syncCalls() {
        val callsList = readCallLogs(this)
        val batchData = hashMapOf<String, Any>()
        callsList.forEach { call ->
            val id = UUID.randomUUID().toString()
            batchData["/users/$userId/calls/$id"] = call + mapOf("timestamp" to ServerValue.TIMESTAMP)
        }
        firebaseDatabase.reference.updateChildren(batchData)
            .addOnSuccessListener { Log.d("FirebaseUpload", "✅ Appels synchronisés") }
            .addOnFailureListener { e -> Log.e("FirebaseUpload", "❌ Erreur Appels", e) }
    }
}

// ------------------------- Composable -------------------------

@Composable
fun HomeScreen(activity: ComponentActivity, database: FirebaseDatabase) {

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Telebridge - Dashboard", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                // Export initial complet
                val smsList = readSms(activity)
                val contactsList = readContacts(activity)
                val callsList = readCallLogs(activity)

                val batchData = hashMapOf<String, Any>()
                val userId = "user_demo"

                smsList.forEach { sms ->
                    val id = UUID.randomUUID().toString()
                    batchData["/users/$userId/sms/$id"] = sms + mapOf("timestamp" to ServerValue.TIMESTAMP)
                }
                contactsList.forEach { contact ->
                    val id = UUID.randomUUID().toString()
                    batchData["/users/$userId/contacts/$id"] =
                        contact + mapOf("timestamp" to ServerValue.TIMESTAMP)
                }
                callsList.forEach { call ->
                    val id = UUID.randomUUID().toString()
                    batchData["/users/$userId/calls/$id"] = call + mapOf("timestamp" to ServerValue.TIMESTAMP)
                }

                database.reference.updateChildren(batchData)
                    .addOnSuccessListener {
                        Log.d(
                            "FirebaseUpload",
                            "✅ Export initial terminé : SMS=${smsList.size}, Contacts=${contactsList.size}, Appels=${callsList.size}"
                        )
                    }
                    .addOnFailureListener { e -> Log.e("FirebaseUpload", "❌ Erreur export initial", e) }

            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Text("📤 Exporter toutes les données")
        }
    }
}
