package com.udyneos.animex

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.udyneos.animex.adapter.AnimeAdapter
import com.udyneos.animex.model.Anime

class AllAnimeActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var searchView: EditText
    private lateinit var ivClearSearch: ImageView
    private lateinit var tvNoResults: TextView
    private lateinit var tvTotalCount: TextView
    private lateinit var animeAdapter: AnimeAdapter
    private var allAnime = listOf<Anime>()
    private var filteredAnime = listOf<Anime>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_all_anime)

        recyclerView = findViewById(R.id.recyclerView)
        searchView = findViewById(R.id.searchView)
        ivClearSearch = findViewById(R.id.ivClearSearch)
        tvNoResults = findViewById(R.id.tvNoResults)
        tvTotalCount = findViewById(R.id.tvTotalCount)

        // Setup toolbar
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = intent.getStringExtra("title") ?: "All Anime"
        toolbar.setNavigationOnClickListener { finish() }

        // Get data
        allAnime = intent.getParcelableArrayListExtra("anime_list") ?: emptyList()
        filteredAnime = allAnime

        tvTotalCount.text = "Menampilkan ${filteredAnime.size} anime"

        setupRecyclerView()
        setupSearch()
    }

    private fun setupRecyclerView() {
        animeAdapter = AnimeAdapter { anime ->
            val intent = Intent(this, AnimeDetailActivity::class.java)
            intent.putExtra("anime_data", anime)
            startActivity(intent)
        }

        recyclerView.layoutManager = GridLayoutManager(this, 3)
        recyclerView.adapter = animeAdapter
        animeAdapter.submitList(filteredAnime)
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
        filteredAnime = if (query.isEmpty()) {
            allAnime
        } else {
            allAnime.filter { anime ->
                anime.title.contains(query, ignoreCase = true) ||
                anime.genre.contains(query, ignoreCase = true) ||
                anime.studio.contains(query, ignoreCase = true)
            }
        }

        if (filteredAnime.isEmpty()) {
            tvNoResults.visibility = View.VISIBLE
            tvNoResults.text = "Tidak ada hasil untuk \"$query\""
        } else {
            tvNoResults.visibility = View.GONE
        }

        tvTotalCount.text = "Menampilkan ${filteredAnime.size} anime"
        animeAdapter.submitList(filteredAnime)
    }
}
