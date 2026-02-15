package com.udyneos.animex

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.progressindicator.LinearProgressIndicator

class SplashActivity : AppCompatActivity() {
    
    private lateinit var progressIndicator: LinearProgressIndicator
    private lateinit var tvLoading: TextView
    private val handler = Handler(Looper.getMainLooper())
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        
        progressIndicator = findViewById(R.id.progressIndicator)
        tvLoading = findViewById(R.id.tvLoading)
        
        // Set progress max
        progressIndicator.max = 100
        progressIndicator.progress = 0
        
        // Simulasi loading
        simulateLoading()
    }
    
    private fun simulateLoading() {
        Thread {
            val loadingMessages = arrayOf(
                "Menyiapkan aplikasi...",
                "Memuat data anime...",
                "Hampir selesai..."
            )
            
            for (i in 0..100) {
                val progressStatus = i
                
                // Update progress dan teks
                handler.post {
                    progressIndicator.progress = progressStatus
                    when {
                        progressStatus < 30 -> tvLoading.text = loadingMessages[0]
                        progressStatus < 70 -> tvLoading.text = loadingMessages[1]
                        else -> tvLoading.text = loadingMessages[2]
                    }
                }
                
                try {
                    Thread.sleep(30) // Delay 30ms untuk total ~3 detik
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            }
            
            // Pindah ke MainActivity setelah loading selesai
            handler.post {
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
            }
        }.start()
    }
}
