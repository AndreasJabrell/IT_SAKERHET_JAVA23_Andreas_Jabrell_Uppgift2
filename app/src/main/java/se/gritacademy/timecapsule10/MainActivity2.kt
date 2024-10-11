package se.gritacademy.timecapsule10

import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity2 : AppCompatActivity() {
    lateinit var fc: View
    private var showFragment1 = true
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main3)

        //only thing happening here is change between fragments
        //MIGHT ADD A LOGOUT BUTTON

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainerView, BlankFragment())
                .commit()
        }
        val fragmentManager = supportFragmentManager
        fc = findViewById(R.id.fragmentContainerView)
        findViewById<Button>(R.id.button2).setOnClickListener {
            if (showFragment1) {
                // Byt till FragmentB
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainerView, BlankFragment2())
                    .addToBackStack(null)
                    .commit()
            } else {
                // Byt tillbaka till FragmentA
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainerView, BlankFragment())
                    .addToBackStack(null)
                    .commit()
            }
            showFragment1 = !showFragment1 // Change between fragments
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}