package com.udyneos.animex

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.PlayerView

class VideoPlayerActivity : AppCompatActivity() {
    
    private lateinit var playerView: PlayerView
    private lateinit var progressBar: ProgressBar
    private var exoPlayer: ExoPlayer? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_player)
        
        playerView = findViewById(R.id.playerView)
        progressBar = findViewById(R.id.progressBar)
        
        val videoUrl = intent.getStringExtra("video_url") ?: ""
        val episodeTitle = intent.getStringExtra("episode_title") ?: "Video Player"
        
        supportActionBar?.title = episodeTitle
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        if (videoUrl.isEmpty()) {
            Toast.makeText(this, "Video URL not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        initializePlayer(videoUrl)
    }
    
    private fun initializePlayer(videoUrl: String) {
        try {
            exoPlayer = ExoPlayer.Builder(this).build()
            playerView.player = exoPlayer
            
            val mediaItem = MediaItem.fromUri(Uri.parse(videoUrl))
            exoPlayer?.setMediaItem(mediaItem)
            exoPlayer?.prepare()
            exoPlayer?.playWhenReady = true
            
            exoPlayer?.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    when (playbackState) {
                        ExoPlayer.STATE_BUFFERING -> progressBar.visibility = View.VISIBLE
                        ExoPlayer.STATE_READY -> progressBar.visibility = View.GONE
                    }
                }
                
                override fun onPlayerError(error: PlaybackException) {
                    progressBar.visibility = View.GONE
                    Toast.makeText(this@VideoPlayerActivity, "Error: ${error.message}", Toast.LENGTH_LONG).show()
                }
            })
        } catch (e: Exception) {
            Toast.makeText(this, "Error initializing player: ${e.message}", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
    
    override fun onStop() {
        super.onStop()
        releasePlayer()
    }
    
    private fun releasePlayer() {
        exoPlayer?.let {
            it.playWhenReady = false
            it.release()
        }
        exoPlayer = null
    }
    
    override fun onDestroy() {
        super.onDestroy()
        releasePlayer()
    }
}
