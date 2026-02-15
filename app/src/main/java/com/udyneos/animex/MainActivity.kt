package com.udyneos.animex

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.udyneos.animex.adapter.AnimeAdapter
import com.udyneos.animex.model.Anime
import com.udyneos.animex.utils.XmlParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var animeAdapter: AnimeAdapter
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        recyclerView = findViewById(R.id.recyclerView)
        swipeRefresh = findViewById(R.id.swipeRefresh)
        
        setupRecyclerView()
        loadAnimeList()
        setupRefreshListener()
    }
    
    private fun setupRecyclerView() {
        animeAdapter = AnimeAdapter { anime ->
            val intent = Intent(this, AnimeDetailActivity::class.java)
            intent.putExtra("anime_data", anime)
            startActivity(intent)
        }
        
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = animeAdapter
    }
    
    private fun loadAnimeList() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val inputStream = assets.open("anime_list.xml")
                val animeList = XmlParser.parseAnimeList(inputStream)
                
                withContext(Dispatchers.Main) {
                    animeAdapter.submitList(animeList)
                    swipeRefresh.isRefreshing = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    swipeRefresh.isRefreshing = false
                }
            }
        }
    }
    
    private fun setupRefreshListener() {
        swipeRefresh.setOnRefreshListener {
            loadAnimeList()
        }
    }
}
