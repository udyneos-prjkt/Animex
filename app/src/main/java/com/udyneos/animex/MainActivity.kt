package com.udyneos.animex

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.button.MaterialButton
import com.udyneos.animex.adapter.SectionAdapter
import com.udyneos.animex.model.Anime
import com.udyneos.animex.network.NetworkService
import com.udyneos.animex.utils.CacheManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressIndicator: LinearProgressIndicator
    private lateinit var btnRefresh: MaterialButton
    private lateinit var sectionAdapter: SectionAdapter
    private var allAnime = listOf<Anime>()
    private var backPressedTime = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.recyclerView)
        progressIndicator = findViewById(R.id.progressIndicator)
        btnRefresh = findViewById(R.id.btnRefresh)

        // Inisialisasi NetworkService dengan context
        NetworkService.init(this)

        setupToolbar()
        setupRecyclerView()
        setupRefreshButton()
        
        // Bersihkan cache expired
        CoroutineScope(Dispatchers.IO).launch {
            CacheManager.clearExpiredCache(this@MainActivity)
        }
        
        loadAnimeData()
    }

    private fun setupToolbar() {
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
    }

    private fun setupRecyclerView() {
        sectionAdapter = SectionAdapter(
            onAnimeClick = { anime ->
                val intent = Intent(this, AnimeDetailActivity::class.java)
                intent.putExtra("anime_data", anime)
                startActivity(intent)
            },
            onViewAllClick = { tag ->
                val filteredList = when(tag) {
                    "new" -> allAnime.filter { it.releaseYear >= 2023 }
                    "popular" -> allAnime.filter { it.rating >= 8.5 }
                    "completed" -> allAnime.filter { it.status.equals("Completed", ignoreCase = true) }
                    else -> allAnime
                }
                
                val intent = Intent(this, AllAnimeActivity::class.java).apply {
                    putParcelableArrayListExtra("anime_list", ArrayList(filteredList))
                    putExtra("title", when(tag) {
                        "new" -> "🔥 New Anime 2023+"
                        "popular" -> "⭐ Popular Anime"
                        "completed" -> "✅ Completed Anime"
                        else -> "📺 All Anime"
                    })
                }
                startActivity(intent)
            }
        )

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = sectionAdapter
    }

    private fun setupRefreshButton() {
        btnRefresh.setOnClickListener {
            // Force refresh dari GitHub
            loadAnimeData(forceRefresh = true)
        }
    }

    private fun showLoading(show: Boolean) {
        if (show) {
            progressIndicator.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            progressIndicator.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
    }

    private fun loadAnimeData(forceRefresh: Boolean = false) {
        showLoading(true)
        if (forceRefresh) {
            Toast.makeText(this, "Refreshing data from GitHub...", Toast.LENGTH_SHORT).show()
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val animeList = NetworkService.fetchAnimeList(forceRefresh)
                allAnime = animeList.sortedByDescending { it.rating }

                withContext(Dispatchers.Main) {
                    showLoading(false)
                    setupSections(animeList)
                    
                    val source = if (forceRefresh) "GitHub" else "cache"
                    Toast.makeText(this@MainActivity, "Loaded ${animeList.size} anime from $source", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun setupSections(animeList: List<Anime>) {
        // Kategorikan anime
        val newAnime = animeList
            .filter { it.releaseYear >= 2023 }
            .sortedByDescending { it.rating }
            .take(9) // 3 baris x 3 kolom

        val popularAnime = animeList
            .filter { it.rating >= 8.5 }
            .sortedByDescending { it.rating }
            .take(9)

        val completedAnime = animeList
            .filter { it.status.equals("Completed", ignoreCase = true) }
            .shuffled()
            .take(9)

        val sections = listOf(
            SectionAdapter.Section("🔥 New Anime 2023+", newAnime, "new"),
            SectionAdapter.Section("⭐ Popular Anime", popularAnime, "popular"),
            SectionAdapter.Section("✅ Completed Anime", completedAnime, "completed")
        )

        sectionAdapter.submitSections(sections)
    }

    override fun onBackPressed() {
        if (backPressedTime + 2000 > System.currentTimeMillis()) {
            super.onBackPressed()
            finishAffinity()
        } else {
            Toast.makeText(this, "Tekan sekali lagi untuk keluar", Toast.LENGTH_SHORT).show()
            backPressedTime = System.currentTimeMillis()
        }
    }
}
