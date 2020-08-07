package com.grand.duke.elliot.kim.kotlin.redvelvetmvcollection

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.AttributeSet
import android.view.*
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.item_view.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Response
import java.io.IOException

class MainActivity : AppCompatActivity() {

    lateinit var favoriteVideoIds: MutableSet<String>
    private lateinit var recyclerViewAdapter: RecyclerViewAdapter
    private lateinit var youtubeDataApi: YouTubeDataApi

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayShowTitleEnabled(false)
        toolbar.title = "RedVelvet MV"

        favoriteVideoIds = loadFavoriteVideoIds()

        youtubeDataApi = YouTubeDataApi()

        recyclerViewAdapter = RecyclerViewAdapter(null, favoriteVideoIds)
        recycler_view.apply {
            adapter = recyclerViewAdapter
            layoutManager = GridLayoutManagerWrapper(this@MainActivity, 1)
        }

        MobileAds.initialize(this)
        ad_view.loadAd(AdRequest.Builder().build())
        val adListener = object : AdListener() {
            @Suppress("DEPRECATION")
            override fun onAdFailedToLoad(p0: Int) {
                println("$TAG: onAdFailedToLoad")
                super.onAdFailedToLoad(p0)
            }

            override fun onAdLoaded() {
                super.onAdLoaded()
                println("$TAG: onAdLoaded")
            }
        }

