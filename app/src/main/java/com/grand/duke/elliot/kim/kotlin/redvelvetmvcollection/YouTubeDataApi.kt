package com.grand.duke.elliot.kim.kotlin.redvelvetmvcollection

import com.google.gson.Gson
import com.google.gson.internal.LinkedTreeMap
import okhttp3.Request
import okhttp3.Response

class YouTubeDataApi {

    fun getVideosRequestByPlaylistId(): Request {
        val url = "https://www.googleapis.com/youtube/v3/playlistItems?" +
                "key=$GOOGLE_API_KEY&part=contentDetails&playlistId=$PLAYLIST_ID&" +
                "maxResults=50"
        return Request.Builder().url(url).get().build()
    }

    fun getVideosRequestByIds(videoIds: String): Request {
        val url = "https://www.googleapis.com/youtube/v3/videos?" +
                "key=$GOOGLE_API_KEY&part=snippet,statistics&id=$videoIds"
        return Request.Builder().url(url).get().build()
    }

    fun getVideoIdsFromResponse(response: Response): List<String>? {
        val map: Map<*, *>? = Gson().fromJson(response.body?.string(), Map::class.java)
        val items = map?.get("items") as ArrayList<*>?

        return items?.map {
            ((it as LinkedTreeMap<*, *>)["contentDetails"]
                    as LinkedTreeMap<*, *>)["videoId"] as String
        }
    }

    fun getVideosFromResponse(response: Response): MutableList<VideoModel> {
        val map: Map<*, *>? =
            Gson().fromJson(response.body?.string(), Map::class.java)
        val items = map?.get("items") as ArrayList<*>
        val videos = mutableListOf<VideoModel>()

        for (item in items) {
            val id = (item as LinkedTreeMap<*, *>)["id"] as String
            val snippet = item["snippet"] as LinkedTreeMap<*, *>
            val title = snippet["title"] as String
            val thumbnails = snippet["thumbnails"] as LinkedTreeMap<*, *>
            val thumbnailUri = (thumbnails["standard"] as LinkedTreeMap<*, *>)["url"] as String

            videos.add(0, VideoModel(id, title, thumbnailUri))
        }

        return videos
    }

    companion object {
        private const val GOOGLE_API_KEY = "AIzaSyAPRGTlWTegKl02ieX7Qj9Om75u9Aq5goE"
        private const val PLAYLIST_ID = "PLjmCcnD_RNHMX3PLSyNmweL8I5ElSj6vM"
    }
}