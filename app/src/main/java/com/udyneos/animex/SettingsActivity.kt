package com.udyneos.animex

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.udyneos.animex.network.NetworkService
import com.udyneos.animex.utils.SimpleCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SettingsActivity : AppCompatActivity() {

    private lateinit var tvVersionInfo: TextView
    private lateinit var tvCacheInfo: TextView
    private lateinit var tvLastUpdate: TextView
    private lateinit var tvAnimeCount: TextView
    private lateinit var btnUpdateData: MaterialButton
    private lateinit var btnClearCache: MaterialButton
    private lateinit var progressIndicator: LinearProgressIndicator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        tvVersionInfo = findViewById(R.id.tvVersionInfo)
        tvCacheInfo = findViewById(R.id.tvCacheInfo)
        tvLastUpdate = findViewById(R.id.tvLastUpdate)
        tvAnimeCount = findViewById(R.id.tvAnimeCount)
        btnUpdateData = findViewById(R.id.btnUpdateData)
        btnClearCache = findViewById(R.id.btnClearCache)
        progressIndicator = findViewById(R.id.progressIndicator)

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Settings"
        toolbar.setNavigationOnClickListener { finish() }

        val versionName = packageManager.getPackageInfo(packageName, 0).versionName
        tvVersionInfo.text = "AnimeX v$versionName"

        loadCacheInfo()
        setupButtons()
    }

    private fun loadCacheInfo() {
        val hasCache = SimpleCache.hasCache()
        val animeList = SimpleCache.getAnimeList()
        
        tvCacheInfo.text = if (hasCache) "✅ Cache aktif" else "⚠️ Cache kosong"
        tvAnimeCount.text = "Total anime: ${animeList.size}"
        
        val lastUpdate = SimpleCache.getLastUpdateTime()
        if (lastUpdate > 0) {
            val date = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID"))
            tvLastUpdate.text = "Update: ${date.format(Date(lastUpdate))}"
        } else {
            tvLastUpdate.text = "Update: Never"
        }
    }

    private fun setupButtons() {
        btnUpdateData.setOnClickListener {
            showConfirmDialog(
                "Update Data",
                "Download data terbaru dari GitHub?",
                { updateData() }
            )
        }

        btnClearCache.setOnClickListener {
            showConfirmDialog(
                "Hapus Cache",
                "Hapus semua data cache?",
                { clearCache() }
            )
        }
    }

    private fun updateData() {
        showLoading(true)
        btnUpdateData.text = "Updating..."
        
        lifecycleScope.launch {
            val animeList = withContext(Dispatchers.IO) {
                NetworkService.loadAnimeList(forceRefresh = true)
            }
            
            showLoading(false)
            btnUpdateData.text = "Update Data"
            loadCacheInfo()
            Toast.makeText(this@SettingsActivity, "Updated ${animeList.size} anime", Toast.LENGTH_SHORT).show()
        }
    }

    private fun clearCache() {
        showLoading(true)
        btnClearCache.text = "Clearing..."
        
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                SimpleCache.clearCache()
            }
            
            showLoading(false)
            btnClearCache.text = "Clear Cache"
            loadCacheInfo()
            Toast.makeText(this@SettingsActivity, "Cache cleared", Toast.LENGTH_SHORT).show()
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
            btnUpdateData.isEnabled = false
            btnClearCache.isEnabled = false
        } else {
            progressIndicator.visibility = android.view.View.GONE
            btnUpdateData.isEnabled = true
            btnClearCache.isEnabled = true
        }
    }
}
