package com.example.player

import android.content.Context
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.database.MusicRepository
import com.example.database.Song
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.*

enum class RepeatMode {
    NONE, ALL, ONE
}

object AudioPlayerManager {
    private const val TAG = "AudioPlayerManager"

    private var mediaPlayer: MediaPlayer? = null
    private var repository: MusicRepository? = null
    private var context: Context? = null
    
    // Coroutine Scope for background operations
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var progressJob: Job? = null
    private var sleepTimerJob: Job? = null

    // Audio Effects
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null

    // Reactive State Flows
    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _isShuffleEnabled = MutableStateFlow(false)
    val isShuffleEnabled: StateFlow<Boolean> = _isShuffleEnabled.asStateFlow()

    private val _repeatMode = MutableStateFlow(RepeatMode.ALL)
    val repeatMode: StateFlow<RepeatMode> = _repeatMode.asStateFlow()

    private val _queue = MutableStateFlow<List<Song>>(emptyList())
    val queue: StateFlow<List<Song>> = _queue.asStateFlow()

    private val _currentIndex = MutableStateFlow(-1)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    // FX State Flows (Equalizer 5/10 bands, Bass, 3D)
    private val _eqEnabled = MutableStateFlow(false)
    val eqEnabled: StateFlow<Boolean> = _eqEnabled.asStateFlow()

    private val _bassBoostStrength = MutableStateFlow(0) // 0 to 1000
    val bassBoostStrength: StateFlow<Int> = _bassBoostStrength.asStateFlow()

    private val _virtualizerStrength = MutableStateFlow(0) // 0 to 1000
    val virtualizerStrength: StateFlow<Int> = _virtualizerStrength.asStateFlow()

    private val _eqBands = MutableStateFlow<List<Int>>(listOf(0, 0, 0, 0, 0)) // Decibel gains (-15 to 15)
    val eqBands: StateFlow<List<Int>> = _eqBands.asStateFlow()

    private val _eqPreset = MutableStateFlow("Normal")
    val eqPreset: StateFlow<String> = _eqPreset.asStateFlow()

    private val _sleepTimerMinutes = MutableStateFlow(0) // 0 = disabled
    val sleepTimerMinutes: StateFlow<Int> = _sleepTimerMinutes.asStateFlow()

    // Transition crossfade state
    private val _isCrossfadeEnabled = MutableStateFlow(true)
    val isCrossfadeEnabled: StateFlow<Boolean> = _isCrossfadeEnabled.asStateFlow()

    fun initialize(ctx: Context, repo: MusicRepository) {
        context = ctx.applicationContext
        repository = repo
        setupMediaPlayer()
    }

