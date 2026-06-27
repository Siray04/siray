package com.example.ui

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.database.Playlist
import com.example.database.Song
import com.example.player.RepeatMode
import com.example.player.AudioPlayerManager
import com.example.ui.theme.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MusicPlayerApp(viewModel: MainViewModel) {
    val context = LocalContext.current
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    
    // Splash screen state
    var showSplash by remember { mutableStateOf(true) }
    
    // Now playing expanded state
    var isNowPlayingExpanded by remember { mutableStateOf(false) }

    // Equalizer dialog state
    var showEqDialog by remember { mutableStateOf(false) }

    // Sleep timer dialog state
    var showTimerDialog by remember { mutableStateOf(false) }

    // Playlist creator state
    var showPlaylistCreator by remember { mutableStateOf(false) }

    val currentSong by viewModel.currentSong.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val sleepTimerMinutes by viewModel.sleepTimerMinutes.collectAsStateWithLifecycle()

    val coroutineScope = rememberCoroutineScope()

    // Slide-up Offset for Swipe controls on NowPlayingSheet
    var swipeOffsetY by remember { mutableStateOf(0f) }

    // Splash Timer transition
    LaunchedEffect(Unit) {
        delay(2500) // Beautiful 2.5s splash
        showSplash = false
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Shared Glassmorphic Background with flowing glowing circles
        AnimatedGlassBackground()

        if (showSplash) {
            SplashScreen()
        } else {
            // MAIN ROOT LAYOUT
            Scaffold(
                containerColor = Color.Transparent, // Let the animated background shine through!
                bottomBar = {
                    Column {
                        // MINI PLAYER (only visible when a song is loaded and Now Playing is not full screen)
                        if (currentSong != null && !isNowPlayingExpanded) {
                            MiniPlayer(
                                song = currentSong!!,
                                isPlaying = isPlaying,
                                onPlayPauseClick = { viewModel.togglePlayPause() },
                                onPrevClick = { viewModel.previous() },
                                onNextClick = { viewModel.next() },
                                onSwipeUp = { isNowPlayingExpanded = true },
                                onClick = { isNowPlayingExpanded = true }
                            )
                        }

                        // STANDARD MATERIAL 3 NAVIGATION BAR
                        NavigationBar(
                            containerColor = AeroSurfaceGlass.copy(alpha = 0.9f),
                            modifier = Modifier
                                .border(
                                    width = 1.dp,
                                    brush = Brush.verticalGradient(
                                        listOf(Color.White.copy(alpha = 0.1f), Color.Transparent)
                                    ),
                                    shape = RoundedCornerShape(0.dp)
                                )
                                .windowInsetsPadding(WindowInsets.navigationBars)
                        ) {
                            val tabs = listOf(
                                NavigationTabItem("Home", "home", Icons.Default.Home, Icons.Outlined.Home),
                                NavigationTabItem("Search", "search", Icons.Default.Search, Icons.Outlined.Search),
                                NavigationTabItem("Library", "library", Icons.Default.LibraryMusic, Icons.Outlined.LibraryMusic),
                                NavigationTabItem("FX & Equalizer", "settings", Icons.Default.Equalizer, Icons.Outlined.Equalizer)
                            )

                            tabs.forEach { tab ->
                                val selected = currentTab == tab.id
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { viewModel.setTab(tab.id) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = tab.label,
                                            tint = if (selected) AeroCyan else AeroTextSecondary
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = tab.label,
                                            color = if (selected) AeroTextPrimary else AeroTextSecondary,
                                            fontSize = 11.sp,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        indicatorColor = Color.White.copy(alpha = 0.08f)
                                    )
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    // MAIN BODY SCREEN CONTENT
                    when (currentTab) {
                        "home" -> HomeScreen(viewModel, onExpandPlayer = { isNowPlayingExpanded = true })
                        "search" -> SearchScreen(viewModel)
                        "library" -> LibraryScreen(
                            viewModel = viewModel,
                            onCreatePlaylistClick = { showPlaylistCreator = true }
                        )
                        "settings" -> SettingsScreen(
                            viewModel = viewModel,
                            onShowEqDialog = { showEqDialog = true },
                            onShowSleepTimer = { showTimerDialog = true }
                        )
                    }
                }
            }
        }

        // FULL SCREEN NOW PLAYING SHEET (Slide up glassmorphism panel)
        AnimatedVisibility(
            visible = isNowPlayingExpanded && !showSplash,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessLow)
            ),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessLow)
            )
        ) {
            if (currentSong != null) {
                NowPlayingSheet(
                    viewModel = viewModel,
                    song = currentSong!!,
                    isPlaying = isPlaying,
                    sleepTimerMin = sleepTimerMinutes,
                    onCollapse = { isNowPlayingExpanded = false },
                    onShowEq = { showEqDialog = true },
                    onShowTimer = { showTimerDialog = true }
                )
            }
        }

        // IN-APP FULL DETAILED EQUALIZER FLOATING MODAL SHEET
        if (showEqDialog) {
            EqualizerSheet(
                viewModel = viewModel,
                onDismiss = { showEqDialog = false }
            )
        }

        // SLEEP TIMER SELECTION DIALOG
        if (showTimerDialog) {
            SleepTimerDialog(
                currentMinutes = sleepTimerMinutes,
                onMinutesSelected = { viewModel.setSleepTimer(it) },
                onDismissRequest = { showTimerDialog = false }
            )
        }

        // PLAYLIST GENERATOR CREATOR DIALOG
        if (showPlaylistCreator) {
            PlaylistCreatorDialog(
                onCreate = { name, desc ->
                    viewModel.createPlaylist(name, desc)
                    showPlaylistCreator = false
                    Toast.makeText(context, "Playlist '$name' created!", Toast.LENGTH_SHORT).show()
                },
                onDismiss = { showPlaylistCreator = false }
            )
        }
    }
}

