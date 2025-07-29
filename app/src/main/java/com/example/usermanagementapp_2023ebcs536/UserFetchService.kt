package com.example.usermanagementapp_2023ebcs536

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.google.firebase.database.*

class UserFetchService : Service() {

    // Firebase Realtime Database reference and listener for dynamic updates
    private lateinit var dbRef: DatabaseReference
    private lateinit var listener: ValueEventListener

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Reference to the "Users" node in Firebase
        dbRef = FirebaseDatabase.getInstance().getReference("Users")

        // Create a ValueEventListener to fetch and listen for user data updates
        listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val userList = ArrayList<String>()

                // Iterate through all user records and extract email and role
                for (child in snapshot.children) {
                    val email = child.child("email").value ?: "N/A"
                    val role = child.child("role").value ?: "N/A"
                    userList.add("Email: $email\nRole: $role")
                }

                // Send the user list data via broadcast intent
                val broadcastIntent = Intent("USER_DATA_BROADCAST")
                broadcastIntent.putStringArrayListExtra("userList", userList)
                sendBroadcast(broadcastIntent)
            }

            override fun onCancelled(error: DatabaseError) {
                // Log data if in case there is any error to fetch data
                Log.e("UserFetchService", "Error: ${error.message}")
            }
        }

        dbRef.addValueEventListener(listener)

        // keep it running unless stopped
        return START_STICKY
    }

    override fun onDestroy() {
        // Removes the listener when the service is destroyed or stopped
        super.onDestroy()
        if (::dbRef.isInitialized && ::listener.isInitialized) {
            dbRef.removeEventListener(listener)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
