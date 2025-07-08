package com.shaikhaklakh.stockprice

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@SuppressLint("CustomSplashScreen")
class SplashActivity : ComponentActivity() {
    private lateinit var firebaseAuth: FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        firebaseAuth = FirebaseAuth.getInstance()

        splashScreen.setKeepOnScreenCondition {
            true
        }

        lifecycleScope.launch {
            delay(1000)
            val currentUser = firebaseAuth.currentUser
            if (currentUser != null)
            {
                startActivity(Intent(this@SplashActivity, HomeActivity::class.java))
                finish()
            }
            else{
                startActivity(Intent(this@SplashActivity, IntroActivity::class.java))
                finish()
            }

        }

    }
}