private data class NavigationTabItem(
    val label: String,
    val id: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

// 1. SPLASH SCREEN (Glowing, elegant logo animation)

@Composable
fun SplashScreen() {
    val infiniteTransition = rememberInfiniteTransition(label = "SplashLogoTransition")
    
    // Pulse animation for the logo
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "logo_scale"
    )

    // Glow orbit translation
    val glowOffset by infiniteTransition.animateFloat(
        initialValue = -20f,
        targetValue = 20f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "glow_x"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(AeroBackground, Color(0xFF0C1019), AeroBackground)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Glowing Background Aura
        Box(
            modifier = Modifier
                .size(240.dp)
                .offset(x = glowOffset.dp, y = (-glowOffset).dp)
                .blur(60.dp)
                .clip(CircleShape)
                .background(AeroCyan.copy(alpha = 0.2f))
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(110.dp * scale)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(AeroCyan, AeroTeal, AeroAccentPink)
                        )
                    )
                    .border(2.dp, Color.White.copy(alpha = 0.4f), CircleShape)
                    .shadow(15.dp, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.MusicNote,
                    contentDescription = "Siray Music logo",
                    modifier = Modifier.size(52.dp),
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "SIRAY MUSIC",
                fontSize = 28.sp,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Black,
                letterSpacing = 8.sp,
                color = AeroTextPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "HIGH-FIDELITY OFFLINE PLAYBACK",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                color = AeroCyan
            )
            
            Spacer(modifier = Modifier.height(48.dp))

            CircularProgressIndicator(
                color = AeroCyan,
                strokeWidth = 3.dp,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

// 2. HOME SCREEN (Recently played, favorites, sliding cards)

@Composable
fun HomeScreen(viewModel: MainViewModel, onExpandPlayer: () -> Unit) {
    val recentlyPlayed by viewModel.recentlyPlayed.collectAsStateWithLifecycle()
    val favoriteSongs by viewModel.favoriteSongs.collectAsStateWithLifecycle()
    val recentlyAdded by viewModel.recentlyAdded.collectAsStateWithLifecycle()
    val allSongs by viewModel.allSongs.collectAsStateWithLifecycle()
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Welcome Header with Dynamic Greeting
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Welcome Back",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = AeroTextPrimary
                )
                Text(
                    text = "Ready for high-quality audio",
                    fontSize = 13.sp,
                    color = AeroTextSecondary
                )
            }

            // Quick Scan Button
            IconButton(
                onClick = { viewModel.scanAndSeedFiles() },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.08f))
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Rescan database",
                    tint = AeroCyan,
                    modifier = Modifier.rotate(if (isScanning) 180f else 0f)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // HERO CAROUSEL: RECOMMENDED TRACKS
        if (allSongs.isNotEmpty()) {
            Text(
                text = "Featured Offline Audio",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = AeroTextPrimary,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(end = 16.dp)
            ) {
                items(allSongs.take(4)) { song ->
                    HeroTrackCard(song = song) {
                        viewModel.playTrack(song, allSongs)
                        onExpandPlayer()
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // SECTION: RECENTLY PLAYED
        if (recentlyPlayed.isNotEmpty()) {
            HomeSectionHeader("Recently Played")
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(recentlyPlayed) { song ->
                    CompactAlbumCard(song = song) {
                        viewModel.playTrack(song, recentlyPlayed)
                        onExpandPlayer()
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // SECTION: FAVORITES
        HomeSectionHeader("Your Favorites")
        if (favoriteSongs.isEmpty()) {
            EmptySectionPlaceholder(
                icon = Icons.Outlined.FavoriteBorder,
                text = "No favorites yet. Double-tap or heart songs to save them offline!"
            )
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(favoriteSongs) { song ->
                    CompactAlbumCard(song = song) {
                        viewModel.playTrack(song, favoriteSongs)
                        onExpandPlayer()
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // SECTION: RECENTLY ADDED
        if (recentlyAdded.isNotEmpty()) {
            HomeSectionHeader("Recently Added")
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                recentlyAdded.take(5).forEach { song ->
                    HorizontalSongItem(song = song, isFavorite = song.isFavorite, onFavoriteClick = {
                        viewModel.toggleSongFavorite(song)
                    }) {
                        viewModel.playTrack(song, recentlyAdded)
                        onExpandPlayer()
                    }
                }
            }
        }
    }
}

@Composable
fun HomeSectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        color = AeroTextPrimary,
        modifier = Modifier.padding(bottom = 14.dp)
    )
}

@Composable
fun HeroTrackCard(song: Song, onClick: () -> Unit) {
    val gradientBrush = Brush.linearGradient(
        colors = when (song.genre) {
            "Ambient" -> listOf(AeroCyan, AeroTeal)
            "Synthwave" -> listOf(AeroAccentPink, Color(0xFF673AB7))
            "Lofi" -> listOf(Color(0xFFFF8A65), Color(0xFF8D6E63))
            else -> listOf(AeroTeal, AeroCyan)
        }
    )

    Box(
        modifier = Modifier
            .width(220.dp)
            .height(130.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(gradientBrush)
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.2f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = song.genre,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = "Play",
                    tint = Color.White,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.25f))
                        .padding(4.dp)
                )
            }

            Column {
                Text(
                    text = song.title,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.artist,
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun CompactAlbumCard(song: Song, onClick: () -> Unit) {
    val artGradient = Brush.radialGradient(
        colors = when (song.genre) {
            "Ambient" -> listOf(AeroCyan, AeroBackground)
            "Synthwave" -> listOf(AeroAccentPink, AeroBackground)
            "Lofi" -> listOf(Color(0xFFFF8A65), AeroBackground)
            else -> listOf(AeroTeal, AeroBackground)
        }
    )

    Column(
        modifier = Modifier
            .width(110.dp)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(110.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(artGradient)
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.MusicNote,
                contentDescription = "Music",
                modifier = Modifier.size(36.dp),
                tint = Color.White.copy(alpha = 0.6f)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = song.title,
            color = AeroTextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = song.artist,
            color = AeroTextSecondary,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun EmptySectionPlaceholder(icon: ImageVector, text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.03f))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = "Empty",
                tint = AeroTextSecondary.copy(alpha = 0.5f),
                modifier = Modifier.size(36.dp)
            )
            Text(
                text = text,
                color = AeroTextSecondary,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(0.85f)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HorizontalSongItem(
    song: Song,
    isFavorite: Boolean,
    onFavoriteClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.03f))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Art placeholder
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF202A3D), Color(0xFF0F1524))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.MusicNote,
                contentDescription = "Music note",
                tint = AeroCyan.copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Titles
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = song.title,
                color = AeroTextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${song.artist} • ${song.album}",
                color = AeroTextSecondary,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Action details
        Text(
            text = "${song.bitrate}k ${song.codec}",
            color = AeroCyan.copy(alpha = 0.5f),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        IconButton(onClick = onFavoriteClick) {
            Icon(
                imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                contentDescription = "Favorite",
                tint = if (isFavorite) AeroAccentPink else AeroTextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// 3. MUSIC LIBRARY TAB (List/Grid of Songs, Custom Playlists)

@Composable
fun LibraryScreen(
    viewModel: MainViewModel,
    onCreatePlaylistClick: () -> Unit
) {
    val allSongs by viewModel.allSongs.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val selectedPlaylistId by viewModel.currentPlaylistId.collectAsStateWithLifecycle()
    val playlistSongs by viewModel.currentPlaylistSongs.collectAsStateWithLifecycle()

    var activeSubTab by remember { mutableStateOf("songs") } // "songs", "playlists"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        if (selectedPlaylistId != null) {
            // DETAILED PLAYLIST SCREEN
            val activePlaylist = playlists.find { it.id == selectedPlaylistId }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.selectPlaylist(null) }) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = AeroTextPrimary)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = activePlaylist?.name ?: "Playlist",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = AeroTextPrimary
                    )
                    Text(
                        text = activePlaylist?.description ?: "Offline collection",
                        fontSize = 12.sp,
                        color = AeroTextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (playlistSongs.isEmpty()) {
                EmptySectionPlaceholder(
                    icon = Icons.Outlined.QueueMusic,
                    text = "This playlist is empty. Go to Songs, long-press any track to add it here!"
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(playlistSongs) { song ->
                        HorizontalSongItem(
                            song = song,
                            isFavorite = song.isFavorite,
                            onFavoriteClick = { viewModel.toggleSongFavorite(song) },
                            onLongClick = {
                                viewModel.removeSongFromPlaylist(selectedPlaylistId!!, song.id)
                            }
                        ) {
                            viewModel.playTrack(song, playlistSongs)
                        }
                    }
                }
            }
        } else {
            // ROOT LIBRARY VIEW
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Music Library",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = AeroTextPrimary
                )

                if (activeSubTab == "playlists") {
                    GlassmorphicButton(
                        onClick = onCreatePlaylistClick,
                        icon = Icons.Default.Add,
                        text = "New"
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sub Tab Selectors
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.04f))
                    .padding(4.dp)
            ) {
                val subTabs = listOf("songs" to "Songs", "playlists" to "Playlists")
                subTabs.forEach { (id, label) ->
                    val active = activeSubTab == id
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (active) AeroCyan.copy(alpha = 0.15f) else Color.Transparent)
                            .clickable { activeSubTab = id }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (active) AeroCyan else AeroTextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (activeSubTab) {
                "songs" -> {
                    if (allSongs.isEmpty()) {
                        EmptySectionPlaceholder(
                            icon = Icons.Outlined.MusicNote,
                            text = "No offline audio files found on device. Tap Rescan in the top right of the Home screen to seed default synth audio!"
                        )
                    } else {
                        var songToAddToPlaylist by remember { mutableStateOf<Song?>(null) }

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(allSongs) { song ->
                                HorizontalSongItem(
                                    song = song,
                                    isFavorite = song.isFavorite,
                                    onFavoriteClick = { viewModel.toggleSongFavorite(song) },
                                    onLongClick = {
                                        songToAddToPlaylist = song
                                    }
                                ) {
                                    viewModel.playTrack(song, allSongs)
                                }
                            }
                        }

                        // Add to Playlist Selection Dialog
                        if (songToAddToPlaylist != null) {
                            AddToPlaylistDialog(
                                playlists = playlists,
                                onPlaylistSelected = { pId ->
                                    viewModel.addSongToPlaylist(pId, songToAddToPlaylist!!.id)
                                    songToAddToPlaylist = null
                                },
                                onDismiss = { songToAddToPlaylist = null }
                            )
                        }
                    }
                }

                "playlists" -> {
                    if (playlists.isEmpty()) {
                        EmptySectionPlaceholder(
                            icon = Icons.Outlined.QueueMusic,
                            text = "No playlists created yet. Create a beautiful customized offline playlist now!"
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(playlists) { playlist ->
                                PlaylistRowItem(
                                    playlist = playlist,
                                    onDeleteClick = { viewModel.deletePlaylist(playlist) }
                                ) {
                                    viewModel.selectPlaylist(playlist.id)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlaylistRowItem(
    playlist: Playlist,
    onDeleteClick: () -> Unit,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.03f))
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Art
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(AeroTeal, AeroCyan)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.QueueMusic,
                contentDescription = "Playlist",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = playlist.name,
                color = AeroTextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = playlist.description ?: "Custom local collection",
                color = AeroTextSecondary,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        IconButton(onClick = onDeleteClick) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete Playlist",
                tint = AeroTextSecondary.copy(alpha = 0.5f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

// PLAYLIST CREATOR DIALOG

@Composable
fun PlaylistCreatorDialog(
    onCreate: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onCreate(name, desc) },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = AeroCyan, contentColor = AeroBackground),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = AeroTextSecondary)
            }
        },
        title = { Text("Create Playlist", color = AeroTextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Playlist Name") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = AeroTextPrimary,
                        unfocusedTextColor = AeroTextPrimary,
                        focusedBorderColor = AeroCyan,
                        focusedLabelColor = AeroCyan
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Description (Optional)") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = AeroTextPrimary,
                        unfocusedTextColor = AeroTextPrimary,
                        focusedBorderColor = AeroCyan,
                        focusedLabelColor = AeroCyan
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        containerColor = AeroSurface,
        shape = RoundedCornerShape(20.dp)
    )
}

// ADD TO PLAYLIST SELECTION DIALOG

@Composable
fun AddToPlaylistDialog(
    playlists: List<Playlist>,
    onPlaylistSelected: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = AeroTextSecondary)
            }
        },
        title = { Text("Add to Playlist", color = AeroTextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            if (playlists.isEmpty()) {
                Text("Please create a playlist first in the Library tab.", color = AeroTextSecondary)
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.heightIn(max = 240.dp)
                ) {
                    items(playlists) { playlist ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onPlaylistSelected(playlist.id) }
                                .padding(12.dp)
                        ) {
                            Text(playlist.name, color = AeroTextPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        containerColor = AeroSurface,
        shape = RoundedCornerShape(20.dp)
    )
}

// 4. INSTANT SEARCH SCREEN (Instant filter, history, suggestions)

@Composable
fun SearchScreen(viewModel: MainViewModel) {
    val allSongs by viewModel.allSongs.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val searchHistory by viewModel.searchHistory.collectAsStateWithLifecycle()

    // Filter results immediately
    val searchResults = remember(searchQuery, allSongs) {
        if (searchQuery.isBlank()) {
            emptyList()
        } else {
            allSongs.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                        it.artist.contains(searchQuery, ignoreCase = true) ||
                        it.album.contains(searchQuery, ignoreCase = true) ||
                        it.genre.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Search Songs",
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            color = AeroTextPrimary
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Large Search Text Field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.setSearchQuery(it) },
            placeholder = { Text("Song, artist, album, genre...", color = AeroTextSecondary) },
            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = AeroCyan) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                        Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear", tint = AeroTextSecondary)
                    }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = {
                viewModel.addSearchToHistory(searchQuery)
            }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = AeroTextPrimary,
                unfocusedTextColor = AeroTextPrimary,
                focusedBorderColor = AeroCyan,
                unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                focusedContainerColor = Color.White.copy(alpha = 0.03f),
                unfocusedContainerColor = Color.White.copy(alpha = 0.03f)
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(20.dp))

        if (searchQuery.isBlank()) {
            // SHOW SEARCH HISTORY
            if (searchHistory.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Recent Searches", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AeroTextPrimary)
                    TextButton(onClick = { viewModel.clearSearchHistory() }) {
                        Text("Clear All", color = AeroAccentPink, fontSize = 12.sp)
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(searchHistory) { historyItem ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setSearchQuery(historyItem)
                                    viewModel.addSearchToHistory(historyItem)
                                }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.History, contentDescription = "History", tint = AeroTextSecondary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(historyItem, color = AeroTextPrimary, fontSize = 14.sp)
                            }
                            Icon(imageVector = Icons.Default.ArrowOutward, contentDescription = "Suggest", tint = AeroTextSecondary.copy(alpha = 0.4f), modifier = Modifier.size(14.dp))
                        }
                    }
                }
            } else {
                EmptySectionPlaceholder(
                    icon = Icons.Default.Search,
                    text = "Search offline songs with high-fidelity indexing!"
                )
            }
        } else {
            // SHOW SEARCH RESULTS
            if (searchResults.isEmpty()) {
                EmptySectionPlaceholder(
                    icon = Icons.Outlined.SearchOff,
                    text = "No results found for '$searchQuery'. Try checking spelling or scanning database."
                )
            } else {
                Text(
                    text = "${searchResults.size} matches found",
                    color = AeroCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(searchResults) { song ->
                        HorizontalSongItem(
                            song = song,
                            isFavorite = song.isFavorite,
                            onFavoriteClick = { viewModel.toggleSongFavorite(song) }
                        ) {
                            viewModel.playTrack(song, searchResults)
                            viewModel.addSearchToHistory(searchQuery)
                        }
                    }
                }
            }
        }
    }
}

