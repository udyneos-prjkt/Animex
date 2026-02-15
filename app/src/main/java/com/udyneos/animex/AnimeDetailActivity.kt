package com.udyneos.animex

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.udyneos.animex.model.Anime

class AnimeDetailActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_anime_detail)
        
        val anime = intent.getParcelableExtra<Anime>("anime_data")
        
        if (anime == null) {
            Toast.makeText(this, "Anime data not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        findViewById<TextView>(R.id.tvTitle).text = anime.title
        findViewById<TextView>(R.id.tvDescription).text = anime.description
        findViewById<TextView>(R.id.tvGenre).text = anime.genre
        findViewById<TextView>(R.id.tvRating).text = String.format("%.1f", anime.rating)
        findViewById<TextView>(R.id.tvStatus).text = anime.status
        findViewById<TextView>(R.id.tvEpisodeCount).text = anime.episodeCount
        findViewById<TextView>(R.id.tvStudio).text = anime.studio
        findViewById<TextView>(R.id.tvReleaseYear).text = anime.releaseYear.toString()
        
        findViewById<Button>(R.id.btnPlay).setOnClickListener {
            val intent = Intent(this, VideoPlayerActivity::class.java)
            intent.putExtra("video_url", anime.videoUrl)
            intent.putExtra("anime_title", anime.title)
            startActivity(intent)
        }
    }
}
