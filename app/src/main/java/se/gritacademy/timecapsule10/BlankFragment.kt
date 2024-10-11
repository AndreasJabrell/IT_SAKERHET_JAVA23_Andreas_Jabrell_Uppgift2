package se.gritacademy.timecapsule10

import android.content.Context
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec


class BlankFragment : Fragment() {
    private val PREFS_NAME = "MyAppPrefs"
    private val TOKEN_KEY = "jwt_token"
    private val PORT = 1234
    private val HOST = "10.0.2.2"
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val view: View = inflater.inflate(R.layout.fragment_blank, container, false)
        val token = getToken()
        val email = getEmail()
        val aesKey = getAESKey()

        Log.i(
            "AESKEY",
            "AES key hämtas vid skapande av message: ${
                Base64.encodeToString(
                    aesKey?.encoded,
                    Base64.DEFAULT
                )
            } "
        )

        val button: Button = view.findViewById(R.id.button3)
        //fetching message/timecapsule from User
        val txtMessage: EditText = view.findViewById(R.id.editTextTextMultiLine)

        button.setOnClickListener {
            val message = txtMessage.text.toString()
            Log.i("Andreas", "message = $message")

            if (aesKey != null) {
                val aesEncrypted: String = AESKryptering(message, aesKey)
                Log.i("Andreas", "Token: $token, Email: $email, Encrypted Message: $aesEncrypted")

                if (token != null && email != null && message.isNotEmpty()) {
                    lifecycleScope.launch(Dispatchers.IO) {
                        try {
                            val socket = Socket(HOST, PORT)
                            val `in` = BufferedReader(InputStreamReader(socket.getInputStream()))
                            val out = PrintWriter(socket.getOutputStream(), true)
                            out.println("registerTimecapsule")
                            out.println("Bearer $token")
                            out.println(email)
                            out.println(aesEncrypted)

                            val response = `in`.readLine()
                            withContext(Dispatchers.Main) {
                                if (response.startsWith("Success")) {
                                    Log.i("Andreas", "Svar från servern: $response")
                                    Toast.makeText(context, "Timecapsule saved", Toast.LENGTH_SHORT)
                                        .show()
                                } else {
                                    Log.e("Andreas", "Sparande av timecapsule misslyckades!" + response)
                                }
                            }
                            txtMessage.text.clear()
                            socket.close()
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                Log.e("Andreas", "Nätverksfel", e)
                                e.printStackTrace()
                            }
                        }
                    }
                }
            } else {
                Log.e("AESKEY", "AES-nyckel är null, kryptering kan inte genomföras.")
            }
        }
        //Toast.makeText(context, "Button clicked!", Toast.LENGTH_SHORT).show()

        return view
    }

    private fun getToken(): String? {
        val sharedPreferences =
            requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sharedPreferences.getString(TOKEN_KEY, null)
    }

    @Throws(java.lang.Exception::class)
    fun AESKryptering(data: String, secretKey: SecretKey?): String {
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        val iv = ByteArray(16)
        SecureRandom().nextBytes(iv) // Generera en ny IV
        val ivParameterSpec = IvParameterSpec(iv)

        cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivParameterSpec)
        val encryptedBytes = cipher.doFinal(data.toByteArray())

        // Returnera IV + Krypterad text
        return Base64.encodeToString(iv, Base64.DEFAULT) + ":" + Base64.encodeToString(
            encryptedBytes,
            Base64.DEFAULT
        )
    }

    //get email from sharedPrefs
    fun getEmail(): String? {
        val sharedPreferences =
            requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val email = sharedPreferences.getString("user_email", null)
        Log.i("Andreas", "Fetched email: $email")
        return email
    }

    //get AESKey from sharedPrefs, a bit of help from ChatGPT here as well
    private fun getAESKey(): SecretKeySpec? {
        val sharedPreferences = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val encodedKey = sharedPreferences.getString("AES_KEY", null)

        if (encodedKey == null) {
            Log.e("AESKEY", "Ingen AES-nyckel hittades i SharedPreferences")
            return null
        }

        Log.i("AESKEY", "Hämtad AES-nyckel från SharedPreferences: $encodedKey")

        // Dekoda Base64-strängen
        val decodedKey = Base64.decode(encodedKey, Base64.DEFAULT)
        Log.i("AESKEY", "Dekodad nyckel: ${decodedKey.joinToString(", ") { it.toString() }}") // Lägg till loggning av den dekodade nyckeln

        // Validera längden på nyckeln
        return if (decodedKey.size == 16 || decodedKey.size == 24 || decodedKey.size == 32) {
            SecretKeySpec(decodedKey, "AES")
        } else {
            Log.e("AESKEY", "Ogiltig AES-nyckellängd: ${decodedKey.size} bytes")
            null
        }
    }
}
