package com.udyneos.animex

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.udyneos.animex.adapter.SectionAdapter
import com.udyneos.animex.model.Anime
import com.udyneos.animex.network.NetworkService
import com.udyneos.animex.utils.SimpleCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var loadingContainer: FrameLayout
    private lateinit var progressIndicator: CircularProgressIndicator
    private lateinit var contentContainer: LinearLayout
    private lateinit var sectionAdapter: SectionAdapter
    
    private var allAnime = listOf<Anime>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.recyclerView)
        swipeRefresh = findViewById(R.id.swipeRefresh)
        bottomNavigation = findViewById(R.id.bottomNavigation)
        loadingContainer = findViewById(R.id.loadingContainer)
        progressIndicator = findViewById(R.id.progressIndicator)
        contentContainer = findViewById(R.id.contentContainer)

        NetworkService.init(this)

        setupRecyclerView()
        setupSwipeRefresh()
        setupBottomNavigation()
        
        loadAnimeData()
    }

    private fun setupRecyclerView() {
        sectionAdapter = SectionAdapter(
            onAnimeClick = { anime ->
                startActivity(Intent(this, AnimeDetailActivity::class.java).apply {
                    putExtra("anime_data", anime)
                })
            },
            onViewAllClick = { tag ->
                // TIDAK MENGIRIM SEMUA DATA VIA INTENT
                // Cukup kirim judul saja, nanti AllAnimeActivity akan ambil dari cache
                startActivity(Intent(this, AllAnimeActivity::class.java).apply {
                    putExtra("title", when (tag) {
                        "popular" -> "⭐ Popular Anime"
                        "new" -> "🔥 New Anime 2023+"
                        "completed" -> "✅ Completed Anime"
                        "recommended" -> "🎯 Recommended For You"
                        else -> "📺 All Anime"
                    })
                })
            }
        )

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = sectionAdapter
    }

    private fun setupSwipeRefresh() {
        swipeRefresh.setColorSchemeColors(getColor(R.color.monet_primary))
        swipeRefresh.setOnRefreshListener {
            loadAnimeData(forceRefresh = true)
        }
    }

    private fun setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true
                R.id.nav_animelist -> {
                    startActivity(Intent(this, AllAnimeActivity::class.java).apply {
                        putExtra("title", "📺 All Anime")
                    })
                    true
                }
                R.id.nav_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    true
                }
                R.id.nav_about -> {
                    startActivity(Intent(this, AboutActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }

    private fun loadAnimeData(forceRefresh: Boolean = false) {
        showLoading(true)
        
        lifecycleScope.launch {
            try {
                val animeList = withContext(Dispatchers.IO) {
                    NetworkService.loadAnimeList(forceRefresh)
                }
                
                allAnime = animeList
                setupSections(animeList)
                showLoading(false)
                swipeRefresh.isRefreshing = false
                
                val source = if (forceRefresh) "GitHub" else "Cache"
                Toast.makeText(this@MainActivity, "Loaded ${animeList.size} anime from $source", Toast.LENGTH_SHORT).show()
                
            } catch (e: Exception) {
                showLoading(false)
                swipeRefresh.isRefreshing = false
                Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setupSections(animeList: List<Anime>) {
        if (animeList.isEmpty()) {
            sectionAdapter.submitSections(emptyList())
            return
        }
        
        val sections = listOf(
            SectionAdapter.Section("⭐ Popular Anime", 
                animeList.filter { it.rating >= 8.5 }.take(9), "popular"),
            SectionAdapter.Section("🔥 New Anime 2023+", 
                animeList.filter { it.releaseYear >= 2023 }.take(9), "new"),
            SectionAdapter.Section("✅ Completed Anime", 
                animeList.filter { it.status.equals("Completed", ignoreCase = true) }.take(9), "completed"),
            SectionAdapter.Section("🎯 Recommended For You", 
                animeList.shuffled().take(9), "recommended")
        )
        sectionAdapter.submitSections(sections)
    }

    private fun showLoading(show: Boolean) {
        if (show) {
            loadingContainer.visibility = View.VISIBLE
            contentContainer.visibility = View.GONE
        } else {
            loadingContainer.visibility = View.GONE
            contentContainer.visibility = View.VISIBLE
        }
    }
}
