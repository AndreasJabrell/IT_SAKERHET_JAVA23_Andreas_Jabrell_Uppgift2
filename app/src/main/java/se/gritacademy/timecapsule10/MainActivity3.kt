package se.gritacademy.timecapsule10

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

class MainActivity3 : AppCompatActivity() {
    private val PORT = 1234
    private val HOST = "10.0.2.2" // Används för att ansluta till server via android emulator
    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main4)

        val txtName: EditText = findViewById(R.id.editTextText3)
        val txtEmail: EditText = findViewById(R.id.editTextText4)
        val txtPassword: EditText = findViewById(R.id.editTextTextPassword2)
        val buttonCreate: Button = findViewById(R.id.button4)

        //click button, all info written sent to database to create user
        buttonCreate.setOnClickListener {
            val name = txtName.text.toString()
            val password = txtPassword.text.toString()
            val email = txtEmail.text.toString()
            val intent = Intent(this, MainActivity::class.java)

            //GENERATE AESkey HÄR
            val aesKey = generateAESKey()
            Log.i("AESKEY", "easkey skapas $aesKey")

            GlobalScope.launch(Dispatchers.IO) {
                try {
                    val socket = Socket(HOST, PORT)
                    val `in` = BufferedReader(InputStreamReader(socket.getInputStream()))
                    val out = PrintWriter(socket.getOutputStream(), true)

                    // Skicka "register" först
                    out.println("register")

                    // Skicka användarnamn, lösenord och email
                    out.println(name)
                    out.println(password)
                    out.println(email)
                    out.println(aesKey)
                    //massa extra loggar för att lista ut vad som händer med aesKey
                    Log.i("AESKEY", "easkey skickas till databasen $aesKey")
                    // Nu läs svaret från servern
                    val response = `in`.readLine()
                    Log.i("Andreas", "Name: $name password:$password email$email AES-key$aesKey $response")
                    withContext(Dispatchers.Main) {
                        if (response.startsWith("Success")) {
                            Log.i("Andreas", "Registrering lyckades: $response")
                            startActivity(intent)
                            Toast.makeText(this@MainActivity3, "Welcome $name, you can now log in", Toast.LENGTH_SHORT).show()
                        } else {
                            Log.e("Andreas", "Registrering misslyckades: $response")
                            Toast.makeText(this@MainActivity3, "Registrering misslyckades: $response", Toast.LENGTH_SHORT).show()
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

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    fun generateAESKey(): String {
        val keyGen = KeyGenerator.getInstance("AES")
        keyGen.init(128) // Kan ändras till 192 eller 256 för att stödja längre nycklar
        val aesKey: SecretKey = keyGen.generateKey()
        return android.util.Base64.encodeToString(aesKey.encoded, android.util.Base64.DEFAULT) // Returnera Base64-sträng
    }
}