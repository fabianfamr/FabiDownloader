package com.fabian.downloader.network

import retrofit2.http.GET
import retrofit2.http.Query

interface ApiService {
    @GET("search")
    suspend fun searchVideos(@Query("q") query: String): List<VideoInfo>
}