// 5. SETTINGS SCREEN (EQ sliders, sleep timer, cache info, preset lists)

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onShowEqDialog: () -> Unit,
    onShowSleepTimer: () -> Unit
) {
    val eqEnabled by viewModel.eqEnabled.collectAsStateWithLifecycle()
    val eqPreset by viewModel.eqPreset.collectAsStateWithLifecycle()
    val sleepTimerRemaining by viewModel.sleepTimerMinutes.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "DSP & Preferences",
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            color = AeroTextPrimary
        )

        Spacer(modifier = Modifier.height(20.dp))

        // SECTION: AUDIO CONTROL CARDS
        Text("Audio Tuning Engine", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AeroCyan)
        Spacer(modifier = Modifier.height(10.dp))

        // EQUALIZER QUICK CONTROL
        GlassmorphicCard(
            modifier = Modifier.fillMaxWidth(),
            onClick = onShowEqDialog
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Equalizer, contentDescription = "Equalizer", tint = AeroCyan, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("10-Band Equalizer", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = AeroTextPrimary)
                        Text(if (eqEnabled) "Preset active: $eqPreset" else "EQ Disabled", fontSize = 11.sp, color = AeroTextSecondary)
                    }
                }
                
                Switch(
                    checked = eqEnabled,
                    onCheckedChange = { viewModel.toggleEq(it) },
                    colors = SwitchDefaults.colors(checkedThumbColor = AeroCyan, checkedTrackColor = AeroCyan.copy(alpha = 0.3f))
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // SLEEP TIMER CONTROL
        GlassmorphicCard(
            modifier = Modifier.fillMaxWidth(),
            onClick = onShowSleepTimer
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Timer, contentDescription = "Sleep timer", tint = AeroCyan, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Sleep Countdown Timer", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = AeroTextPrimary)
                        Text(
                            text = if (sleepTimerRemaining > 0) "$sleepTimerRemaining min remaining" else "Disabled",
                            fontSize = 11.sp,
                            color = AeroTextSecondary
                        )
                    }
                }
                Icon(imageVector = Icons.Default.KeyboardArrowRight, contentDescription = "Open", tint = AeroTextSecondary)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // APP PREFERENCES
        Text("Device Storage & Cache", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AeroCyan)
        Spacer(modifier = Modifier.height(10.dp))

        GlassmorphicCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Offline Media Cache", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AeroTextPrimary)
                    Text("Stores synthetic waveforms & covers", fontSize = 11.sp, color = AeroTextSecondary)
                }
                Text("2.4 MB Used", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AeroCyan)
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Gapless Crossfade (Transition)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AeroTextPrimary)
                    Text("Fades out ending tracks", fontSize = 11.sp, color = AeroTextSecondary)
                }
                Switch(
                    checked = true,
                    onCheckedChange = {},
                    colors = SwitchDefaults.colors(checkedThumbColor = AeroCyan, checkedTrackColor = AeroCyan.copy(alpha = 0.3f))
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ABOUT INFORMATION
        GlassmorphicCard(modifier = Modifier.fillMaxWidth()) {
            Text("Siray Music Premium", fontSize = 16.sp, fontWeight = FontWeight.Black, color = AeroCyan)
            Text("v1.0.0 Stable (Official Release)", fontSize = 11.sp, color = AeroTextSecondary)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "A premium offline high-fidelity audio experience engineered with fully integrated 10-band hardware equalizers, bass boosters, and synchronized LRC lyrics parsers.",
                fontSize = 12.sp,
                color = AeroTextSecondary,
                lineHeight = 18.sp
            )
        }
    }
}

