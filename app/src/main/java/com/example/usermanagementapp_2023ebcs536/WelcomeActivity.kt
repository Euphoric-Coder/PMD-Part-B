package com.example.usermanagementapp_2023ebcs536

import android.content.*
import android.net.ConnectivityManager
import android.os.Build
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class WelcomeActivity : AppCompatActivity() {

    // Firebase and shared preferences declarations
    private lateinit var auth: FirebaseAuth
    private lateinit var dbRef: DatabaseReference
    private lateinit var sharedPreferences: SharedPreferences

    // UI elements value storer declaration
    private lateinit var greetingText: TextView
    private lateinit var logoutBtn: Button
    private lateinit var userDetailsListView: ListView
    private lateinit var adapter: ArrayAdapter<String>
    private val userList = mutableListOf<String>()

    // BroadcastReceiver to receive dynamic user data updates from UserFetchService
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val data = intent?.getStringArrayListExtra("userList")
            if (data != null) {
                userList.clear()
                userList.addAll(data)
                adapter.notifyDataSetChanged()
                Toast.makeText(this@WelcomeActivity, "User data loaded", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this@WelcomeActivity, "No data received", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)

        // Initialize Firebase and SharedPreferences
        auth = FirebaseAuth.getInstance()
        dbRef = FirebaseDatabase.getInstance().getReference("Users")
        sharedPreferences = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)

        // UI elements from xml file
        greetingText = findViewById(R.id.greetingText)
        logoutBtn = findViewById(R.id.logoutButton)
        userDetailsListView = findViewById(R.id.userDetailsListView)

        // Setup ListView adapter
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, userList)
        userDetailsListView.adapter = adapter

        // Retrieve user information from SharedPreferences
        val email = sharedPreferences.getString("email", "N/A")
        val role = sharedPreferences.getString("role", "normal")
        val uid = sharedPreferences.getString("uid", "") ?: ""

        if(role == "admin") {
            greetingText.text = "Welcome Admin:\n$email"
        } else {
            greetingText.text = "Welcome:\n$email"
        }


        // Register BroadcastReceiver before starting service
        val filter = IntentFilter("USER_DATA_BROADCAST")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(receiver, filter, RECEIVER_EXPORTED)
        } else {
            registerReceiver(receiver, filter, RECEIVER_NOT_EXPORTED)
        }

        // Checks if network is available or not
        if (!isNetworkAvailable()) {
            Toast.makeText(this, "No Internet Connection", Toast.LENGTH_LONG).show()
        } else {
            // Admins get real-time updates via service and normal users fetch their own data only one time
            if (role == "admin") {
                startService(Intent(this, UserFetchService::class.java))
            } else {
                fetchUserDetails(uid)
            }
        }

        // Handle the logout logic and redirects to the LoginActivity Page
        logoutBtn.setOnClickListener {
            auth.signOut()
            sharedPreferences.edit().clear().apply()
            unregisterReceiver(receiver)
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    // Function to fetch details of the currently logged-in normal user
    private fun fetchUserDetails(uid: String) {
        dbRef.child(uid).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                userList.clear()
                val email = snapshot.child("email").value ?: "N/A"
                val role = snapshot.child("role").value ?: "N/A"
                userList.add("Email: $email\nRole: $role")
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@WelcomeActivity, "Error fetching data", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // Function to check if there is internet connection
    private fun isNetworkAvailable(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return cm.activeNetworkInfo?.isConnected == true
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(receiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
