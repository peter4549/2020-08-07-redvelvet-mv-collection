package com.grand.duke.elliot.kim.kotlin.redvelvetmvcollection

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_favorite_videos.view.*

class FavoriteVideosFragment: Fragment() {

    private lateinit var videos: List<VideoModel>

    fun setVideos(videos: List<VideoModel>) {
        this.videos = videos
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_favorite_videos, container, false)

        if (videos.isEmpty())
            view.text_view_empty.visibility = View.VISIBLE
        else
            view.text_view_empty.visibility = View.GONE

        view.recycler_view.apply {
            adapter = MainActivity().RecyclerViewAdapter(videos, (requireActivity() as MainActivity).favoriteVideoIds)
            layoutManager = MainActivity.GridLayoutManagerWrapper(requireContext(), 1)
        }

        return view
    }
}