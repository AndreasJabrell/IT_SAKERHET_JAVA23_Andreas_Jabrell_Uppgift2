package se.gritacademy.timecapsule10

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket

class MainActivity : AppCompatActivity() {
    private val PORT = 1234
    private val HOST = "10.0.2.2" // Used to connect to computer from android emulator
    private val PREFS_NAME = "MyAppPrefs"
    private val TOKEN_KEY = "jwt_token"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        //first page for login

        //setting all textfields
        val txtUser: EditText = findViewById(R.id.editTextText)
        val txtPassword: EditText = findViewById(R.id.editTextTextPassword)
        val btnLogIn: Button = findViewById(R.id.button)
        val txtCreate: TextView = findViewById(R.id.textView)

        //click to second page(create user)
        txtCreate.setOnClickListener {
            val intent2 = Intent(this, MainActivity3::class.java)
            startActivity(intent2)
        }

        btnLogIn.setOnClickListener {
            val email = txtUser.text.toString()
            val password = txtPassword.text.toString()
            val intent = Intent(this, MainActivity2::class.java)

            GlobalScope.launch(Dispatchers.IO) {
                try {
                    val socket = Socket(HOST, PORT)
                    val `in` = BufferedReader(InputStreamReader(socket.getInputStream()))
                    val out = PrintWriter(socket.getOutputStream(), true)

                    // Skicka login-kommando
                    out.println("login")
                    out.println(email)
                    out.println(password)

                    // Läs svar från servern
                    val response = `in`.readLine()
                    withContext(Dispatchers.Main) {
                        if (response.startsWith("Success")) {
                            val parts = response.split(" ")
                            val token = parts[1] // JWT-token
                            val aesKey = parts[2] // AES-nyckeln
                            Log.i("AESKEY", "easkey hämtas tillbaka från database $aesKey")

                            // Spara token och email till SharedPreferences
                            saveToken(token)
                            saveEmail(email)
                            Log.i("AESKEY", "easkey sparas i sharedPref Innan $aesKey")
                            saveAESKey(aesKey) // Spara AES-nyckeln i SharedPreferences
                            Log.i("AESKEY", "easkey sparas efter $aesKey")
                            startActivity(intent) // Byt till nästa sida
                            Log.i("Andreas", "JWT mottaget: $token för $email")
                            Toast.makeText(this@MainActivity, "Welcome $email", Toast.LENGTH_SHORT)
                                .show()
                        } else {
                            Log.e("Andreas", "Inloggning misslyckades: $response")
                        }
                    }
                    socket.close()
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Log.e("Andreas", "Nätverksfel", e)
                    }
                }
            }
        }

    }

    //function for saving token to sharedPrefs
    private fun saveToken(token: String) {
        val sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sharedPreferences.edit().putString(TOKEN_KEY, token).apply()
    }

    //function for saving email to sharedPrefs
    private fun saveEmail(email: String) {
        val sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sharedPreferences.edit().putString("user_email", email).apply()
        Log.i("Andreas", "Saving email: $email")
    }

    //save AESKey to sharedPrefs, a little bit of help from ChatGPT to figure this out
    private fun saveAESKey(encodedKey: String) {
        val sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        Log.i("AESKEY", "easkey innan editor $encodedKey")
        // Spara den Base64-kodade nyckeln i SharedPreferences
        editor.putString("AES_KEY", encodedKey)
        editor.apply()

        Log.i("AESKEY", "AES-nyckel sparad: $encodedKey")
    }
}