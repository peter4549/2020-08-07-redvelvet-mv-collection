package com.grand.duke.elliot.kim.kotlin.redvelvetmvcollection

import android.os.Bundle
import android.widget.Toast
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.google.android.youtube.player.YouTubeBaseActivity
import com.google.android.youtube.player.YouTubeInitializationResult
import com.google.android.youtube.player.YouTubePlayer
import kotlinx.android.synthetic.main.activity_you_tube_player.*
import java.util.*


class YouTubePlayerActivity : YouTubeBaseActivity() {

    private var playOptions = -1
    private var videoId: String? = null

    /*
    private val drawableImageIds = arrayOf(
        R.drawable.irene_00,
        R.drawable.irene_01,
        R.drawable.irene_02,
        R.drawable.irene_03,
        R.drawable.joy_00,
        R.drawable.joy_01,
        R.drawable.joy_02,
        R.drawable.joy_03,
        R.drawable.seulgi_00,
        R.drawable.seulgi_01,
        R.drawable.seulgi_02,
        R.drawable.seulgi_03,
        R.drawable.wendy_00,
        R.drawable.wendy_01,
        R.drawable.wendy_02,
        R.drawable.wendy_03,
        R.drawable.yeri_00,
        R.drawable.yeri_01,
        R.drawable.yeri_02,
        R.drawable.yeri_03
    )
     */

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_you_tube_player)

        // val drawableImageId = drawableImageIds[Random().nextInt(drawableImageIds.count())]

        if (intent.action != MainActivity.ACTION_YOUTUBE_PLAYER)
            finish()

        playOptions = intent.getIntExtra(MainActivity.KEY_PLAY_OPTIONS, -1)
        videoId = intent.getStringExtra(MainActivity.KEY_VIDEO_ID)
        
        if (playOptions == -1) {
            Toast.makeText(this, "알 수 없는 오류가 발생했습니다.", Toast.LENGTH_LONG).show()
            finish()
        }

        /*
        Glide.with(image_view.context)
            .load(drawableImageId)
            .error(R.drawable.ic_sentiment_dissatisfied_grey_32dp)
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(image_view)
         */

        youtube_player_view.initialize(TAG, object : YouTubePlayer.OnInitializedListener {
            override fun onInitializationSuccess(
                provider: YouTubePlayer.Provider?,
                player: YouTubePlayer?,
                wasRestored: Boolean
            ) {
                if (!wasRestored) {
                    when(playOptions) {
                        MainActivity.PLAY_SINGLE_VIDEO -> player?.cueVideo(videoId)
                        MainActivity.PLAY_ALL_VIDEOS -> player?.cueVideos(MainActivity.allVideos.map { it.id }.toList())
                        MainActivity.PLAY_WATCHLIST_VIDEOS -> player?.cueVideos(MainActivity.favoriteVideoIds.toList())
                    }
                }
                    

                player?.setPlayerStateChangeListener(object : YouTubePlayer.PlayerStateChangeListener {
                    override fun onAdStarted() {  }
                    override fun onLoading() {  }
                    override fun onVideoStarted() {  }

                    override fun onLoaded(videoId: String?) {
                        player.play()
                    }

                    override fun onVideoEnded() {  }

                    override fun onError(errorReason: YouTubePlayer.ErrorReason?) {
                        println("TAG: $errorReason")
                    }

                })
            }

            override fun onInitializationFailure(
                provider: YouTubePlayer.Provider?,
                result: YouTubeInitializationResult?
            ) {
                Toast.makeText(this@YouTubePlayerActivity,
                    getString(R.string.player_initialization_failure_message), Toast.LENGTH_LONG).show()
            }

        })
    }

    companion object {
        private const val TAG = "YouTubePlayerActivity"
    }
}
