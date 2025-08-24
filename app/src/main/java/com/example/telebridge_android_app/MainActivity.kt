package com.example.telebridge_android_app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
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

// Utils
import com.example.telebridge_android_app.utils.readSms
import com.example.telebridge_android_app.utils.readContacts
import com.example.telebridge_android_app.utils.readCallLogs
import com.example.telebridge_android_app.utils.uploadToFirebase
import com.google.firebase.database.FirebaseDatabase

class MainActivity : ComponentActivity() {

    private val permissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        permissions.forEach { (perm, granted) ->
            Log.d("Permissions", "$perm granted=$granted")
        }
    }

    private val firebaseDatabase by lazy {
        FirebaseDatabase.getInstance("https://telebridge-fc798-default-rtdb.firebaseio.com/")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Demande des permissions runtime
        requestPermissions()

        // Test Firebase
        val testRef = firebaseDatabase.getReference("test")
        testRef.setValue("Firebase connecté ✅")
        Log.d("FirebaseTest", "Donnée envoyée à Firebase")

        // UI Compose
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
        }
    }
}

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

        // Bouton SMS
        Button(
            onClick = {
               /* val smsList = readSms(activity).ifEmpty {
                    listOf(
                        mapOf("address" to "123456789", "body" to "Test SMS", "date" to "2025-08-21"),
                        mapOf("address" to "987654321", "body" to "Hello World", "date" to "2025-08-20")
                    )
                }
                val smsRef = database.getReference("sms")
                smsRef.setValue(smsList)
                Log.d("FirebaseUpload", "SMS envoyés : ${smsList.size}")*/
                val smsList = readSms(activity)
                val smsRef = database.getReference("sms")
                smsRef.setValue(smsList)
                Log.d("FirebaseUpload", "Sms envoyés : ${smsList.size}")
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Text("📩 Exporter SMS")
        }

        // Bouton Contacts
        Button(
            /*onClick = {
                val contactsList = readContacts(activity).ifEmpty {
                    listOf(
                        mapOf("name" to "Alice", "number" to "123456789"),
                        mapOf("name" to "Bob", "number" to "987654321")
                    )
                }
                val contactsRef = database.getReference("contacts")
                contactsRef.setValue(contactsList)
                Log.d("FirebaseUpload", "Contacts envoyés : ${contactsList.size}")
            },*/
            onClick = {
                val contactsList = readContacts(activity)
                val contactsRef = database.getReference("contacts")
                contactsRef.setValue(contactsList)
                Log.d("FirebaseUpload", "Contacts envoyés : ${contactsList.size}")
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Text("👤 Exporter Contacts")
        }

        // Bouton Appels
        Button(
            onClick = {
               /* val callsList = readCallLogs(activity).ifEmpty {
                    listOf(
                        mapOf("number" to "123456789", "type" to "INCOMING", "date" to "2025-08-21", "duration" to "60"),
                        mapOf("number" to "987654321", "type" to "MISSED", "date" to "2025-08-20", "duration" to "0")
                    )
                }
                val callsRef = database.getReference("calls")
                callsRef.setValue(callsList)
                Log.d("FirebaseUpload", "Appels envoyés : ${callsList.size}")*/
                val callsList = readCallLogs(activity)
                val callsRef = database.getReference("calls")
                callsRef.setValue(callsList)
                Log.d("FirebaseUpload", "Appels envoyés : ${callsList.size}")
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Text("📞 Exporter Historique Appels")
        }
    }
}
