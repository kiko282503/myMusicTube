package com.musictube.player.service

import com.musictube.player.data.model.SearchResult
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Hilt singleton that holds the last search state so it survives SearchViewModel recreation
 * (e.g. when the user navigates away from search and comes back via the download banner).
 */
@Singleton
class SearchStateHolder @Inject constructor() {
    var lastQuery: String = ""
    var lastResults: List<SearchResult> = emptyList()
    var continuationToken: String? = null
    var canLoadMore: Boolean = true
}
