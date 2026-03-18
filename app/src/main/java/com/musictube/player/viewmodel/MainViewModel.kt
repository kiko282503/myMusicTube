package com.musictube.player.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musictube.player.data.repository.MusicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val musicRepository: MusicRepository
) : ViewModel() {
    
    init {
        // Initialize any global app state here
    }
    
    fun scanForLocalMusic() {
        viewModelScope.launch {
            musicRepository.scanLocalMusic()
        }
    }
}