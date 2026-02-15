package com.udyneos.animex

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.udyneos.animex.network.NetworkService
import com.udyneos.animex.utils.CacheManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SplashActivity : AppCompatActivity() {
    
    private lateinit var progressIndicator: LinearProgressIndicator
    private lateinit var tvLoading: TextView
    private lateinit var tvStatus: TextView
    private val handler = Handler(Looper.getMainLooper())
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        
        progressIndicator = findViewById(R.id.progressIndicator)
        tvLoading = findViewById(R.id.tvLoading)
        tvStatus = findViewById(R.id.tvStatus)
        
        // Set progress max
        progressIndicator.max = 100
        progressIndicator.progress = 0
        
        // Inisialisasi NetworkService
        NetworkService.init(this)
        
        // Mulai loading data
        loadData()
    }
    
    private fun loadData() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Step 1: Cek cache
                updateProgress(10, "Mengecek cache...")
                val cachedData = CacheManager.loadAnimeList(this@SplashActivity)
                
                if (cachedData != null) {
                    // Gunakan cache
                    updateProgress(50, "Memuat data dari cache...")
                    Thread.sleep(500) // Simulasi loading
                    updateProgress(80, "Menyiapkan aplikasi...")
                    Thread.sleep(500)
                } else {
                    // Download dari GitHub
                    updateProgress(20, "Mendownload data dari GitHub...")
                    val animeList = NetworkService.fetchAnimeList(forceRefresh = true)
                    
                    updateProgress(60, "Menyimpan ke cache...")
                    Thread.sleep(300)
                    
                    updateProgress(80, "Memproses ${animeList.size} anime...")
                    Thread.sleep(500)
                }
                
                // Selesai
                updateProgress(100, "Selesai! Memulai aplikasi...")
                Thread.sleep(500)
                
                // Pindah ke MainActivity
                withContext(Dispatchers.Main) {
                    val intent = Intent(this@SplashActivity, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                }
                
            } catch (e: Exception) {
                e.printStackTrace()
                updateProgress(100, "Error: ${e.message}")
                Thread.sleep(1000)
                
                withContext(Dispatchers.Main) {
                    val intent = Intent(this@SplashActivity, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                }
            }
        }
    }
    
    private fun updateProgress(progress: Int, message: String) {
        handler.post {
            progressIndicator.progress = progress
            tvLoading.text = message
            
            when {
                progress < 30 -> tvStatus.text = "Menghubungi server..."
                progress < 60 -> tvStatus.text = "Mengunduh data..."
                progress < 90 -> tvStatus.text = "Memproses..."
                else -> tvStatus.text = "Hampir selesai..."
            }
        }
    }
}
