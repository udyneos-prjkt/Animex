package com.udyneos.animex

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AboutActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)

        // Setup toolbar
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "About"
        toolbar.setNavigationOnClickListener { finish() }

        // Initialize views
        val ivLogo = findViewById<ImageView>(R.id.ivLogo)
        val tvTitle = findViewById<TextView>(R.id.tvTitle)
        val tvVersion = findViewById<TextView>(R.id.tvVersion)
        val tvLastUpdate = findViewById<TextView>(R.id.tvLastUpdate)
        val tvHeart = findViewById<TextView>(R.id.tvHeart)
        val btnTelegram = findViewById<MaterialButton>(R.id.btnTelegram)
        val btnGithub = findViewById<MaterialButton>(R.id.btnGithub)

        // Set app version
        val versionName = packageManager.getPackageInfo(packageName, 0).versionName
        tvVersion.text = "AnimeX v$versionName - Beta"

        // Set last update (ambil dari build time atau current)
        val lastUpdate = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID"))
            .format(Date(System.currentTimeMillis()))
        tvLastUpdate.text = "Last Update : $lastUpdate"

        // Set heart with HTML
        tvHeart.text = "Dibuat dengan ❤️"

        // Set click listeners for social buttons
        btnTelegram.setOnClickListener {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/udyneos"))
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "Tidak dapat membuka Telegram", Toast.LENGTH_SHORT).show()
            }
        }

        btnGithub.setOnClickListener {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/udyneos-prjkt"))
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "Tidak dapat membuka GitHub", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
