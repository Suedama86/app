package com.example.swedishvehiclelookup

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsMessage
import android.util.Log

class SmsReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "SmsReceiver"
        const val SMS_RECEIVED_ACTION = "com.example.swedishvehiclelookup.SMS_RECEIVED"
        const val EXTRA_MESSAGE_BODY = "message_body"
        const val EXTRA_SENDER = "sender"
    }
    
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == "android.provider.Telephony.SMS_RECEIVED") {
            val bundle = intent.extras
            if (bundle != null) {
                try {
                    val pdus = bundle.get("pdus") as Array<*>
                    val messages = arrayOfNulls<SmsMessage>(pdus.size)
                    
                    for (i in pdus.indices) {
                        messages[i] = SmsMessage.createFromPdu(pdus[i] as ByteArray)
                    }
                    
                    // Combine message parts if needed
                    val messageBody = StringBuilder()
                    var sender = ""
                    
                    for (message in messages) {
                        message?.let {
                            sender = it.originatingAddress ?: ""
                            messageBody.append(it.messageBody)
                        }
                    }
                    
                    Log.d(TAG, "SMS received from: $sender")
                    Log.d(TAG, "Message body: $messageBody")
                    
                    // Check if message is from 71640
                    if (sender.contains("71640") || sender.contains("71640")) {
                        Log.d(TAG, "Message from vehicle lookup service!")
                        
                        // Broadcast to MainActivity
                        val broadcastIntent = Intent(SMS_RECEIVED_ACTION).apply {
                            putExtra(EXTRA_MESSAGE_BODY, messageBody.toString())
                            putExtra(EXTRA_SENDER, sender)
                            setPackage(context?.packageName)
                        }
                        context?.sendBroadcast(broadcastIntent)
                    }
                    
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing SMS", e)
                }
            }
        }
    }
}
