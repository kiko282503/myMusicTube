package com.musictube.player.service

import com.musictube.player.data.model.SearchResult

/** Holds the last search state across ViewModel re-creation (singleton via Koin). */
class SearchStateHolder {
    var lastQuery: String = ""
    var lastResults: List<SearchResult> = emptyList()
    var continuationToken: String? = null
    var canLoadMore: Boolean = true
}
