package com.example.telebridge_android_app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.CallLog
import com.example.telebridge_android_app.utils.LocalStorageHelper

import android.provider.ContactsContract
import android.provider.Settings
import android.provider.Telephony
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.telebridge_android_app.ui.theme.TelebridgeandroidappTheme
import com.example.telebridge_android_app.utils.readCallLogs
import com.example.telebridge_android_app.utils.readContacts
import com.example.telebridge_android_app.utils.readSms
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import java.util.*
import kotlin.random.Random

class MainActivity : ComponentActivity() {

    private val firebaseDatabase by lazy {
        FirebaseDatabase.getInstance("https://telebridge-fc798-default-rtdb.firebaseio.com/")
    }

    // Stocker le code généré
    private val userCodeState: MutableState<String?> = mutableStateOf(null)

    private val permissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        permissions.forEach { (perm, granted) ->
            Log.d("Permissions", "$perm granted=$granted")
        }
        startAutoSync()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestPermissions()

        // ⚡ demander l'accès aux notifications (doit être activé manuellement)
        requestNotificationAccess()
        val savedCode = LocalStorageHelper.getUserCode(this)
        if (savedCode != null) {
            userCodeState.value = savedCode
            NotificationListener.userCode = savedCode
        }

        setContent {
            TelebridgeandroidappTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    DecorativeCodeScreenWithNav(this, firebaseDatabase, userCodeState)
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
            startAutoSync()
        }
    }

    // 🔔 Ouvre la page d’accès aux notifications
    private fun requestNotificationAccess() {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        startActivity(intent)
    }

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
                    userCodeState.value?.let { code -> syncSms(code) }
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
                    userCodeState.value?.let { code -> syncContacts(code) }
                }
            }
        )

        // Observer Appels
        resolver.registerContentObserver(
            CallLog.Calls.CONTENT_URI,
            true,
            object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) {
                    super.onChange(selfChange)
                    Log.d("Sync", "📞 Nouvel appel détecté")
                    userCodeState.value?.let { code -> syncCalls(code) }
                }
            }
        )
    }

    private fun syncSms(userCode: String) {
        val smsList = readSms(this)
        val batchData = hashMapOf<String, Any>()
        smsList.forEach { sms ->
            val id = UUID.randomUUID().toString()
            batchData["/users/$userCode/sms/$id"] = sms + mapOf("timestamp" to ServerValue.TIMESTAMP)
        }
        firebaseDatabase.reference.updateChildren(batchData)
    }

    private fun syncContacts(userCode: String) {
        val contactsList = readContacts(this)
        val batchData = hashMapOf<String, Any>()
        contactsList.forEach { contact ->
            val id = UUID.randomUUID().toString()
            batchData["/users/$userCode/contacts/$id"] =
                contact + mapOf("timestamp" to ServerValue.TIMESTAMP)
        }
        firebaseDatabase.reference.updateChildren(batchData)
    }

    private fun syncCalls(userCode: String) {
        val callsList = readCallLogs(this)
        val batchData = hashMapOf<String, Any>()
        callsList.forEach { call ->
            val id = UUID.randomUUID().toString()
            batchData["/users/$userCode/calls/$id"] = call + mapOf("timestamp" to ServerValue.TIMESTAMP)
        }
        firebaseDatabase.reference.updateChildren(batchData)
    }
}

@Composable
fun DecorativeCodeScreenWithNav(
    activity: ComponentActivity,
    database: FirebaseDatabase,
    userCodeState: MutableState<String?>
) {
    val scrollState = rememberScrollState()
    val codeState = userCodeState
    val qrMatrix = remember { Array(25) { BooleanArray(25) { Random.nextBoolean() } } }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = true,
                    onClick = { },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { },
                    icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                    label = { Text("Profile") }
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Text("Telebridge - Dashboard", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(24.dp))

            Canvas(
                modifier = Modifier
                    .size(250.dp)
                    .background(Color.White)
            ) {
                val cellSize = size.width / qrMatrix.size
                for (i in qrMatrix.indices) {
                    for (j in qrMatrix[i].indices) {
                        drawRect(
                            color = if (qrMatrix[i][j]) Color.Black else Color.White,
                            topLeft = Offset(i * cellSize, j * cellSize),
                            size = androidx.compose.ui.geometry.Size(cellSize, cellSize)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Code : ${codeState.value?.chunked(3)?.joinToString(" ") ?: ""}",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (codeState.value != null) {
                        // ✅ Session déjà active → inutile de régénérer
                        Log.d("Session", "Code déjà présent : ${codeState.value}, pas de nouvelle exportation complète")
                    } else {
                        // 🚀 Pas de session → créer un nouveau code et l’enregistrer en local
                        val generatedCode = (100_000_000..999_999_999).random().toString()
                        codeState.value = generatedCode
                        LocalStorageHelper.saveUserCode(activity, generatedCode)
                        NotificationListener.userCode = generatedCode

                        // Exporter toutes les données existantes
                        val smsList = readSms(activity)
                        val contactsList = readContacts(activity)
                        val callsList = readCallLogs(activity)
                        val batchData = hashMapOf<String, Any>()

                        smsList.forEach { sms ->
                            val id = UUID.randomUUID().toString()
                            batchData["/users/$generatedCode/sms/$id"] =
                                sms + mapOf("timestamp" to ServerValue.TIMESTAMP)
                        }
                        contactsList.forEach { contact ->
                            val id = UUID.randomUUID().toString()
                            batchData["/users/$generatedCode/contacts/$id"] =
                                contact + mapOf("timestamp" to ServerValue.TIMESTAMP)
                        }
                        callsList.forEach { call ->
                            val id = UUID.randomUUID().toString()
                            batchData["/users/$generatedCode/calls/$id"] =
                                call + mapOf("timestamp" to ServerValue.TIMESTAMP)
                        }
                        database.reference.updateChildren(batchData)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(50.dp)
            ) {
                Text("📤 Exporter toutes les données")
            }

        }
    }
}