// 6. MINI PLAYER LAYER (Docked above Bottom Bar)

@Composable
fun MiniPlayer(
    song: Song,
    isPlaying: Boolean,
    onPlayPauseClick: () -> Unit,
    onPrevClick: () -> Unit,
    onNextClick: () -> Unit,
    onSwipeUp: () -> Unit,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(AeroSurfaceGlass.copy(alpha = 0.95f))
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = 0.12f), Color.Transparent)
                ),
                shape = RoundedCornerShape(0.dp)
            )
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    if (dragAmount.y < -15) { // Significant swipe up
                        onSwipeUp()
                    }
                }
            }
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Rotating disc art
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(colors = listOf(AeroCyan, AeroAccentPink))
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.MusicNote,
                contentDescription = "MiniArt",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Titles
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = song.title,
                color = AeroTextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song.artist,
                color = AeroTextSecondary,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Action Buttons
        IconButton(onClick = onPrevClick) {
            Icon(imageVector = Icons.Filled.SkipPrevious, contentDescription = "Prev", tint = AeroTextPrimary, modifier = Modifier.size(24.dp))
        }

        IconButton(
            onClick = onPlayPauseClick,
            modifier = Modifier
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.08f))
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = "PlayPause",
                tint = AeroCyan,
                modifier = Modifier.size(26.dp)
            )
        }

        IconButton(onClick = onNextClick) {
            Icon(imageVector = Icons.Filled.SkipNext, contentDescription = "Next", tint = AeroTextPrimary, modifier = Modifier.size(24.dp))
        }
    }
}

