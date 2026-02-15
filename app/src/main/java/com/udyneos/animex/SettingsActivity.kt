package com.udyneos.animex

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.udyneos.animex.network.NetworkService
import com.udyneos.animex.utils.CacheManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SettingsActivity : AppCompatActivity() {

    private lateinit var tvCacheSize: TextView
    private lateinit var tvCacheDate: TextView
    private lateinit var tvVersionInfo: TextView
    private lateinit var btnClearCache: MaterialButton
    private lateinit var btnUpdateData: MaterialButton
    private lateinit var progressIndicator: LinearProgressIndicator
    private lateinit var cardCacheInfo: CardView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // Initialize views
        tvCacheSize = findViewById(R.id.tvCacheSize)
        tvCacheDate = findViewById(R.id.tvCacheDate)
        tvVersionInfo = findViewById(R.id.tvVersionInfo)
        btnClearCache = findViewById(R.id.btnClearCache)
        btnUpdateData = findViewById(R.id.btnUpdateData)
        progressIndicator = findViewById(R.id.progressIndicator)
        cardCacheInfo = findViewById(R.id.cardCacheInfo)

        // Setup toolbar
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Settings"
        toolbar.setNavigationOnClickListener { finish() }

        // Set version info
        val versionName = packageManager.getPackageInfo(packageName, 0).versionName
        tvVersionInfo.text = "AnimeX v$versionName"

        // Load cache info
        loadCacheInfo()

        // Setup buttons
        setupButtons()
    }

    private fun loadCacheInfo() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val cacheInfo = CacheManager.getCacheInfo(this@SettingsActivity)
                
                withContext(Dispatchers.Main) {
                    tvCacheSize.text = cacheInfo.first
                    
                    val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID"))
                    tvCacheDate.text = if (cacheInfo.second > 0) {
                        "Last update: ${dateFormat.format(Date(cacheInfo.second))}"
                    } else {
                        "No cache data"
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun setupButtons() {
        btnClearCache.setOnClickListener {
            showConfirmDialog(
                "Clear Cache",
                "Are you sure you want to clear all cached data? This will free up storage space but may slow down initial loading.",
                {
                    clearCache()
                }
            )
        }

        btnUpdateData.setOnClickListener {
            showConfirmDialog(
                "Update Data",
                "Download latest anime data from GitHub? This will refresh all cached data.",
                {
                    updateData()
                }
            )
        }
    }

    private fun clearCache() {
        showLoading(true)
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                CacheManager.clearAllCache(this@SettingsActivity)
                
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    loadCacheInfo()
                    Toast.makeText(this@SettingsActivity, "Cache cleared successfully", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    Toast.makeText(this@SettingsActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun updateData() {
        showLoading(true)
        btnUpdateData.text = "Updating..."
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Force refresh dari GitHub
                val animeList = NetworkService.fetchAnimeList(forceRefresh = true)
                
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    btnUpdateData.text = "Update Data"
                    loadCacheInfo()
                    Toast.makeText(this@SettingsActivity, "Updated ${animeList.size} anime from GitHub", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    btnUpdateData.text = "Update Data"
                    Toast.makeText(this@SettingsActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showConfirmDialog(title: String, message: String, onConfirm: () -> Unit) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Yes") { _, _ -> onConfirm() }
            .setNegativeButton("No", null)
            .show()
    }

    private fun showLoading(show: Boolean) {
        if (show) {
            progressIndicator.visibility = android.view.View.VISIBLE
            btnClearCache.isEnabled = false
            btnUpdateData.isEnabled = false
        } else {
            progressIndicator.visibility = android.view.View.GONE
            btnClearCache.isEnabled = true
            btnUpdateData.isEnabled = true
        }
    }
}
