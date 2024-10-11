package se.gritacademy.timecapsule10

import android.content.Context
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class BlankFragment2 : Fragment() {
    private val PREFS_NAME = "MyAppPrefs"
    private val TOKEN_KEY = "jwt_token"
    private val PORT = 1234
    private val HOST = "10.0.2.2"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val view: View = inflater.inflate(R.layout.fragment_blank2, container, false)
        val token = getToken()
        val email = getEmail()
        val aesKey = getAESKey()
        Log.i("AESKEY", "AES key från variabel $aesKey")
        Log.i("AESKEY", "AES key hämtas vid hämtande av message: ${Base64.encodeToString(aesKey?.encoded, Base64.DEFAULT)}")

        val txtDisplay: TextView = view.findViewById(R.id.textView3)

        if (token != null && email != null) {
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val socket = Socket(HOST, PORT)
                    val `in` = BufferedReader(InputStreamReader(socket.getInputStream()))
                    val out = PrintWriter(socket.getOutputStream(), true)
                    out.println("displayTimecapsule")
                    Log.i("Andreas", "AES key vid hämtande av message2 = $aesKey")
                    out.println("Bearer $token")
                    out.println(email)

                    val response = `in`.readLine()
                    withContext(Dispatchers.Main) {
                        if (response.startsWith("Success")) {
                            val messages = response.substringAfter("Success ").split(";").filter { it.isNotEmpty() }

                            val decryptedMessages = messages.mapNotNull { message ->
                                try {
                                    AESDekryptering(message, aesKey)
                                } catch (e: Exception) {
                                    Log.e("Andreas", "Dekryptering misslyckades för meddelande: $message", e)
                                    null
                                }
                            }

                            val displayText = decryptedMessages.joinToString("\n")
                            txtDisplay.text = displayText
                            Log.i("Andreas", "Dekrypterade timecapsules: $displayText")
                        } else {
                            Log.e("Andreas", "Hämtande av timecapsule misslyckades! $response")
                        }
                    }
                    socket.close()
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Log.e("Andreas", "Nätverksfel", e)
                    }
                }
            }
        } else {
            Log.e("Andreas", "Token eller email är null. Kontrollera om de är sparade i SharedPreferences.")
        }

        return view
    }

    private fun getToken(): String? {
        val sharedPreferences = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val token = sharedPreferences.getString(TOKEN_KEY, null)
        Log.i("Andreas", "Fetched token: $token")
        return token
    }

    fun getEmail(): String? {
        val sharedPreferences = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val email = sharedPreferences.getString("user_email", null)
        Log.i("Andreas", "Fetched email: $email")
        return email
    }

    private fun getAESKey(): SecretKeySpec? {
        val sharedPreferences = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val encodedKey = sharedPreferences.getString("AES_KEY", null)

        if (encodedKey == null) {
            Log.e("AESKEY", "Ingen AES-nyckel hittades i SharedPreferences")
            return null
        }

        Log.i("AESKEY", "Hämtad AES-nyckel från SharedPreferences: $encodedKey")

        val decodedKey = Base64.decode(encodedKey, Base64.DEFAULT)
        Log.i("AESKEY", "Dekodad nyckel: ${decodedKey.joinToString(", ") { it.toString() }}")

        return if (decodedKey.size == 16 || decodedKey.size == 24 || decodedKey.size == 32) {
            SecretKeySpec(decodedKey, "AES")
        } else {
            Log.e("AESKEY", "Ogiltig AES-nyckellängd: ${decodedKey.size} bytes")
            null
        }
    }

    // AES dekrypteringsmetod
    @Throws(Exception::class)
    fun AESDekryptering(encryptedData: String?, secretKey: SecretKey?): String? {
        val parts = encryptedData!!.split(":")
        if (parts.size != 2) {
            Log.e("AESDekryptering", "Ogiltigt format på krypterat meddelande: $encryptedData")
            throw IllegalArgumentException("Ogiltigt krypterat datformat.")
        }

        val iv = Base64.decode(parts[0], Base64.DEFAULT)
        val encryptedBytes = Base64.decode(parts[1], Base64.DEFAULT)

        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        val ivParameterSpec = IvParameterSpec(iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, ivParameterSpec)

        val decryptedBytes = cipher.doFinal(encryptedBytes)
        return String(decryptedBytes)
    }

}