// 7. FULL SCREEN NOW PLAYING GLASS PANEL SHEET

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NowPlayingSheet(
    viewModel: MainViewModel,
    song: Song,
    isPlaying: Boolean,
    sleepTimerMin: Int,
    onCollapse: () -> Unit,
    onShowEq: () -> Unit,
    onShowTimer: () -> Unit
) {
    val currentPosition by viewModel.currentPosition.collectAsStateWithLifecycle()
    val duration by viewModel.duration.collectAsStateWithLifecycle()
    val playbackSpeed by viewModel.playbackSpeed.collectAsStateWithLifecycle()
    val isShuffleEnabled by viewModel.isShuffleEnabled.collectAsStateWithLifecycle()
    val repeatMode by viewModel.repeatMode.collectAsStateWithLifecycle()
    val currentQueue by viewModel.currentQueue.collectAsStateWithLifecycle()

    var showLyricsPanel by remember { mutableStateOf(false) }
    var showQueuePanel by remember { mutableStateOf(false) }

    // Shifting color scheme background inside glass sheet
    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xEE0A0F1D),
            Color(0xF90A0D15)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // HEADER BAR
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onCollapse) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = "Minimize",
                        tint = AeroTextPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "NOW PLAYING",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        color = AeroCyan
                    )
                    Text(
                        text = song.album,
                        fontSize = 13.sp,
                        color = AeroTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 200.dp)
                    )
                }

                IconButton(onClick = { viewModel.toggleSongFavorite(song) }) {
                    Icon(
                        imageVector = if (song.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (song.isFavorite) AeroAccentPink else AeroTextPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(0.1f))

            if (showLyricsPanel) {
                // DISK REPLACED WITH SCROLLING LYRICS PANEL
                LyricsPanel(
                    viewModel = viewModel,
                    currentPosition = currentPosition,
                    onToggleBack = { showLyricsPanel = false }
                )
            } else if (showQueuePanel) {
                // DISK REPLACED WITH QUEUE REORDER LIST
                QueuePanel(
                    viewModel = viewModel,
                    currentQueue = currentQueue,
                    onToggleBack = { showQueuePanel = false }
                )
            } else {
                // ROTATING DISC ARTWORK WITH GLOW
                VinylDisk(
                    isPlaying = isPlaying,
                    song = song,
                    sizeDp = 250.dp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // MUSIC WAVE VISUALIZATION ACCENTS
                MusicWaveVisualizer(
                    isPlaying = isPlaying,
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .height(36.dp)
                )
            }

            Spacer(modifier = Modifier.weight(0.1f))

            // SONG LABELS (Artist, title)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = song.title,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Black,
                    color = AeroTextPrimary,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(0.85f)
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = song.artist,
                    fontSize = 14.sp,
                    color = AeroCyan,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // TIMELINE PROGRESS SLIDER
            val progressRatio = if (duration > 0) currentPosition.toFloat() / duration else 0f
            
            Column(modifier = Modifier.fillMaxWidth()) {
                Slider(
                    value = progressRatio,
                    onValueChange = { ratio ->
                        viewModel.seekTo((ratio * duration).toLong())
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = AeroCyan,
                        activeTrackColor = AeroCyan,
                        inactiveTrackColor = Color.White.copy(alpha = 0.1f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatTime(currentPosition),
                        fontSize = 11.sp,
                        color = AeroTextSecondary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = formatTime(duration),
                        fontSize = 11.sp,
                        color = AeroTextSecondary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // PLAYBACK CONTROLS (Row)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Shuffle Button
                IconButton(onClick = { viewModel.toggleShuffle() }) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (isShuffleEnabled) AeroCyan else AeroTextSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Prev Button
                IconButton(onClick = { viewModel.previous() }) {
                    Icon(
                        imageVector = Icons.Filled.SkipPrevious,
                        contentDescription = "Previous",
                        tint = AeroTextPrimary,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Play / Pause Circle Accent Button
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(AeroCyan, AeroTeal)
                            )
                        )
                        .clickable { viewModel.togglePlayPause() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = AeroBackground,
                        modifier = Modifier.size(38.dp)
                    )
                }

                // Next Button
                IconButton(onClick = { viewModel.next() }) {
                    Icon(
                        imageVector = Icons.Filled.SkipNext,
                        contentDescription = "Next",
                        tint = AeroTextPrimary,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Repeat Mode Button
                IconButton(onClick = { viewModel.cycleRepeatMode() }) {
                    val repeatIcon = when (repeatMode) {
                        RepeatMode.NONE -> Icons.Default.Repeat
                        RepeatMode.ALL -> Icons.Default.Repeat
                        RepeatMode.ONE -> Icons.Default.RepeatOne
                    }
                    val repeatColor = if (repeatMode != RepeatMode.NONE) AeroCyan else AeroTextSecondary
                    Icon(
                        imageVector = repeatIcon,
                        contentDescription = "Repeat",
                        tint = repeatColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // FOOTER CONTROL ACCESSORIES (EQ, Speed, Sleep, Lyrics, Queue triggers)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.04f))
                    .padding(vertical = 10.dp, horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Equalizer Trigger
                IconButton(onClick = onShowEq) {
                    Icon(imageVector = Icons.Default.Equalizer, contentDescription = "Equalizer", tint = AeroTextSecondary, modifier = Modifier.size(20.dp))
                }

                // Sleep Timer Trigger
                IconButton(onClick = onShowTimer) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = "Sleep timer",
                        tint = if (sleepTimerMin > 0) AeroCyan else AeroTextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Speed Selector Slider
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(imageVector = Icons.Default.Speed, contentDescription = "Speed", tint = AeroTextSecondary, modifier = Modifier.size(16.dp))
                    Text(
                        text = "${String.format("%.1f", playbackSpeed)}x",
                        color = AeroCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable {
                            val nextSpeed = when (playbackSpeed) {
                                1.0f -> 1.2f
                                1.2f -> 1.5f
                                1.5f -> 2.0f
                                2.0f -> 0.8f
                                else -> 1.0f
                            }
                            viewModel.setPlaybackSpeed(nextSpeed)
                        }
                    )
                }

                // Show Lyrics Toggle
                IconButton(onClick = {
                    showQueuePanel = false
                    showLyricsPanel = !showLyricsPanel
                }) {
                    Icon(
                        imageVector = Icons.Filled.Lyrics,
                        contentDescription = "Lyrics",
                        tint = if (showLyricsPanel) AeroCyan else AeroTextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Show Queue Toggle
                IconButton(onClick = {
                    showLyricsPanel = false
                    showQueuePanel = !showQueuePanel
                }) {
                    Icon(
                        imageVector = Icons.Filled.QueueMusic,
                        contentDescription = "Queue",
                        tint = if (showQueuePanel) AeroCyan else AeroTextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

// 8. HIGH-FIDELITY SYNCHRONIZED SCROLLING LYRICS INTERFACE

@Composable
fun LyricsPanel(
    viewModel: MainViewModel,
    currentPosition: Long,
    onToggleBack: () -> Unit
) {
    val lyrics by viewModel.lyrics.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    
    // Find active lyric line index
    val activeIndex = remember(lyrics, currentPosition) {
        lyrics.indexOfLast { currentPosition >= it.timeMs }
    }

    // Auto scroll to active lyrics line beautifully
    LaunchedEffect(activeIndex) {
        if (activeIndex != -1 && lyrics.isNotEmpty()) {
            listState.animateScrollToItem(
                index = (activeIndex - 2).coerceAtLeast(0),
                scrollOffset = 0
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color.Black.copy(alpha = 0.25f))
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        if (lyrics.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No synchronized lyrics file loaded.", color = AeroTextSecondary, fontSize = 12.sp)
            }
        } else {
            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(lyrics) { index, line ->
                    val isActive = index == activeIndex
                    val color = if (isActive) AeroCyan else AeroTextSecondary.copy(alpha = 0.6f)
                    val scale = if (isActive) 1.15f else 0.95f
                    val weight = if (isActive) FontWeight.Black else FontWeight.Normal

                    Text(
                        text = line.text,
                        color = color,
                        fontSize = (15 * scale).sp,
                        fontWeight = weight,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .clickable {
                                viewModel.seekTo(line.timeMs)
                            }
                    )
                }
            }
        }

        // Mini Back Button Overlay
        IconButton(
            onClick = onToggleBack,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(24.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.1f))
        ) {
            Icon(imageVector = Icons.Default.Close, contentDescription = "Close lyrics", tint = Color.White, modifier = Modifier.size(14.dp))
        }
    }
}

// 9. LIVE PLAYBACK QUEUE MANAGER LIST

@Composable
fun QueuePanel(
    viewModel: MainViewModel,
    currentQueue: List<Song>,
    onToggleBack: () -> Unit
) {
    val currentIndex by viewModel.currentIndex.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color.Black.copy(alpha = 0.25f))
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(20.dp))
            .padding(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Upcoming Queue (${currentQueue.size} songs)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = AeroCyan
                )

                IconButton(
                    onClick = onToggleBack,
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.1f))
                ) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close queue", tint = Color.White, modifier = Modifier.size(14.dp))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (currentQueue.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No upcoming tracks in queue.", color = AeroTextSecondary, fontSize = 12.sp)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(currentQueue) { index, song ->
                        val isCurrent = index == currentIndex
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isCurrent) AeroCyan.copy(alpha = 0.08f) else Color.Transparent)
                                .clickable {
                                    viewModel.playTrack(song, currentQueue)
                                }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${index + 1}",
                                color = if (isCurrent) AeroCyan else AeroTextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(20.dp)
                            )

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = song.title,
                                    color = if (isCurrent) AeroCyan else AeroTextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = song.artist,
                                    color = AeroTextSecondary,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            IconButton(
                                onClick = { AudioPlayerManager.removeFromQueue(song.id) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.RemoveCircleOutline,
                                    contentDescription = "Remove",
                                    tint = AeroAccentPink.copy(alpha = 0.6f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// 10. IN-APP 10-BAND GRAPHIC EQUALIZER CONTROL PANEL

@Composable
fun EqualizerSheet(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val eqEnabled by viewModel.eqEnabled.collectAsStateWithLifecycle()
    val eqBands by viewModel.eqBands.collectAsStateWithLifecycle()
    val eqPreset by viewModel.eqPreset.collectAsStateWithLifecycle()
    val bassBoostStrength by viewModel.bassBoostStrength.collectAsStateWithLifecycle()
    val virtualizerStrength by viewModel.virtualizerStrength.collectAsStateWithLifecycle()

    val presets = listOf("Normal", "Rock", "Pop", "Jazz", "Hip Hop", "Dance", "Classic", "Electronic", "Bass Booster", "Vocal Booster")

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = AeroCyan, contentColor = AeroBackground),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Apply")
            }
        },
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Equalizer Engine", color = AeroTextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Enable", color = AeroTextSecondary, fontSize = 12.sp, modifier = Modifier.padding(end = 6.dp))
                    Switch(
                        checked = eqEnabled,
                        onCheckedChange = { viewModel.toggleEq(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = AeroCyan)
                    )
                }
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Preset List Selector
                Text("Audio Presets", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AeroCyan)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(presets) { preset ->
                        val selected = eqPreset == preset
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selected) AeroCyan.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.04f))
                                .clickable { viewModel.applyPreset(preset) }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = preset,
                                color = if (selected) AeroCyan else AeroTextSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Divider(color = Color.White.copy(alpha = 0.05f))

                // Graphic Slider Bands (5 / 10 Bands dependent on array)
                Text("Decibel Frequency Tuner", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AeroCyan)
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val bandLabels = listOf("60Hz", "230Hz", "910Hz", "4kHz", "14kHz")
                    eqBands.forEachIndexed { index, gain ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        ) {
                            Text(
                                text = if (gain > 0) "+$gain" else "$gain",
                                color = if (eqEnabled) AeroCyan else AeroTextSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Slider(
                                value = (gain + 15).toFloat() / 30f,
                                onValueChange = { ratio ->
                                    val newGain = (ratio * 30 - 15).roundToInt()
                                    viewModel.setEqBandGain(index, newGain)
                                },
                                enabled = eqEnabled,
                                modifier = Modifier
                                    .weight(1f)
                                    .graphicsLayer {
                                        rotationZ = -90f
                                    },
                                colors = SliderDefaults.colors(
                                    thumbColor = AeroCyan,
                                    activeTrackColor = AeroCyan,
                                    inactiveTrackColor = Color.White.copy(alpha = 0.05f)
                                )
                            )

                            Text(
                                text = bandLabels.getOrElse(index) { "B$index" },
                                color = AeroTextSecondary,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Divider(color = Color.White.copy(alpha = 0.05f))

                // Hardware DSP Accents: Bass & 3D Virtualization
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Acoustic DSP Drivers", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AeroCyan)
                    
                    // Bass boost Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Sub-Bass Driver", fontSize = 12.sp, color = AeroTextSecondary, modifier = Modifier.width(90.dp))
                        Slider(
                            value = bassBoostStrength.toFloat() / 1000f,
                            onValueChange = { ratio ->
                                viewModel.setBassBoost((ratio * 1000).toInt())
                            },
                            enabled = eqEnabled,
                            colors = SliderDefaults.colors(thumbColor = AeroCyan, activeTrackColor = AeroCyan),
                            modifier = Modifier.weight(1f)
                        )
                        Text("${(bassBoostStrength / 10)}%", color = AeroTextPrimary, fontSize = 11.sp, modifier = Modifier.width(30.dp), textAlign = TextAlign.End)
                    }

                    // 3D Spatial Audio Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("3D Spatial Driver", fontSize = 12.sp, color = AeroTextSecondary, modifier = Modifier.width(90.dp))
                        Slider(
                            value = virtualizerStrength.toFloat() / 1000f,
                            onValueChange = { ratio ->
                                viewModel.setVirtualizer((ratio * 1000).toInt())
                            },
                            enabled = eqEnabled,
                            colors = SliderDefaults.colors(thumbColor = AeroCyan, activeTrackColor = AeroCyan),
                            modifier = Modifier.weight(1f)
                        )
                        Text("${(virtualizerStrength / 10)}%", color = AeroTextPrimary, fontSize = 11.sp, modifier = Modifier.width(30.dp), textAlign = TextAlign.End)
                    }
                }
            }
        },
        containerColor = AeroSurface,
        shape = RoundedCornerShape(24.dp)
    )
}

// TIMELINE FORMATTING UTILS

private fun formatTime(ms: Long): String {
    val totalSec = ms / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    return String.format("%02d:%02d", min, sec)
}
