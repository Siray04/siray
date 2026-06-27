package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.database.AppDatabase
import com.example.database.MusicRepository
import com.example.database.Playlist
import com.example.database.Song
import com.example.player.AudioGenerator
import com.example.player.AudioPlayerManager
import com.example.player.RepeatMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class LyricLine(val timeMs: Long, val text: String)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = MusicRepository(database.musicDao())

    // App state
    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    // Search query
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Selected navigation screen
    private val _currentTab = MutableStateFlow("home") // "home", "library", "search", "settings"
    val currentTab: StateFlow<String> = _currentTab.asStateFlow()

    // Main lists from database
    val allSongs: StateFlow<List<Song>> = repository.allSongs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteSongs: StateFlow<List<Song>> = repository.favoriteSongs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentlyPlayed: StateFlow<List<Song>> = repository.recentlyPlayedSongs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val mostPlayed: StateFlow<List<Song>> = repository.mostPlayedSongs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentlyAdded: StateFlow<List<Song>> = repository.recentlyAddedSongs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val playlists: StateFlow<List<Playlist>> = repository.allPlaylists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active playlist detailed songs
    private val _currentPlaylistId = MutableStateFlow<Long?>(null)
    val currentPlaylistId: StateFlow<Long?> = _currentPlaylistId.asStateFlow()
    
    val currentPlaylistSongs: StateFlow<List<Song>> = _currentPlaylistId
        .flatMapLatest { id ->
            if (id != null) repository.getSongsForPlaylist(id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Player State forwarded from AudioPlayerManager
    val currentSong = AudioPlayerManager.currentSong
    val isPlaying = AudioPlayerManager.isPlaying
    val currentPosition = AudioPlayerManager.currentPosition
    val duration = AudioPlayerManager.duration
    val playbackSpeed = AudioPlayerManager.playbackSpeed
    val isShuffleEnabled = AudioPlayerManager.isShuffleEnabled
    val repeatMode = AudioPlayerManager.repeatMode
    val currentQueue = AudioPlayerManager.queue
    val currentIndex = AudioPlayerManager.currentIndex

    // FX State forwarded
    val eqEnabled = AudioPlayerManager.eqEnabled
    val eqBands = AudioPlayerManager.eqBands
    val eqPreset = AudioPlayerManager.eqPreset
    val bassBoostStrength = AudioPlayerManager.bassBoostStrength
    val virtualizerStrength = AudioPlayerManager.virtualizerStrength
    val sleepTimerMinutes = AudioPlayerManager.sleepTimerMinutes

    // Search History
    private val _searchHistory = MutableStateFlow<List<String>>(
        listOf("Luna Prism", "Celestia", "Lofi Rainfall", "Deep Meditation", "Synthwave")
    )
    val searchHistory: StateFlow<List<String>> = _searchHistory.asStateFlow()

    // Synchronized lyrics state
    private val _lyrics = MutableStateFlow<List<LyricLine>>(emptyList())
    val lyrics: StateFlow<List<LyricLine>> = _lyrics.asStateFlow()

    init {
        AudioPlayerManager.initialize(application, repository)
        
        // Scan/Seed files in background at startup
        scanAndSeedFiles()

        // Observe current song changes to parse its lyrics
        viewModelScope.launch {
            currentSong.collect { song ->
                if (song != null) {
                    val lyricsText = song.lyrics ?: getDefaultLyricsFor(song.title)
                    _lyrics.value = parseLrc(lyricsText)
                } else {
                    _lyrics.value = emptyList()
                }
            }
        }
    }

    fun setTab(tab: String) {
        _currentTab.value = tab
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun addSearchToHistory(query: String) {
        if (query.isBlank()) return
        val current = _searchHistory.value.toMutableList()
        current.remove(query)
        current.add(0, query)
        _searchHistory.value = current.take(10)
    }

    fun clearSearchHistory() {
        _searchHistory.value = emptyList()
    }

    fun selectPlaylist(playlistId: Long?) {
        _currentPlaylistId.value = playlistId
    }

    // AUDIO SCANNING & SEEDING

    fun scanAndSeedFiles() {
        viewModelScope.launch {
            _isScanning.value = true
            withContext(Dispatchers.IO) {
                // Generate and save premium synthesized loops to internal app storage
                val seedSongs = AudioGenerator.generateDefaultSongsIfMissing(getApplication())
                
                // Add default songs if they are missing in the Database
                for (song in seedSongs) {
                    // Check if already in db by title to avoid duplicates
                    val existing = database.musicDao().getAllSongs().firstOrNull()?.find { it.title == song.title }
                    if (existing == null) {
                        database.musicDao().insertSong(song)
                    }
                }

                // Create a few default playlists if none exist
                val existingPlaylists = database.musicDao().getAllPlaylists().first()
                if (existingPlaylists.isEmpty()) {
                    val p1 = Playlist(name = "Midnight Vibes", description = "Lush ambient pads for retro relaxation", isPinned = true)
                    val p2 = Playlist(name = "Cyber Plucks", description = "Synthwave driving beats for focusing", isPinned = false)
                    val id1 = database.musicDao().insertPlaylist(p1)
                    val id2 = database.musicDao().insertPlaylist(p2)

                    // Distribute songs into these playlists
                    val all = database.musicDao().getAllSongs().first()
                    all.find { it.genre == "Ambient" }?.let { database.musicDao().insertPlaylistSongCrossRef(com.example.database.PlaylistSongCrossRef(id1, it.id)) }
                    all.find { it.genre == "Lofi" }?.let { database.musicDao().insertPlaylistSongCrossRef(com.example.database.PlaylistSongCrossRef(id1, it.id)) }
                    all.find { it.genre == "Synthwave" }?.let { database.musicDao().insertPlaylistSongCrossRef(com.example.database.PlaylistSongCrossRef(id2, it.id)) }
                }

                // Give it a little delay for responsive scanning feel
                kotlinx.coroutines.delay(1000)
            }
            _isScanning.value = false
        }
    }

    // PLAYER BRIDGE METHODS

    fun playTrack(song: Song, contextList: List<Song>) {
        val index = contextList.indexOfFirst { it.id == song.id }
        AudioPlayerManager.setQueue(contextList, if (index != -1) index else 0)
    }

    fun togglePlayPause() = AudioPlayerManager.togglePlayPause()
    fun next() = AudioPlayerManager.next()
    fun previous() = AudioPlayerManager.previous()
    fun seekTo(positionMs: Long) = AudioPlayerManager.seekTo(positionMs)
    fun setPlaybackSpeed(speed: Float) = AudioPlayerManager.setPlaybackSpeed(speed)
    fun toggleShuffle() = AudioPlayerManager.toggleShuffle()
    fun cycleRepeatMode() {
        val current = repeatMode.value
        val nextMode = when (current) {
            RepeatMode.NONE -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.NONE
        }
        AudioPlayerManager.setRepeatMode(nextMode)
    }

    fun toggleSongFavorite(song: Song) {
        viewModelScope.launch {
            val updated = song.copy(isFavorite = !song.isFavorite)
            repository.updateSong(updated)
            // Update active song reference if it's currently playing
            if (currentSong.value?.id == song.id) {
                // Trigger reload state of current song inside the manager
                AudioPlayerManager.currentSong.value?.let {
                    // Update favoritism state reactively
                    (AudioPlayerManager.currentSong as MutableStateFlow).value = updated
                }
            }
        }
    }

    // PLAYLIST METHODS

    fun createPlaylist(name: String, description: String? = null) {
        viewModelScope.launch {
            repository.insertPlaylist(Playlist(name = name, description = description))
        }
    }

    fun updatePlaylist(playlist: Playlist) {
        viewModelScope.launch {
            repository.updatePlaylist(playlist)
        }
    }

    fun deletePlaylist(playlist: Playlist) {
        viewModelScope.launch {
            if (_currentPlaylistId.value == playlist.id) {
                _currentPlaylistId.value = null
            }
            repository.deletePlaylist(playlist)
        }
    }

    fun addSongToPlaylist(playlistId: Long, songId: Long) {
        viewModelScope.launch {
            repository.addSongToPlaylist(playlistId, songId)
        }
    }

    fun removeSongFromPlaylist(playlistId: Long, songId: Long) {
        viewModelScope.launch {
            repository.removeSongFromPlaylist(playlistId, songId)
        }
    }

    // FX CONTROLS

    fun toggleEq(enabled: Boolean) = AudioPlayerManager.toggleEq(enabled)
    fun setBassBoost(strength: Int) = AudioPlayerManager.setBassBoost(strength)
    fun setVirtualizer(strength: Int) = AudioPlayerManager.setVirtualizer(strength)
    fun setEqBandGain(band: Int, gain: Int) = AudioPlayerManager.setEqBandGain(band, gain)
    fun applyPreset(presetName: String) = AudioPlayerManager.applyPreset(presetName)
    fun setSleepTimer(minutes: Int) = AudioPlayerManager.startSleepTimer(minutes)
    fun cancelSleepTimer() = AudioPlayerManager.cancelSleepTimer()

    // LYRICS UTILS

    private fun parseLrc(lrcText: String): List<LyricLine> {
        val linesList = mutableListOf<LyricLine>()
        val lines = lrcText.split("\n")
        val regex = Regex("\\[(\\d{2}):(\\d{2})\\.(\\d{2})](.*)")
        for (line in lines) {
            val match = regex.find(line.trim())
            if (match != null) {
                val min = match.groupValues[1].toLong()
                val sec = match.groupValues[2].toLong()
                val ms = match.groupValues[3].toLong() * 10
                val text = match.groupValues[4].trim()
                val timeMs = (min * 60 * 1000) + (sec * 1000) + ms
                linesList.add(LyricLine(timeMs, text))
            } else if (line.isNotBlank()) {
                // If line contains metadata like [ar: Luna Prism], ignore or add at 0
                if (!line.startsWith("[")) {
                    linesList.add(LyricLine(0, line.trim()))
                }
            }
        }
        return linesList.sortedBy { it.timeMs }
    }

    private fun getDefaultLyricsFor(title: String): String {
        return when (title) {
            "Ethereal Dreamscape" -> """
                [00:00.00]Ethereal Dreamscape
                [00:01.00]Written and Synthesized by Luna Prism
                [00:03.00]Entering a world of deep cosmic resonance...
                [00:05.50]Float away into the starry cosmos
                [00:08.00]Where waves of light surround you
                [00:11.00]Experience the gravity-free resonance
                [00:14.50]Quiet thoughts drift across the void
                [00:17.50]As the synthesizer sweeps the sky
                [00:21.00]A dream that never ends
                [00:25.00]Celestia shines bright tonight
            """.trimIndent()
            
            "Neon Cyberpunk" -> """
                [00:00.00]Neon Cyberpunk
                [00:01.00]Beats composed by Vector Grid
                [00:02.50]Initializing network connectivity...
                [00:04.00]Driving through the electronic grid
                [00:06.50]In the shade of holographic towers
                [00:09.00]Laser signals flashing in the rain
                [00:11.50]Synthesizer plucks drive the vehicle forward
                [00:14.00]Accelerating past the speed of light
                [00:17.00]A hyper-connected future is here
                [00:20.50]Vector signals crossing lines
                [00:23.50]System online: 100% active
            """.trimIndent()
            
            "Lofi Rainfall" -> """
                [00:00.00]Lofi Rainfall
                [00:01.00]Mellow tones by Cozy Chill
                [00:03.00]Relaxing sound of raindrops on glass
                [00:06.00]Sip a warm cup of coffee
                [00:09.00]Let the vintage piano notes soothe your mind
                [00:12.50]Raindrops keep calling
                [00:15.00]Warm blanket, calm room
                [00:18.00]Time slows down to a gentle halt
                [00:21.00]A perfect night for writing code
                [00:25.00]Sleeping under the cloudy sky
            """.trimIndent()
            
            "Cosmic Frequency" -> """
                [00:00.00]Cosmic Frequency
                [00:01.50]Frequency alignment by Solfeggio 528
                [00:04.00]Tune into the Solfeggio 528Hz frequency
                [00:07.50]Binaural beats align the hemisphere
                [00:11.00]Deep grounding 174Hz sub-bass hums
                [00:14.50]Like an ocean wave washing away tension
                [00:18.00]Inhale serenity, exhale clutter
                [00:22.00]Mental clarity in the theta state
                [00:26.00]Harmony restored, cell by cell
            """.trimIndent()

            "Aero Sunrise" -> """
                [00:00.00]Aero Sunrise
                [00:01.00]Organic acoustic loops by Sola
                [00:03.50]Golden hour rays shine through
                [00:06.00]A fresh morning breeze sweeps the aura
                [00:08.50]Acoustic plucks rising with the sun
                [00:11.50]Warmth spreads across the horizon
                [00:14.50]Shedding the night, embracing the day
                [00:18.00]A beautiful journey starts today
                [00:21.50]Every second is a brand new start
                [00:25.00]The sunrise aura surrounds us
            """.trimIndent()
            
            else -> """
                [00:00.00]$title
                [00:02.00]Enjoy this beautiful offline sound
                [00:04.50]Siray Music: High-Fidelity Music Engine
                [00:08.00]Providing absolute offline peace
                [00:12.00]Smooth, seamless gapless playback
            """.trimIndent()
        }
    }
}