    private fun setupMediaPlayer() {
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer().apply {
                setOnCompletionListener {
                    handleSongCompletion()
                }
                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "MediaPlayer error: what=$what, extra=$extra")
                    true
                }
            }
        }
    }

    private fun handleSongCompletion() {
        scope.launch {
            when (repeatMode.value) {
                RepeatMode.ONE -> {
                    // Loop current song
                    seekTo(0)
                    play()
                }
                RepeatMode.ALL -> {
                    next()
                }
                RepeatMode.NONE -> {
                    if (currentIndex.value < queue.value.lastIndex) {
                        next()
                    } else {
                        pause()
                        seekTo(0)
                    }
                }
            }
        }
    }

    // QUEUE MANAGEMENT

    fun setQueue(songs: List<Song>, startIndex: Int = 0) {
        _queue.value = songs
        _currentIndex.value = startIndex.coerceIn(-1, songs.lastIndex)
        if (startIndex >= 0 && startIndex < songs.size) {
            loadSong(songs[startIndex])
        }
    }

    fun addToQueueNext(song: Song) {
        val current = _queue.value.toMutableList()
        val index = _currentIndex.value
        if (index == -1) {
            current.add(song)
            setQueue(current, 0)
        } else {
            // Check if already in queue, remove it to move it next
            val existingIndex = current.indexOfFirst { it.id == song.id }
            if (existingIndex != -1) {
                current.removeAt(existingIndex)
            }
            // Insert after current index
            val insertPos = if (existingIndex != -1 && existingIndex < index) index else index + 1
            current.add(insertPos, song)
            _queue.value = current
        }
    }

    fun removeFromQueue(songId: Long) {
        val current = _queue.value.toMutableList()
        val removeIndex = current.indexOfFirst { it.id == songId }
        if (removeIndex != -1) {
            current.removeAt(removeIndex)
            val currentIdx = _currentIndex.value
            when {
                currentIdx == removeIndex -> {
                    _queue.value = current
                    if (current.isNotEmpty()) {
                        val newIdx = removeIndex.coerceIn(0, current.lastIndex)
                        _currentIndex.value = newIdx
                        loadSong(current[newIdx])
                    } else {
                        _currentIndex.value = -1
                        stop()
                    }
                }
                currentIdx > removeIndex -> {
                    _currentIndex.value = currentIdx - 1
                    _queue.value = current
                }
                else -> {
                    _queue.value = current
                }
            }
        }
    }

    fun reorderQueue(fromIndex: Int, toIndex: Int) {
        val current = _queue.value.toMutableList()
        if (fromIndex in current.indices && toIndex in current.indices) {
            val item = current.removeAt(fromIndex)
            current.add(toIndex, item)
            _queue.value = current
            
            // Adjust current index
            val currentIdx = _currentIndex.value
            if (currentIdx == fromIndex) {
                _currentIndex.value = toIndex
            } else if (currentIdx in (fromIndex + 1)..toIndex) {
                _currentIndex.value = currentIdx - 1
            } else if (currentIdx in toIndex..<fromIndex) {
                _currentIndex.value = currentIdx + 1
            }
        }
    }

    fun clearQueue() {
        stop()
        _queue.value = emptyList()
        _currentIndex.value = -1
        _currentSong.value = null
    }

    // PLAYBACK CONTROLS

    fun playSongDirectly(song: Song) {
        // Find if already in queue
        val index = _queue.value.indexOfFirst { it.id == song.id }
        if (index != -1) {
            _currentIndex.value = index
            loadSong(song, startImmediately = true)
        } else {
            // Set queue to single song or append
            val newQueue = _queue.value.toMutableList()
            newQueue.add(song)
            _queue.value = newQueue
            _currentIndex.value = newQueue.lastIndex
            loadSong(song, startImmediately = true)
        }
    }

    private fun loadSong(song: Song, startImmediately: Boolean = true) {
        setupMediaPlayer()
        progressJob?.cancel()
        _isPlaying.value = false

        try {
            mediaPlayer?.reset()
            mediaPlayer?.setDataSource(song.path)
            mediaPlayer?.prepare()
            
            _currentSong.value = song
            _duration.value = mediaPlayer?.duration?.toLong() ?: song.duration
            _currentPosition.value = 0L

            // Apply playback speed params
            applySpeed()

            // Initialize audio effects attached to this new session
            mediaPlayer?.audioSessionId?.let { sessionId ->
                setupAudioEffects(sessionId)
            }

            if (startImmediately) {
                play()
                // Log playback history in repository
                scope.launch {
                    repository?.addSongToHistory(song.id)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading song: ${song.title}", e)
            // Auto skip to next on error to maintain premium fluid UX
            next()
        }
    }

    fun play() {
        setupMediaPlayer()
        if (mediaPlayer != null && !mediaPlayer!!.isPlaying) {
            try {
                mediaPlayer?.start()
                _isPlaying.value = true
                startProgressTracker()
            } catch (e: Exception) {
                Log.e(TAG, "Error starting playback", e)
            }
        }
    }

    fun pause() {
        if (mediaPlayer != null && mediaPlayer!!.isPlaying) {
            try {
                mediaPlayer?.pause()
                _isPlaying.value = false
                progressJob?.cancel()
            } catch (e: Exception) {
                Log.e(TAG, "Error pausing playback", e)
            }
        }
    }

    fun togglePlayPause() {
        if (_isPlaying.value) {
            pause()
        } else {
            if (_currentSong.value == null && _queue.value.isNotEmpty()) {
                _currentIndex.value = 0
                loadSong(_queue.value[0], startImmediately = true)
            } else {
                play()
            }
        }
    }

    fun stop() {
        progressJob?.cancel()
        _isPlaying.value = false
        _currentPosition.value = 0L
        try {
            mediaPlayer?.stop()
            mediaPlayer?.reset()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping media player", e)
        }
        releaseAudioEffects()
    }

    fun next() {
        if (_queue.value.isEmpty()) return

        var nextIndex = _currentIndex.value + 1
        if (_isShuffleEnabled.value) {
            // Select random index other than current
            if (_queue.value.size > 1) {
                do {
                    nextIndex = Random().nextInt(_queue.value.size)
                } while (nextIndex == _currentIndex.value)
            } else {
                nextIndex = 0
            }
        } else if (nextIndex >= _queue.value.size) {
            nextIndex = if (repeatMode.value == RepeatMode.ALL) 0 else _queue.value.lastIndex
        }

        if (nextIndex in _queue.value.indices) {
            _currentIndex.value = nextIndex
            loadSong(_queue.value[nextIndex], startImmediately = true)
        }
    }

    fun previous() {
        if (_queue.value.isEmpty()) return

        // If current position is greater than 3 seconds, restart current song
        if (_currentPosition.value > 3000) {
            seekTo(0)
            return
        }

        var prevIndex = _currentIndex.value - 1
        if (_isShuffleEnabled.value) {
            if (_queue.value.size > 1) {
                do {
                    prevIndex = Random().nextInt(_queue.value.size)
                } while (prevIndex == _currentIndex.value)
            } else {
                prevIndex = 0
            }
        } else if (prevIndex < 0) {
            prevIndex = if (repeatMode.value == RepeatMode.ALL) _queue.value.lastIndex else 0
        }

        if (prevIndex in _queue.value.indices) {
            _currentIndex.value = prevIndex
            loadSong(_queue.value[prevIndex], startImmediately = true)
        }
    }

    fun seekTo(positionMs: Long) {
        try {
            mediaPlayer?.seekTo(positionMs.toInt())
            _currentPosition.value = positionMs
        } catch (e: Exception) {
            Log.e(TAG, "Error seeking to $positionMs", e)
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        _playbackSpeed.value = speed
        applySpeed()
    }

    private fun applySpeed() {
        try {
            mediaPlayer?.let { player ->
                if (player.isPlaying || _isPlaying.value) {
                    val params = PlaybackParams().apply {
                        this.speed = _playbackSpeed.value
                    }
                    player.playbackParams = params
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to apply speed params: ${e.message}")
        }
    }

    fun toggleShuffle() {
        _isShuffleEnabled.value = !_isShuffleEnabled.value
    }

    fun setRepeatMode(mode: RepeatMode) {
        _repeatMode.value = mode
    }

    // AUDIO EFFECTS & EQUALIZER CONTROLS

    private fun setupAudioEffects(audioSessionId: Int) {
        try {
            releaseAudioEffects()

            // Equalizer Setup
            equalizer = Equalizer(0, audioSessionId).apply {
                enabled = _eqEnabled.value
                // Synchronize sliders
                applyBandsToHardware()
            }

            // Bass Boost Setup
            bassBoost = BassBoost(0, audioSessionId).apply {
                enabled = _eqEnabled.value
                setStrength(_bassBoostStrength.value.toShort())
            }

            // Virtualizer Setup
            virtualizer = Virtualizer(0, audioSessionId).apply {
                enabled = _eqEnabled.value
                setStrength(_virtualizerStrength.value.toShort())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up audio effects", e)
        }
    }

    private fun releaseAudioEffects() {
        try {
            equalizer?.release()
            equalizer = null
            bassBoost?.release()
            bassBoost = null
            virtualizer?.release()
            virtualizer = null
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing audio effects", e)
        }
    }

    fun toggleEq(enabled: Boolean) {
        _eqEnabled.value = enabled
        try {
            equalizer?.enabled = enabled
            bassBoost?.enabled = enabled
            virtualizer?.enabled = enabled
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling eq", e)
        }
    }

    fun setBassBoost(strength: Int) {
        _bassBoostStrength.value = strength.coerceIn(0, 1000)
        try {
            bassBoost?.setStrength(_bassBoostStrength.value.toShort())
        } catch (e: Exception) {
            Log.w(TAG, "BassBoost not supported by device hardware")
        }
    }

    fun setVirtualizer(strength: Int) {
        _virtualizerStrength.value = strength.coerceIn(0, 1000)
        try {
            virtualizer?.setStrength(_virtualizerStrength.value.toShort())
        } catch (e: Exception) {
            Log.w(TAG, "Virtualizer not supported by device hardware")
        }
    }

    fun setEqBandGain(bandIndex: Int, gainDb: Int) {
        val current = _eqBands.value.toMutableList()
        if (bandIndex in current.indices) {
            current[bandIndex] = gainDb.coerceIn(-15, 15)
            _eqBands.value = current
            _eqPreset.value = "Custom"
            applyBandsToHardware()
        }
    }

    private fun applyBandsToHardware() {
        val bands = _eqBands.value
        try {
            equalizer?.let { eq ->
                val numBandsInHardware = eq.numberOfBands.toInt()
                for (i in 0 until numBandsInHardware.coerceAtMost(bands.size)) {
                    val gainMillibels = (bands[i] * 100).toShort()
                    eq.setBandLevel(i.toShort(), gainMillibels)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to apply EQ band level: ${e.message}")
        }
    }

    fun applyPreset(presetName: String) {
        _eqPreset.value = presetName
        val presetBands = when (presetName) {
            "Rock" -> listOf(4, 2, -1, 3, 5)
            "Pop" -> listOf(-2, 1, 3, 2, -1)
            "Jazz" -> listOf(3, 1, -2, 2, 3)
            "Hip Hop" -> listOf(5, 3, 0, 1, 3)
            "Dance" -> listOf(4, 0, 2, 3, 1)
            "Classic" -> listOf(2, 1, 0, 1, -2)
            "Electronic" -> listOf(4, 2, 0, 2, 4)
            "Bass Booster" -> listOf(6, 4, 0, 0, 0)
            "Vocal Booster" -> listOf(-2, -1, 2, 4, 2)
            else -> listOf(0, 0, 0, 0, 0) // Normal
        }
        _eqBands.value = presetBands
        applyBandsToHardware()
    }

    // SLEEP TIMER

    fun startSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        _sleepTimerMinutes.value = minutes
        if (minutes > 0) {
            sleepTimerJob = scope.launch {
                while (_sleepTimerMinutes.value > 0) {
                    delay(60 * 1000L)
                    _sleepTimerMinutes.value -= 1
                }
                pause() // Sleep timer elapsed, pause music nicely
            }
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        _sleepTimerMinutes.value = 0
    }

    // PROGRESS TRACKING

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                try {
                    mediaPlayer?.let { player ->
                        if (player.isPlaying) {
                            _currentPosition.value = player.currentPosition.toLong()
                        }
                    }
                } catch (e: Exception) {
                    // Ignore transient exceptions from MediaPlayer
                }
                delay(200L)
            }
        }
    }
}
