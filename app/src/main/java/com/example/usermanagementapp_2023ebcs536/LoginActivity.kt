package com.example.usermanagementapp_2023ebcs536

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class LoginActivity : AppCompatActivity() {

    // Firebase Auth and Database Refer
    private lateinit var auth: FirebaseAuth
    private lateinit var dbRef: DatabaseReference

    // SharedPreferences to store user login details locally
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        dbRef = FirebaseDatabase.getInstance().reference

        // Access local storage for storing UID, email, role
        sharedPreferences = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)

        // UI elements from XML file
        val emailEditText = findViewById<EditText>(R.id.etLoginUsername)
        val passwordEditText = findViewById<EditText>(R.id.etLoginPassword)
        val loginButton = findViewById<Button>(R.id.btnLogin)
        val registerLink = findViewById<TextView>(R.id.tvRegisterLink)

        // Login button click event
        loginButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            // Check network before processing
            if (!isNetworkAvailable()) {
                Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show()
            } else if (email.isNotEmpty() && password.isNotEmpty()) {
                loginUser(email, password)
            } else {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            }
        }

        // Check network before proceeding
        registerLink.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    // Handles user authentication and role fetch
    private fun loginUser(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                val uid = auth.currentUser?.uid ?: return@addOnSuccessListener

                // Fetch the user's role from Firebase Database
                dbRef.child("Users").child(uid).child("role")
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            val role = snapshot.getValue(String::class.java) ?: "normal"

                            // Save to SharedPreferences for access to the Service and fetch the data accordingly if Admin
                            sharedPreferences.edit()
                                .putString("uid", uid)
                                .putString("email", email)
                                .putString("role", role)
                                .apply()

                            // Navigate to WelcomeActivity
                            startActivity(Intent(this@LoginActivity, WelcomeActivity::class.java))
                            finish()
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Toast.makeText(this@LoginActivity, "Failed to retrieve role", Toast.LENGTH_SHORT).show()
                        }
                    })
            }
            .addOnFailureListener {
                Toast.makeText(this, "Login failed: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // Function to check if the device has active internet connection
    private fun isNetworkAvailable(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return cm.activeNetworkInfo?.isConnected == true
    }
}
