package com.udyneos.animex

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.udyneos.animex.adapter.AnimeAdapter
import com.udyneos.animex.model.Anime
import com.udyneos.animex.utils.SimpleCache

class AllAnimeActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var searchView: EditText
    private lateinit var ivClearSearch: ImageView
    private lateinit var tvNoResults: TextView
    private lateinit var tvTotalCount: TextView
    private lateinit var progressBar: CircularProgressIndicator
    private lateinit var animeAdapter: AnimeAdapter
    
    // Data management
    private var allAnime = listOf<Anime>()
    private var filteredAnime = listOf<Anime>()
    private val displayedAnime = mutableListOf<Anime>()
    
    // Pagination
    private var currentPage = 0
    private val pageSize = 30
    private var isLoading = false
    private var isLastPage = false
    
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_all_anime)

        recyclerView = findViewById(R.id.recyclerView)
        searchView = findViewById(R.id.searchView)
        ivClearSearch = findViewById(R.id.ivClearSearch)
        tvNoResults = findViewById(R.id.tvNoResults)
        tvTotalCount = findViewById(R.id.tvTotalCount)
        progressBar = findViewById(R.id.progressBar)

        // Setup toolbar
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = intent.getStringExtra("title") ?: "All Anime"
        toolbar.setNavigationOnClickListener { finish() }

        // Ambil dari cache
        loadAnimeFromCache()
        
        setupRecyclerView()
        setupSearch()
        setupScrollListener()
    }

    private fun loadAnimeFromCache() {
        showLoading(true)
        
        // Ambil dari cache
        allAnime = SimpleCache.getAnimeList()
        filteredAnime = allAnime
        tvTotalCount.text = "Total ${filteredAnime.size} anime"
        
        // Reset pagination
        currentPage = 0
        isLastPage = false
        displayedAnime.clear()
        
        // Load page pertama
        handler.postDelayed({
            loadNextPage()
        }, 300)
    }

    private fun setupRecyclerView() {
        animeAdapter = AnimeAdapter { anime ->
            val intent = Intent(this, AnimeDetailActivity::class.java)
            intent.putExtra("anime_data", anime)
            startActivity(intent)
        }

        recyclerView.layoutManager = GridLayoutManager(this, 3)
        recyclerView.adapter = animeAdapter
    }

    private fun setupScrollListener() {
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                
                val layoutManager = recyclerView.layoutManager as GridLayoutManager
                val visibleItemCount = layoutManager.childCount
                val totalItemCount = layoutManager.itemCount
                val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
                
                if (!isLoading && !isLastPage) {
                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount 
                        && firstVisibleItemPosition >= 0
                        && totalItemCount >= pageSize) {
                        loadNextPage()
                    }
                }
            }
        })
    }

    private fun loadNextPage() {
        if (isLoading || isLastPage) return
        
        // Validasi ukuran list
        if (filteredAnime.isEmpty()) {
            isLastPage = true
            showLoading(false)
            return
        }
        
        isLoading = true
        
        val start = currentPage * pageSize
        val end = minOf(start + pageSize, filteredAnime.size)
        
        // Validasi index
        if (start >= filteredAnime.size) {
            isLastPage = true
            isLoading = false
            showLoading(false)
            return
        }
        
        // Pastikan end > start
        if (end <= start) {
            isLastPage = true
            isLoading = false
            showLoading(false)
            return
        }
        
        try {
            val nextPage = filteredAnime.subList(start, end)
            displayedAnime.addAll(nextPage)
            
            // Update adapter di UI thread
            runOnUiThread {
                animeAdapter.submitList(displayedAnime.toList())
            }
            
            currentPage++
            
            // Cek apakah sudah mencapai akhir
            if (end >= filteredAnime.size) {
                isLastPage = true
            }
            
        } catch (e: IndexOutOfBoundsException) {
            e.printStackTrace()
            isLastPage = true
        } finally {
            isLoading = false
            showLoading(false)
        }
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

        ivClearSearch.setOnClickListener {
            searchView.text.clear()
        }
    }

    private fun filterAnime(query: String) {
        // Reset pagination
        currentPage = 0
        isLastPage = false
        displayedAnime.clear()
        
        filteredAnime = if (query.isEmpty()) {
            allAnime
        } else {
            allAnime.filter { anime ->
                anime.title.contains(query, ignoreCase = true) ||
                anime.genre.contains(query, ignoreCase = true) ||
                anime.studio.contains(query, ignoreCase = true)
            }
        }

        tvTotalCount.text = "Total ${filteredAnime.size} anime"
        
        if (filteredAnime.isEmpty()) {
            tvNoResults.visibility = View.VISIBLE
            tvNoResults.text = "Tidak ada hasil untuk \"$query\""
            animeAdapter.submitList(emptyList())
            showLoading(false)
        } else {
            tvNoResults.visibility = View.GONE
            // Load page pertama dari hasil filter
            handler.postDelayed({
                loadNextPage()
            }, 300)
        }
    }

    private fun showLoading(show: Boolean) {
        runOnUiThread {
            if (show && displayedAnime.isEmpty()) {
                progressBar.visibility = View.VISIBLE
                recyclerView.visibility = View.GONE
            } else {
                progressBar.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE
            }
        }
    }
}
