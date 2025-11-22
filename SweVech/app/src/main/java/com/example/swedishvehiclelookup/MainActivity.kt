package com.example.swedishvehiclelookup

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.speech.RecognizerIntent
import android.telephony.SmsManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
    
    private lateinit var speechRecognizerLauncher: ActivityResultLauncher<Intent>
    private var localSmsReceiver: BroadcastReceiver? = null
    
    // State variables - using mutableStateOf for Compose
    private val _registrationNumber = mutableStateOf("")
    private val _responseText = mutableStateOf("")
    private val _isLoading = mutableStateOf(false)
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Setup speech recognizer launcher
        speechRecognizerLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                val matches = result.data?.getStringArrayListExtra(
                    RecognizerIntent.EXTRA_RESULTS
                )
                if (!matches.isNullOrEmpty()) {
                    val spokenText = matches[0]
                    _registrationNumber.value = cleanRegistrationNumber(spokenText)
                }
            }
        }
        
        // Request necessary permissions
        requestPermissions()
        
        // Setup local SMS receiver for this activity
        setupLocalSmsReceiver()
        
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    VehicleLookupScreen()
                }
            }
        }
    }
    
    @Composable
    fun VehicleLookupScreen() {
        val registrationNumber by remember { _registrationNumber }
        val responseText by remember { _responseText }
        val isLoading by remember { _isLoading }
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            
            // App Title
            Text(
                text = "Swedish Vehicle Lookup",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Service info
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SMS Service: ",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "71640",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Registration number input
            OutlinedTextField(
                value = registrationNumber,
                onValueChange = { 
                    _registrationNumber.value = it.uppercase().filter { char ->
                        char.isLetterOrDigit() || char.isWhitespace()
                    }
                },
                label = { Text("Registration Number") },
                placeholder = { Text("e.g., ABC123") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                trailingIcon = {
                    Row {
                        if (registrationNumber.isNotEmpty()) {
                            IconButton(onClick = { 
                                _registrationNumber.value = ""
                                _responseText.value = ""
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear"
                                )
                            }
                        }
                        IconButton(onClick = { startVoiceRecognition() }) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Voice Input",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Hint text
            Text(
                text = "💬 Tap microphone to speak the registration number",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Send SMS button
            Button(
                onClick = { sendVehicleLookupSms() },
                enabled = registrationNumber.isNotBlank() && !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                if (isLoading) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Waiting for response...")
                    }
                } else {
                    Text(
                        "🔍 Lookup Vehicle Information",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Cost warning
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = "⚠️ Standard SMS rates apply. Response typically arrives within 30 seconds.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(12.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Response display
            if (responseText.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "📋 Vehicle Information",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            if (!isLoading) {
                                Text(
                                    text = "✓",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Divider()
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Text(
                            text = responseText,
                            style = MaterialTheme.typography.bodyMedium,
                            lineHeight = MaterialTheme.typography.bodyMedium.lineHeight.times(1.5f)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
    
    private fun startVoiceRecognition() {
        if (!checkPermission(Manifest.permission.RECORD_AUDIO)) {
            Toast.makeText(
                this,
                "Microphone permission required",
                Toast.LENGTH_SHORT
            ).show()
            requestPermissions()
            return
        }
        
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "sv-SE")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Säg registreringsnumret")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        
        try {
            speechRecognizerLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "Voice recognition not available on this device",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
    
    private fun cleanRegistrationNumber(input: String): String {
        // Remove spaces and keep only alphanumeric characters
        return input.replace(Regex("[^A-Za-z0-9]"), "").uppercase()
    }
    
    private fun sendVehicleLookupSms() {
        val regNumber = _registrationNumber.value.trim()
        
        if (!checkPermission(Manifest.permission.SEND_SMS)) {
            Toast.makeText(
                this,
                "SMS permission required",
                Toast.LENGTH_SHORT
            ).show()
            requestPermissions()
            return
        }
        
        if (regNumber.isBlank()) {
            Toast.makeText(this, "Please enter a registration number", Toast.LENGTH_SHORT).show()
            return
        }
        
        try {
            val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }
            
            smsManager.sendTextMessage(
                "71640",
                null,
                regNumber,
                null,
                null
            )
            
            _isLoading.value = true
            _responseText.value = "📤 SMS sent to 71640\n⏳ Waiting for response..."
            
            Toast.makeText(
                this,
                "SMS sent successfully!",
                Toast.LENGTH_SHORT
            ).show()
            
            // Set timeout for response
            android.os.Handler(mainLooper).postDelayed({
                if (_isLoading.value) {
                    _isLoading.value = false
                    if (_responseText.value.contains("Waiting for response")) {
                        _responseText.value = "⏱️ No response received yet.\n\nPlease check your SMS inbox manually.\nThe response should arrive from 71640."
                    }
                }
            }, 45000) // 45 second timeout
            
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "Failed to send SMS: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
            _isLoading.value = false
            _responseText.value = "❌ Error: ${e.message}"
        }
    }
    
    private fun setupLocalSmsReceiver() {
        localSmsReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == SmsReceiver.SMS_RECEIVED_ACTION) {
                    val messageBody = intent.getStringExtra(SmsReceiver.EXTRA_MESSAGE_BODY)
                    val sender = intent.getStringExtra(SmsReceiver.EXTRA_SENDER)
                    
                    if (messageBody != null) {
                        _isLoading.value = false
                        _responseText.value = messageBody
                        
                        runOnUiThread {
                            Toast.makeText(
                                this@MainActivity,
                                "✓ Vehicle information received!",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            }
        }
        
        // Register receiver for custom broadcast from SmsReceiver
        val filter = IntentFilter(SmsReceiver.SMS_RECEIVED_ACTION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(localSmsReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            ContextCompat.registerReceiver(this, localSmsReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        }
    }
    
    private fun checkPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    private fun requestPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.SEND_SMS,
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.RECORD_AUDIO
        )
        
        // READ_SMS might not be strictly necessary for receiving
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.READ_SMS)
        }
        
        val permissionsToRequest = permissions.filter { !checkPermission(it) }
        
        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toTypedArray(),
                PERMISSION_REQUEST_CODE
            )
        }
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        if (requestCode == PERMISSION_REQUEST_CODE) {
            val allGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            if (allGranted) {
                Toast.makeText(this, "✓ All permissions granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(
                    this,
                    "⚠️ Some permissions denied. App may not work correctly.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        try {
            localSmsReceiver?.let { unregisterReceiver(it) }
        } catch (e: Exception) {
            // Receiver was not registered or already unregistered
        }
    }
    
    companion object {
        private const val PERMISSION_REQUEST_CODE = 100
    }
}
