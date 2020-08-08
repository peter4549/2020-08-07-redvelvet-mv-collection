package com.grand.duke.elliot.kim.kotlin.redvelvetmvcollection

import android.os.Bundle
import android.view.*
import android.widget.Toast
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

        (requireActivity() as MainActivity).setSupportActionBar(view.toolbar)
        (requireActivity() as MainActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)
        view.toolbar.title = requireContext().getString(R.string.watchlist)
        setHasOptionsMenu(true)

        if (videos.isEmpty())
            view.text_view_empty.visibility = View.VISIBLE
        else
            view.text_view_empty.visibility = View.GONE

        view.recycler_view.apply {
            adapter = MainActivity().RecyclerViewAdapter(requireContext(),
                videos, MainActivity.favoriteVideoIds)
            layoutManager = MainActivity.GridLayoutManagerWrapper(requireContext(), 1)
        }

        return view
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        menu.clear()
        (requireActivity() as MainActivity).menuInflater.inflate(
            R.menu.menu_favorite_videos, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            android.R.id.home -> requireActivity().onBackPressed()
            R.id.item_favorites_auto_play -> {
                if (MainActivity.favoriteVideoIds.isNotEmpty())
                (requireActivity() as MainActivity)
                    .startYouTubePlayerActivity(
                        requireContext(),
                        null,
                        MainActivity.PLAY_WATCHLIST_VIDEOS
                    )
                else
                    Toast.makeText(requireContext(),
                        getString(R.string.no_videos), Toast.LENGTH_LONG).show()
            }
        }

        return super.onOptionsItemSelected(item)
    }
}