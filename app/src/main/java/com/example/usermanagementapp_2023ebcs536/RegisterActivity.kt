package com.example.usermanagementapp_2023ebcs536

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class RegisterActivity : AppCompatActivity() {

    // Declare UI elements
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var userTypeSpinner: Spinner
    private lateinit var registerButton: Button
    private lateinit var loginLink: TextView

    // Firebase Instances
    private lateinit var auth: FirebaseAuth
    private lateinit var dbRef: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // Initialize UI elements
        emailEditText = findViewById(R.id.email_edittext)
        passwordEditText = findViewById(R.id.password_edittext)
        userTypeSpinner = findViewById(R.id.spinnerUserType)
        registerButton = findViewById(R.id.register_button)
        loginLink = findViewById(R.id.tvLoginLink)

        // Initialize Firebase Authentication and Realtime Database
        auth = FirebaseAuth.getInstance()
        dbRef = FirebaseDatabase.getInstance().reference

        // Add dropdown with user roles
        val roles = arrayOf("admin", "normal")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, roles)
        userTypeSpinner.adapter = adapter

        // Register button with click listener
        registerButton.setOnClickListener {
            if (isNetworkAvailable()) {
                registerUser() // Move forward if internet is available
            } else {
                Toast.makeText(this, "No Internet Connection", Toast.LENGTH_SHORT).show()
            }
        }

        // Navigate to login screen
        loginLink.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    // Function to register new user
    private fun registerUser() {
        val email = emailEditText.text.toString().trim()
        val password = passwordEditText.text.toString().trim()
        val role = userTypeSpinner.selectedItem.toString()

        // Checks if either of the input fields is empty or not
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter both email and password", Toast.LENGTH_SHORT).show()
            return
        }

        // Create User on the Firebase with email and password
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                val uid = it.user?.uid
                // For checking null uid
                if (uid == null) {
                    Toast.makeText(this, "UID is null", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                // User data that is needed to store in database
                val userData = mapOf(
                    "email" to email,
                    "role" to role
                )

                // Reference to user node in Firebase
                val userNode = dbRef.child("Users").child(uid)
                // Write user data to Firebase
                userNode.setValue(userData)
                    .addOnSuccessListener {
                        Toast.makeText(this, "User registered successfully", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, LoginActivity::class.java))
                        finish()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "DB write failed", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Registration failed", Toast.LENGTH_SHORT).show()
            }
    }
    // Function to check if network is available
    private fun isNetworkAvailable(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return cm.activeNetworkInfo?.isConnected == true
    }
}
