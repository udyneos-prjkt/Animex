package com.udyneos.animex

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.udyneos.animex.adapter.AnimeAdapter
import com.udyneos.animex.model.Anime
import com.udyneos.animex.network.NetworkService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressIndicator: LinearProgressIndicator
    private lateinit var tvLoadingStatus: TextView
    private lateinit var ivLogo: ImageView
    private lateinit var searchView: EditText
    private lateinit var ivSearchIcon: ImageView
    private lateinit var ivClearSearch: ImageView
    private lateinit var tvNoResults: TextView
    private lateinit var animeAdapter: AnimeAdapter
    private var allAnime = listOf<Anime>()
    private var filteredAnime = listOf<Anime>()
    private var backPressedTime = 0L
    private var currentPage = 0
    private val pageSize = 50
    private var isLoading = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        recyclerView = findViewById(R.id.recyclerView)
        progressIndicator = findViewById(R.id.progressIndicator)
        tvLoadingStatus = findViewById(R.id.tvLoadingStatus)
        ivLogo = findViewById(R.id.ivLogo)
        searchView = findViewById(R.id.searchView)
        ivSearchIcon = findViewById(R.id.ivSearchIcon)
        ivClearSearch = findViewById(R.id.ivClearSearch)
        tvNoResults = findViewById(R.id.tvNoResults)
        
        setupToolbar()
        setupRecyclerView()
        setupSearch()
        fetchFirst50Anime()
    }
    
    private fun setupToolbar() {
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
    }
    
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_search -> {
                // Focus ke search view
                searchView.requestFocus()
                true
            }
            R.id.action_settings -> {
                Toast.makeText(this, "Settings feature coming soon", Toast.LENGTH_SHORT).show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun setupRecyclerView() {
        // GridLayout dengan 3 kolom
        val gridLayoutManager = GridLayoutManager(this, 3)
        recyclerView.layoutManager = gridLayoutManager
        
        animeAdapter = AnimeAdapter { anime ->
            val intent = Intent(this, AnimeDetailActivity::class.java)
            intent.putExtra("anime_data", anime)
            startActivity(intent)
        }
        
        recyclerView.adapter = animeAdapter
        
        // Setup scroll listener untuk infinite scroll
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                
                val visibleItemCount = gridLayoutManager.childCount
                val totalItemCount = gridLayoutManager.itemCount
                val firstVisibleItemPosition = gridLayoutManager.findFirstVisibleItemPosition()
                
                if (!isLoading && !searchView.text.isNullOrEmpty()) {
                    val lastVisibleItem = firstVisibleItemPosition + visibleItemCount
                    if (lastVisibleItem >= totalItemCount - 5) {
                        // Load more saat scroll ke bawah
                        loadMoreAnime()
                    }
                }
            }
        })
    }
    
    private fun setupSearch() {
        searchView.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterAnime(s.toString())
                ivClearSearch.visibility = if (s.isNullOrEmpty()) View.GONE else View.VISIBLE
            }
            
            override fun afterTextChanged(s: Editable?) {}
        })
        
        ivSearchIcon.setOnClickListener {
            searchView.requestFocus()
        }
        
        ivClearSearch.setOnClickListener {
            searchView.text.clear()
        }
    }
    
    private fun filterAnime(query: String) {
        if (query.isEmpty()) {
            // Tampilkan 50 anime pertama
            filteredAnime = allAnime.take(50)
            tvNoResults.visibility = View.GONE
        } else {
            // Filter berdasarkan judul atau genre
            filteredAnime = allAnime.filter { anime ->
                anime.title.contains(query, ignoreCase = true) ||
                anime.genre.contains(query, ignoreCase = true) ||
                anime.studio.contains(query, ignoreCase = true)
            }
            
            if (filteredAnime.isEmpty()) {
                tvNoResults.visibility = View.VISIBLE
                tvNoResults.text = "No results found for \"$query\""
            } else {
                tvNoResults.visibility = View.GONE
            }
        }
        
        animeAdapter.submitList(filteredAnime)
    }
    
    private fun fetchFirst50Anime() {
        showLoading(true)
        tvLoadingStatus.text = "Loading 50 anime..."
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Ambil semua anime
                val allAnimeData = NetworkService.fetchAnimeList()
                allAnime = allAnimeData
                
                // Ambil 50 pertama
                val first50 = allAnimeData.take(50)
                filteredAnime = first50
                
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    animeAdapter.submitList(first50)
                    
                    if (first50.isNotEmpty()) {
                        tvLoadingStatus.text = "Loaded ${first50.size} anime"
                        delay(1500)
                        tvLoadingStatus.visibility = View.GONE
                    } else {
                        tvLoadingStatus.text = "No anime data found"
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    tvLoadingStatus.text = "Error: ${e.message}"
                    Toast.makeText(this@MainActivity, "Error loading anime", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun loadMoreAnime() {
        if (searchView.text.isNotEmpty()) return // Jangan load more saat searching
        
        isLoading = true
        val startIndex = currentPage * pageSize + 50
        val endIndex = minOf(startIndex + pageSize, allAnime.size)
        
        if (startIndex >= allAnime.size) {
            isLoading = false
            return
        }
        
        tvLoadingStatus.visibility = View.VISIBLE
        tvLoadingStatus.text = "Loading more anime..."
        
        CoroutineScope(Dispatchers.Main).launch {
            delay(500) // Delay untuk UX
            
            val moreAnime = allAnime.subList(startIndex, endIndex)
            val currentList = filteredAnime.toMutableList()
            currentList.addAll(moreAnime)
            filteredAnime = currentList
            
            animeAdapter.submitList(filteredAnime)
            
            currentPage++
            isLoading = false
            tvLoadingStatus.visibility = View.GONE
        }
    }
    
    private fun showLoading(show: Boolean) {
        if (show) {
            progressIndicator.visibility = View.VISIBLE
            tvLoadingStatus.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            progressIndicator.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
    }
    
    override fun onBackPressed() {
        if (searchView.text.isNotEmpty()) {
            searchView.text.clear()
            return
        }
        
        if (backPressedTime + 2000 > System.currentTimeMillis()) {
            super.onBackPressed()
            finishAffinity()
        } else {
            Toast.makeText(this, "Tekan sekali lagi untuk keluar", Toast.LENGTH_SHORT).show()
            backPressedTime = System.currentTimeMillis()
        }
    }
}
