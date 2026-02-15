package com.udyneos.animex

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.udyneos.animex.adapter.AnimeAdapter
import com.udyneos.animex.databinding.ActivityMainBinding
import com.udyneos.animex.model.Anime
import com.udyneos.animex.network.UpdateChecker
import com.udyneos.animex.utils.XmlParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private lateinit var animeAdapter: AnimeAdapter
    private val updateChecker = UpdateChecker()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupRecyclerView()
        loadAnimeList()
        checkForUpdates()
        setupRefreshListener()
    }
    
    private fun setupRecyclerView() {
        animeAdapter = AnimeAdapter { anime ->
            navigateToDetail(anime)
        }
        
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = animeAdapter
        }
    }
    
    private fun loadAnimeList() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val inputStream = assets.open("anime_list.xml")
                val animeList = XmlParser.parseAnimeList(inputStream)
                
                withContext(Dispatchers.Main) {
                    animeAdapter.submitList(animeList)
                    hideLoading()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@MainActivity,
                        "Error loading anime list: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    hideLoading()
                }
            }
        }
    }
    
    private fun checkForUpdates() {
        CoroutineScope(Dispatchers.IO).launch {
            val hasUpdate = updateChecker.checkForUpdate()
            
            withContext(Dispatchers.Main) {
                if (hasUpdate) {
                    showUpdateNotification()
                }
            }
        }
    }
    
    private fun setupRefreshListener() {
        binding.swipeRefresh.setOnRefreshListener {
            loadAnimeList()
            checkForUpdates()
        }
    }
    
    private fun showUpdateNotification() {
        binding.updateBanner.visibility = android.view.View.VISIBLE
        binding.updateBanner.setOnClickListener {
            startUpdateProcess()
        }
    }
    
    private fun startUpdateProcess() {
        CoroutineScope(Dispatchers.IO).launch {
            val success = updateChecker.downloadAndUpdate()
            
            withContext(Dispatchers.Main) {
                if (success) {
                    Toast.makeText(
                        this@MainActivity,
                        "Update successful!",
                        Toast.LENGTH_SHORT
                    ).show()
                    binding.updateBanner.visibility = android.view.View.GONE
                    loadAnimeList()
                } else {
                    Toast.makeText(
                        this@MainActivity,
                        "Update failed!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
    
    private fun navigateToDetail(anime: Anime) {
        val intent = Intent(this, AnimeDetailActivity::class.java)
        intent.putExtra("anime_data", anime)
        startActivity(intent)
    }
    
    private fun hideLoading() {
        binding.swipeRefresh.isRefreshing = false
    }
}
