package com.example.database

import kotlinx.coroutines.flow.Flow

class MusicRepository(private val musicDao: MusicDao) {
    val allSongs: Flow<List<Song>> = musicDao.getAllSongs()
    val favoriteSongs: Flow<List<Song>> = musicDao.getFavoriteSongs()
    val recentlyPlayedSongs: Flow<List<Song>> = musicDao.getRecentlyPlayedSongs()
    val mostPlayedSongs: Flow<List<Song>> = musicDao.getMostPlayedSongs()
    val recentlyAddedSongs: Flow<List<Song>> = musicDao.getRecentlyAddedSongs()
    val allPlaylists: Flow<List<Playlist>> = musicDao.getAllPlaylists()

    suspend fun getSongById(id: Long): Song? = musicDao.getSongById(id)
    fun getSongByIdFlow(id: Long): Flow<Song?> = musicDao.getSongByIdFlow(id)

    suspend fun insertSong(song: Song): Long = musicDao.insertSong(song)
    suspend fun insertSongs(songs: List<Song>) = musicDao.insertSongs(songs)
    suspend fun updateSong(song: Song) = musicDao.updateSong(song)
    suspend fun deleteSongById(id: Long) = musicDao.deleteSongById(id)

    suspend fun insertPlaylist(playlist: Playlist): Long = musicDao.insertPlaylist(playlist)
    suspend fun updatePlaylist(playlist: Playlist) = musicDao.updatePlaylist(playlist)
    suspend fun deletePlaylist(playlist: Playlist) = musicDao.deletePlaylist(playlist)
    suspend fun getPlaylistById(id: Long): Playlist? = musicDao.getPlaylistById(id)

    suspend fun addSongToPlaylist(playlistId: Long, songId: Long) {
        musicDao.insertPlaylistSongCrossRef(PlaylistSongCrossRef(playlistId, songId))
    }

    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long) {
        musicDao.deletePlaylistSongCrossRef(playlistId, songId)
    }

    fun getSongsForPlaylist(playlistId: Long): Flow<List<Song>> = musicDao.getSongsForPlaylist(playlistId)

    suspend fun addSongToHistory(songId: Long) {
        // Record playback in history
        musicDao.insertPlaybackHistory(PlaybackHistory(songId = songId))
        // Increment play count and update last played time
        val song = musicDao.getSongById(songId)
        if (song != null) {
            musicDao.updateSong(
                song.copy(
                    playCount = song.playCount + 1,
                    lastPlayed = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun clearHistory() = musicDao.clearPlaybackHistory()
}
