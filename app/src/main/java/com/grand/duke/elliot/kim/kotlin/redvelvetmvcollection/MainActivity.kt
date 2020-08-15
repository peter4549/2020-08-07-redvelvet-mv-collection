package com.grand.duke.elliot.kim.kotlin.redvelvetmvcollection

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
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

    private lateinit var recyclerViewAdapter: RecyclerViewAdapter
    private lateinit var youtubeDataApi: YouTubeDataApi
    private lateinit var stringOfVideoIds: String
    private var dataUpdate = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        activeRemoteConfig()

        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayShowTitleEnabled(false)
        toolbar.title = "RedVelvet MV"

        stringOfVideoIds = loadStringOfVideoIds()
        favoriteVideoIds = loadFavoriteVideoIds()

        youtubeDataApi = YouTubeDataApi()

        recyclerViewAdapter = RecyclerViewAdapter(this, null, favoriteVideoIds)
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
        if (supportFragmentManager.findFragmentByTag(TAG_FAVORITE_VIDEOS_FRAGMENT) != null) {
            recyclerViewAdapter.notifyDataSetChanged()
            super.onBackPressed()
        }
        else
            ExitDialogFragment().show(supportFragmentManager, TAG)
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
            R.id.item_auto_play -> startYouTubePlayerActivity(this, null, PLAY_ALL_VIDEOS)
            R.id.item_watchlist -> startFavoriteVideosFragment(favoriteVideoIds)
        }

        return super.onOptionsItemSelected(item)
    }

    private fun activeRemoteConfig() {
        val firebaseRemoteConfig = FirebaseRemoteConfig.getInstance()
        firebaseRemoteConfig.setDefaultsAsync(R.xml.initial_config)
        firebaseRemoteConfig.fetch(0).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                firebaseRemoteConfig.fetchAndActivate()
                runRemoteConfig(firebaseRemoteConfig)
            } else {
                println("$TAG: ${task.exception}")
            }
        }
    }

    private fun runRemoteConfig(firebaseRemoteConfig: FirebaseRemoteConfig) {
        val versionUpdate = firebaseRemoteConfig.getBoolean("version_update")
        val quitWhenNotUpdating = firebaseRemoteConfig.getBoolean("quit_when_not_updating")
        val updateMessage = firebaseRemoteConfig.getString("update_message")
        val downloadLink = firebaseRemoteConfig.getString("download_link")
        val versionName = firebaseRemoteConfig.getString("version_name")
        val currentVersionName = getVersionName()

        dataUpdate = firebaseRemoteConfig.getBoolean("data_update")

        if (versionUpdate) {
            println("$TAG: current version: $currentVersionName, target version: $versionName")
            if (versionName != currentVersionName) {
                val notificationDialogFragment = NotificationDialogFragment(
                    updateMessage,
                    downloadLink, quitWhenNotUpdating
                )
                notificationDialogFragment.show(supportFragmentManager, TAG)
            }
        }
    }

    fun getVersionName(): String {
        return try {
            packageManager.getPackageInfo(packageName, 0).versionName
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
            return "version info not found"
        }
    }

    fun startYouTubePlayerActivity(context: Context, videoId: String?, playOptions: Int) {
        val intent = Intent(context, YouTubePlayerActivity::class.java)
        intent.action = ACTION_YOUTUBE_PLAYER
        intent.putExtra(KEY_PLAY_OPTIONS, playOptions)
        intent.putExtra(KEY_VIDEO_ID, videoId)
        context.startActivity(intent)
    }

    private fun startFavoriteVideosFragment(favoriteVideoIds: Set<String>) {
        val favoriteVideos = allVideos.filter { favoriteVideoIds.contains(it.id) }
        val favoriteVideosFragment = FavoriteVideosFragment()
        favoriteVideosFragment.setVideos(favoriteVideos)
        supportFragmentManager.beginTransaction()
            .addToBackStack(null)
            .setCustomAnimations(
                R.anim.anim_slide_in_from_bottom,
                R.anim.anim_slide_out_to_top,
                R.anim.anim_slide_in_from_top,
                R.anim.anim_slide_out_to_bottom
            ).replace(
                R.id.relative_layout_frame,
                favoriteVideosFragment,
                TAG_FAVORITE_VIDEOS_FRAGMENT
            ).commit()
    }

    private fun saveStringOfVideoIds(string: String) {
        getSharedPreferences(PREFERENCES_VIDEO_IDS, Context.MODE_PRIVATE).edit()
            .putString(KEY_STRING_OF_VIDEO_IDS, string).apply()
    }

    private fun loadStringOfVideoIds() = getSharedPreferences(PREFERENCES_VIDEO_IDS,
        Context.MODE_PRIVATE).getString(KEY_STRING_OF_VIDEO_IDS, "") ?: ""

    private fun saveFavoriteVideoIds() {
        getSharedPreferences(PREFERENCES_VIDEO_IDS, Context.MODE_PRIVATE).edit()
            .putStringSet(KEY_FAVORITE_VIDEO_IDS, favoriteVideoIds).apply()
    }

    private fun loadFavoriteVideoIds(): MutableSet<String> = getSharedPreferences(PREFERENCES_VIDEO_IDS,
        Context.MODE_PRIVATE).getStringSet(KEY_FAVORITE_VIDEO_IDS, mutableSetOf())?.toMutableSet() ?: mutableSetOf()


    inner class RecyclerViewAdapter(private val context: Context,
                                    private val favoriteVideos: List<VideoModel>? = null,
                                    private val favoriteVideoIds: MutableSet<String>)
        : RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder>() {

        private lateinit var recyclerView: RecyclerView
        private var videos = mutableListOf<VideoModel>()

        init {
            if (favoriteVideos == null) {
                if (dataUpdate) {
                    setVideosByPlaylistId()
                    println("$TAG: videos are loaded from the playlist id, dataUpdate is true")
                } else {
                    if (stringOfVideoIds.isBlank()) {
                        setVideosByPlaylistId()
                        println("$TAG: videos are loaded from the playlist id, stringOfVideoIds is empty")
                    }
                    else {
                        setVideosByIds(stringOfVideoIds)
                        println("$TAG: videos are loaded from the existing video ids")
                    }
                }
            }
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
                startYouTubePlayerActivity(context, video.id, PLAY_SINGLE_VIDEO)
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

                            if (videoIds != null) {
                                stringOfVideoIds = videoIds.joinToString(separator = ",")
                                saveStringOfVideoIds(stringOfVideoIds)
                                setVideosByIds(stringOfVideoIds)
                            } else {
                                showToast(getString(R.string.failed_to_load_videos))
                            }
                        } catch (e: Exception) {
                            showToast(getString(R.string.failed_to_load_videos))
                            e.printStackTrace()
                        }
                    } else {
                        showToast(getString(R.string.failed_to_load_videos))
                        println("${response.body?.string()}")
                    }
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
                            allVideos = videos as ArrayList<VideoModel>
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

    @Suppress("unused")
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
        private const val PREFERENCES_VIDEO_IDS = "preferences_video_ids"
        private const val KEY_FAVORITE_VIDEO_IDS = "key_favorite_video_ids"
        private const val KEY_STRING_OF_VIDEO_IDS = "key_string_of_video_ids"

        const val ACTION_YOUTUBE_PLAYER = "main.activity.action.youtube.player"
        const val KEY_VIDEO_ID = "key_video_id"
        const val KEY_PLAY_OPTIONS = "key_play_options"
        
        const val PLAY_SINGLE_VIDEO = 0
        const val PLAY_ALL_VIDEOS = 1
        const val PLAY_WATCHLIST_VIDEOS = 2

        var allVideos = ArrayList<VideoModel>()
        var favoriteVideoIds = mutableSetOf<String>()
    }
}