        ad_view.adListener = adListener
    }

    override fun onBackPressed() {
        if (supportFragmentManager.findFragmentByTag(TAG_FAVORITE_VIDEOS_FRAGMENT) != null)
            recyclerViewAdapter.notifyDataSetChanged()
        super.onBackPressed()
    }

    override fun onPause() {
        saveFavoriteVideoIds()
        super.onPause()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.item_auto_play -> {  }
            R.id.item_watchlist -> startFavoriteVideosFragment(favoriteVideoIds)
            else -> {  }
        }

        return super.onOptionsItemSelected(item)
    }

    private fun startYouTubePlayerActivity(videoId: String) {
        val intent = Intent(this, YouTubePlayerActivity::class.java)
        intent.action = ACTION_YOUTUBE_PLAYER
        intent.putExtra(KEY_VIDEO_ID, videoId)
        startActivity(intent)
    }

    private fun startFavoriteVideosFragment(favoriteVideoIds: Set<String>) {
        val favoriteVideos = allVideos.filter { favoriteVideoIds.contains(it.id) }
        val favoriteVideosFragment = FavoriteVideosFragment()
        favoriteVideosFragment.setVideos(favoriteVideos)
        supportFragmentManager.beginTransaction()
            .addToBackStack(null)
            .setCustomAnimations(
                R.anim.anim_slide_in_from_bottom,
                R.anim.anim_slide_out_to_bottom,
                R.anim.anim_slide_in_from_top,
                R.anim.anim_slide_out_to_top
            ).replace(
                R.id.relative_layout_frame,
                favoriteVideosFragment,
                TAG_FAVORITE_VIDEOS_FRAGMENT
            ).commit()
    }

    private fun saveFavoriteVideoIds() {
        getSharedPreferences(PREFERENCES_FAVORITE_VIDEO_IDS, Context.MODE_PRIVATE).edit()
            .putStringSet(KEY_FAVORITE_VIDEO_IDS, favoriteVideoIds).apply()
    }

    private fun loadFavoriteVideoIds(): MutableSet<String> = getSharedPreferences(PREFERENCES_FAVORITE_VIDEO_IDS,
        Context.MODE_PRIVATE).getStringSet(KEY_FAVORITE_VIDEO_IDS, mutableSetOf())?.toMutableSet() ?: mutableSetOf()


    inner class RecyclerViewAdapter(private val favoriteVideos: List<VideoModel>? = null,
                                    private val favoriteVideoIds: MutableSet<String>)
        : RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder>() {

        private lateinit var recyclerView: RecyclerView
        private var videos = mutableListOf<VideoModel>()

        init {
            if (favoriteVideos == null)
                setVideosByPlaylistId()
        }

        inner class ViewHolder(val view: View) : RecyclerView.ViewHolder(view)

        override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
            super.onAttachedToRecyclerView(recyclerView)
            this.recyclerView = recyclerView

            if (favoriteVideos != null) {
                videos = favoriteVideos.toMutableList()
                notifyDataSetChanged()
                recyclerView.scheduleLayoutAnimation()
            }
        }

        override fun onCreateViewHolder(
                parent: ViewGroup,
                viewType: Int
        ): RecyclerViewAdapter.ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_view, parent, false)
            return ViewHolder(view)
        }

        override fun getItemCount(): Int {
            return videos.size
        }

        override fun onBindViewHolder(holder: RecyclerViewAdapter.ViewHolder, position: Int) {
            val video = videos[position]
            val view = holder.view

            Glide.with(view.image_view_thumbnail.context)
                .load(video.thumbnailUri)
                .error(R.drawable.ic_sentiment_dissatisfied_grey_32dp)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(view.image_view_thumbnail)

            view.text_view_title.text = video.title

            view.setOnClickListener {
                startYouTubePlayerActivity(video.id)
            }

            view.image_view_favorite.setImageResource(if (favoriteVideoIds.contains(video.id))
                R.drawable.ic_favorite_blue_24dp
            else R.drawable.ic_favorite_grey_24dp)
            view.image_view_favorite.setOnClickListener {
                if (favoriteVideoIds.contains(video.id)) {
                    (it as ImageView).setImageResource(R.drawable.ic_favorite_grey_24dp)
                    favoriteVideoIds.remove(video.id)
                } else {
                    (it as ImageView).setImageResource(R.drawable.ic_favorite_blue_24dp)
                    favoriteVideoIds.add(video.id)
                }
            }
        }

        private fun setVideosByPlaylistId() {
            val request = youtubeDataApi.getVideosRequestByPlaylistId()
            val okHttpClient = OkHttpClient()

            okHttpClient.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    showToast(getString(R.string.failed_to_load_videos))
                    e.printStackTrace()
                }

                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        try {
                            val videoIds =
                                    youtubeDataApi.getVideoIdsFromResponse(response)
                            if (videoIds != null)
                                setVideosByIds(videoIds.joinToString(separator = ","))
                            else
                                showToast(getString(R.string.failed_to_load_videos))
                        } catch (e: Exception) {
                            showToast(getString(R.string.failed_to_load_videos))
                            e.printStackTrace()
                        }
                    } else
                        showToast(getString(R.string.failed_to_load_videos))
                }
            })
        }

        private fun setVideosByIds(videoIds: String) {
            val request = youtubeDataApi.getVideosRequestByIds(videoIds)
            val okHttpClient = OkHttpClient()
            okHttpClient.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    showToast(getString(R.string.failed_to_load_videos))
                    e.printStackTrace()
                }

                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        try {
                            this@RecyclerViewAdapter.videos = youtubeDataApi.getVideosFromResponse(response)
                            allVideos = videos.toSet()
                            CoroutineScope(Dispatchers.Main).launch {
                                notifyDataSetChanged()
                                recyclerView.scheduleLayoutAnimation()
                            }
                        } catch (e: Exception) {
                            showToast(getString(R.string.failed_to_load_videos))
                            e.printStackTrace()
                        }
                    }
                }
            })
        }
    }

    private fun showToast(text: String, duration: Int = Toast.LENGTH_SHORT) {
        CoroutineScope(Dispatchers.Main).launch {
            Toast.makeText(this@MainActivity, text, duration).show()
        }
    }

    class GridLayoutManagerWrapper: GridLayoutManager {
        constructor(context: Context, spanCount: Int) : super(context, spanCount)
        constructor(context: Context, spanCount: Int, orientation: Int, reverseLayout: Boolean) :
                super(context, spanCount, orientation, reverseLayout)
        constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int) :
                super(context, attrs, defStyleAttr, defStyleRes)
        override fun supportsPredictiveItemAnimations(): Boolean { return false }
    }

    companion object {
        private const val TAG = "MainActivity"
        private const val TAG_FAVORITE_VIDEOS_FRAGMENT = "tag.favorite.videos.fragment"
        private const val PREFERENCES_FAVORITE_VIDEO_IDS = "preferences_favorite_video_ids"
        private const val KEY_FAVORITE_VIDEO_IDS = "key_favorite_video_ids"

        const val ACTION_YOUTUBE_PLAYER = "main.activity.action.youtube.player"
        const val KEY_VIDEO_ID = "key_video_id"

        var allVideos = setOf<VideoModel>()
    }
}
