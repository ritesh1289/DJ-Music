package com.example.auramusic

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.*
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.PlaybackException
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

// --- 1. DATA MODELS ---
data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val imageUrl: Any,
    val songUrl: String
)

val internetSongs = listOf(
    Song("1", "Airtel Phonk", "Mashuq Haque", R.drawable.airtel_phonk, "https://docs.google.com/uc?export=download&id=1h6jt6Mge4PVcVD8ZDSsaOF-YYVAGev3_"),
    Song("2", "Alone Pt-II", "Alan Walker", R.drawable.alone_pt2, "https://docs.google.com/uc?export=download&id=17vid_BGl7AyulpZwy3dSGJIWbJBL6wvf"),
    Song("3", "Believer", "DubstepGutter", R.drawable.beleiver, "https://docs.google.com/uc?export=download&id=1j7PQ8XmPeO6MajiX5HYEIjGjWki7UQDn"),
    Song("4", "Dandelions", "Ruth B.", R.drawable.dandelions, "https://docs.google.com/uc?export=download&id=1BPbhERa2uVn2KKBNuaftBGeLx8zlejcM"),
    Song("5","Everything","Diamond Eyes",R.drawable.everything,"https://docs.google.com/uc?export=download&id=1T3cSZDZmFeJqiogBqqmnOjZovbzkIaAP"),
    Song("6","Give Me Promiscuous","Moody Melodies",R.drawable.give_me_promiscous,"https://docs.google.com/uc?export=download&id=1M5rTvAKRk4sMJV1zwUwNLRZVcD7fIs7D"),
    Song("7","Gurenge","LiSA",R.drawable.gurenge,"https://docs.google.com/uc?export=download&id=117OMvBxXyyjrarAJ2jUXHEJQiqCBYyNR"),
    Song("8","Heat Waves","Glass Animals",R.drawable.heat_waves,"https://docs.google.com/uc?export=download&id=16mMEFgA09ktLPUAKHgS7M-VVlbZg_Yyy"),
    Song("9","High","JPB, Aleesia",R.drawable.high,"https://docs.google.com/uc?export=download&id=1jD3xCAzcBfg2UDA7-K7yHDFYAGK9OBjK"),
    Song("10","Infinity","Jaymes Young",R.drawable.infinity,"https://docs.google.com/uc?export=download&id=1i0tOfPRt3ZDRWuIQh5m3KLu3X1SKOvUD"),
    Song("11","Legends Never Die","League of Legends, Against the Current",R.drawable.legends_never_die,"https://docs.google.com/uc?export=download&id=1Xdlx0ETOpdRnfN4JjOgAWahVO23g2mAq"),
    Song("12","MATUSHKA ULTRAPHONK","satirin, FUNK DEMON, EVO",R.drawable.matushka,"https://docs.google.com/uc?export=download&id=17hXdR4JvX5NQT6zPl8q7ZCgvU3BrcUJ5"),
    Song("13","MIDU ECHOING JUMPSTYLE","TWXNY, KPHK, Innxcence",R.drawable.midu_echoing_jumpstyle,"https://docs.google.com/uc?export=download&id=1Zx_xzetabqWCs8RcavXYz-0CdPuIo0oh"),
    Song("14","One Kiss X I Was Never There","TikTokOnly",R.drawable.one_kiss,"https://docs.google.com/uc?export=download&id=1SMQEMzx6Rux_NiIzgYin9z29XjmgFsQJ"),
    Song("15","Replay","Iyaz",R.drawable.replay,"https://docs.google.com/uc?export=download&id=1uGZaoU_JwLHhHMMKHrOo3PMocw0i5Zh1"),
)

// --- 2. VIEWMODEL ---
class MusicViewModel(application: Application) : AndroidViewModel(application) {

    private val exoPlayer: ExoPlayer = ExoPlayer.Builder(application)
        .setAudioAttributes(AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(), true)
        .build()

    var currentSong by mutableStateOf<Song?>(null)
    var isPlaying by mutableStateOf(false)
    var currentPosition by mutableStateOf(0L)
    var totalDuration by mutableStateOf(0L)

    // LIBRARY STATE
    val likedSongs = mutableStateListOf<Song>()
    private var currentSongIndex = -1

    init {
        exoPlayer.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) { isPlaying = false }
            override fun onIsPlayingChanged(playing: Boolean) { isPlaying = playing }
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) totalDuration = exoPlayer.duration.coerceAtLeast(0L)
            }
        })

        viewModelScope.launch {
            while (isActive) {
                if (isPlaying) currentPosition = exoPlayer.currentPosition
                delay(500L)
            }
        }
    }

    fun playSong(song: Song) {
        currentSong = song
        currentSongIndex = internetSongs.indexOf(song)
        val mediaItem = MediaItem.fromUri(song.songUrl)
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.play()
    }

    fun toggleLike(song: Song) {
        if (likedSongs.contains(song)) likedSongs.remove(song) else likedSongs.add(song)
    }

    fun seekTo(position: Float) {
        currentPosition = position.toLong()
        exoPlayer.seekTo(position.toLong())
    }

    fun playNext() {
        if (internetSongs.isNotEmpty()) {
            currentSongIndex = (currentSongIndex + 1) % internetSongs.size
            playSong(internetSongs[currentSongIndex])
        }
    }

    fun playPrevious() {
        if (internetSongs.isNotEmpty()) {
            currentSongIndex = if (currentSongIndex <= 0) internetSongs.size - 1 else currentSongIndex - 1
            playSong(internetSongs[currentSongIndex])
        }
    }

    fun togglePlayPause() { if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play() }

    fun formatTime(ms: Long): String {
        val totalSeconds = (ms / 1000).coerceAtLeast(0L)
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%d:%02d", minutes, seconds)
    }

    override fun onCleared() { super.onCleared(); exoPlayer.release() }
}

// --- 3. MAIN ACTIVITY ---
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

        super.onCreate(savedInstanceState)

        val sharedPref = getSharedPreferences("aura_prefs", Context.MODE_PRIVATE)
        val isLoggedIn = sharedPref.getBoolean("is_logged_in", false)

        setContent { AuraMusicApp(isLoggedIn) }
    }
}

@Composable
fun AuraMusicApp(alreadyLoggedIn: Boolean) {
    val musicViewModel: MusicViewModel = viewModel()
    val navController = rememberNavController()
    val sharedPref = LocalContext.current.getSharedPreferences("aura_prefs", Context.MODE_PRIVATE)
    val startDest = if (alreadyLoggedIn) "main_app" else "signup"

    Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF121212)) {
        NavHost(navController = navController, startDestination = startDest) {
            composable("signup") {
                SignupScreen(
                    onSignupSuccess = { name, email, pass ->
                        sharedPref.edit().putString("user_name", name).putString("email", email).putString("pass", pass).apply()
                        navController.navigate("login")
                    },
                    onNavigateToLogin = { navController.navigate("login") }
                )
            }
            composable("login") {
                val savedEmail = sharedPref.getString("email", "") ?: ""
                val savedPass = sharedPref.getString("pass", "") ?: ""
                LoginScreen(
                    registeredEmail = savedEmail, registeredPassword = savedPass,
                    onLoginSuccess = {
                        sharedPref.edit().putBoolean("is_logged_in", true).apply()
                        navController.navigate("main_app") { popUpTo("login") { inclusive = true } }
                    },
                    onNavigateToSignup = { navController.navigate("signup") }
                )
            }
            composable("main_app") {
                MainScreenShell(musicViewModel = musicViewModel, onLogoutRequest = {
                    sharedPref.edit().putBoolean("is_logged_in", false).apply()
                    navController.navigate("login") { popUpTo("main_app") { inclusive = true } }
                })
            }
        }
    }
}

// --- 4. AUTH SCREENS ---
@Composable
fun SignupScreen(onSignupSuccess: (String, String, String) -> Unit, onNavigateToLogin: () -> Unit) {
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    val fieldColors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedLabelColor = Color(0xFF1DB954), unfocusedLabelColor = Color.LightGray, focusedBorderColor = Color(0xFF1DB954), unfocusedBorderColor = Color.Gray)

    Column(modifier = Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Join DJ", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(30.dp))
        OutlinedTextField(value = fullName, onValueChange = { fullName = it }, label = { Text("Full Name") }, modifier = Modifier.fillMaxWidth(), colors = fieldColors)
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth(), colors = fieldColors, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = password, onValueChange = { password = it }, label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(), colors = fieldColors,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = { IconButton(onClick = { passwordVisible = !passwordVisible }) { Icon(imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, contentDescription = null, tint = Color.Gray) } }
        )
        Button(onClick = { if (fullName.isNotEmpty() && email.isNotEmpty()) onSignupSuccess(fullName, email, password) }, modifier = Modifier.fillMaxWidth().padding(top = 32.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1DB954))) { Text("Create Account", color = Color.Black) }
        TextButton(onClick = onNavigateToLogin) { Text("Already have an account? Log in", color = Color.Gray) }
    }
}

@Composable
fun LoginScreen(registeredEmail: String, registeredPassword: String, onLoginSuccess: () -> Unit, onNavigateToSignup: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    val fieldColors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedLabelColor = Color(0xFF1DB954), unfocusedLabelColor = Color.LightGray, focusedBorderColor = Color(0xFF1DB954), unfocusedBorderColor = Color.Gray)

    Column(modifier = Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("DJ Music", color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(40.dp))
        OutlinedTextField(value = email, onValueChange = { email = it; errorMessage = "" }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth(), colors = fieldColors,  keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = password, onValueChange = { password = it; errorMessage = "" }, label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(), colors = fieldColors,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = { IconButton(onClick = { passwordVisible = !passwordVisible }) { Icon(imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, contentDescription = null, tint = Color.Gray) } }
        )
        if (errorMessage.isNotEmpty()) Text(errorMessage, color = Color.Red)
        Button(onClick = { if (email == registeredEmail && password == registeredPassword) onLoginSuccess() else errorMessage = "Wrong login!" }, modifier = Modifier.fillMaxWidth().padding(top = 24.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1DB954))) { Text("Log In", color = Color.Black) }
        TextButton(onClick = onNavigateToSignup) { Text("Sign up", color = Color.Gray) }
    }
}

// --- 5. MAIN SHELL ---
@Composable
fun MainScreenShell(musicViewModel: MusicViewModel, onLogoutRequest: () -> Unit) {
    val bottomNavController = rememberNavController()
    var showFullPlayer by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            bottomBar = {
                Column {
                    MiniPlayer(musicViewModel, onOpen = { showFullPlayer = true })
                    BottomNavigationBar(bottomNavController)
                }
            },
            containerColor = Color(0xFF121212)
        ) { p ->
            Box(Modifier.padding(p)) {
                NavHost(navController = bottomNavController, startDestination = "home") {
                    composable("home") { HomeScreen(musicViewModel) }
                    composable("search") { SearchScreen(musicViewModel) }
                    composable("library") { LibraryScreen(musicViewModel) } // UPDATED LIBRARY
                    composable("profile") { ProfileScreen(onLogoutRequest) }
                }
            }
        }
        if (showFullPlayer) FullPlayerView(musicViewModel, onCollapse = { showFullPlayer = false })
    }
}

// --- 6. CORE SCREENS ---
@Composable
fun HomeScreen(vm: MusicViewModel) {
    LazyColumn(Modifier.fillMaxSize().padding(16.dp)) {
        item { Text("Good evening", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(16.dp)) }
        items(internetSongs) { song ->
            SongItem(song, isSelected = (song == vm.currentSong), isLiked = vm.likedSongs.contains(song), onLike = { vm.toggleLike(song) }) { vm.playSong(song) }
        }
    }
}

@Composable
fun SearchScreen(vm: MusicViewModel) {
    var query by remember { mutableStateOf("") }
    val filtered = internetSongs.filter { it.title.contains(query, ignoreCase = true) }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(value = query, onValueChange = { query = it }, placeholder = { Text("Search songs", color = Color.Gray) }, modifier = Modifier.fillMaxWidth())
        LazyColumn {
            items(filtered) { song -> SongItem(song, isSelected = (song == vm.currentSong), isLiked = vm.likedSongs.contains(song), onLike = { vm.toggleLike(song) }) { vm.playSong(song) } }
        }
    }
}

@Composable
fun LibraryScreen(vm: MusicViewModel) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Your Library", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        if (vm.likedSongs.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No liked songs yet!", color = Color.Gray) }
        } else {
            LazyColumn {
                items(vm.likedSongs) { song ->
                    SongItem(song, isSelected = (song == vm.currentSong), isLiked = true, onLike = { vm.toggleLike(song) }) { vm.playSong(song) }
                }
            }
        }
    }
}

@Composable
fun ProfileScreen(onLogout: () -> Unit) {
    val sp = LocalContext.current.getSharedPreferences("aura_prefs", Context.MODE_PRIVATE)
    val name = sp.getString("user_name", "DJ User") ?: "User"
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        // CHOICE ICON: AccountCircle for a professional Spotify look
        Icon(imageVector = Icons.Default.AccountCircle, contentDescription = null, tint = Color(0xFF1DB954), modifier = Modifier.size(120.dp))
        Spacer(Modifier.height(16.dp))
        Text(name, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Button(onClick = onLogout, Modifier.padding(top = 40.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text("Log Out") }
    }
}

// --- 7. PLAYER COMPONENTS ---
@Composable
fun SongItem(song: Song, isSelected: Boolean, isLiked: Boolean, onLike: () -> Unit, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        AsyncImage(model = song.imageUrl, contentDescription = null, modifier = Modifier.size(56.dp).background(Color(0xFF333333)))
        Column(Modifier.padding(start = 16.dp).weight(1f)) {
            Text(song.title, color = if (isSelected) Color(0xFF1DB954) else Color.White, fontWeight = FontWeight.Bold)
            Text(song.artist, color = Color.Gray, fontSize = 12.sp)
        }
        IconButton(onClick = onLike) {
            Icon(imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder, contentDescription = null, tint = if (isLiked) Color(0xFF1DB954) else Color.Gray)
        }
    }
}

@Composable
fun MiniPlayer(vm: MusicViewModel, onOpen: () -> Unit) {
    val song = vm.currentSong ?: return
    Row(Modifier.fillMaxWidth().height(64.dp).background(Color(0xFF282828)).clickable { onOpen() }.padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        AsyncImage(model = song.imageUrl, contentDescription = null, modifier = Modifier.size(48.dp))
        Text(song.title, color = Color.White, modifier = Modifier.weight(1f).padding(start = 12.dp), maxLines = 1)
        IconButton(onClick = { vm.playPrevious() }) { Icon(imageVector = Icons.Default.SkipPrevious, contentDescription = null, tint = Color.White) }
        IconButton(onClick = { vm.togglePlayPause() }) { Icon(imageVector = if (vm.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp)) }
        IconButton(onClick = { vm.playNext() }) { Icon(imageVector = Icons.Default.SkipNext, contentDescription = null, tint = Color.White) }
    }
}

@Composable
fun FullPlayerView(vm: MusicViewModel, onCollapse: () -> Unit) {
    val song = vm.currentSong ?: return
    BackHandler { onCollapse() }
    Column(Modifier.fillMaxSize().background(Color(0xFF121212)).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(onClick = onCollapse, Modifier.align(Alignment.Start)) { Icon(imageVector = Icons.Default.KeyboardArrowDown, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp)) }
        Spacer(Modifier.height(48.dp))
        AsyncImage(model = song.imageUrl, contentDescription = null, modifier = Modifier.fillMaxWidth().aspectRatio(1f).background(Color(0xFF333333)))
        Spacer(Modifier.height(48.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(song.title, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text(song.artist, color = Color.Gray, fontSize = 18.sp)
            }
            IconButton(onClick = { vm.toggleLike(song) }) {
                Icon(imageVector = if (vm.likedSongs.contains(song)) Icons.Default.Favorite else Icons.Default.FavoriteBorder, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
            }
        }
        Spacer(Modifier.height(30.dp))
        Slider(
            value = vm.currentPosition.toFloat(),
            onValueChange = { vm.seekTo(it) },
            valueRange = 0f..vm.totalDuration.toFloat().coerceAtLeast(1f),
            colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.White)
        )
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
            Text(vm.formatTime(vm.currentPosition), color = Color.Gray, fontSize = 12.sp)
            Text(vm.formatTime(vm.totalDuration), color = Color.Gray, fontSize = 12.sp)
        }
        Spacer(Modifier.height(40.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { vm.playPrevious() }) { Icon(imageVector = Icons.Default.SkipPrevious, contentDescription = null, tint = Color.White, modifier = Modifier.size(48.dp)) }
            FloatingActionButton(onClick = { vm.togglePlayPause() }, containerColor = Color.White, shape = CircleShape) { Icon(imageVector = if (vm.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black) }
            IconButton(onClick = { vm.playNext() }) { Icon(imageVector = Icons.Default.SkipNext, contentDescription = null, tint = Color.White, modifier = Modifier.size(48.dp)) }
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    NavigationBar(containerColor = Color.Black) {
        val items = listOf("Home", "Search", "Library", "Profile")
        val icons = listOf(Icons.Default.Home, Icons.Default.Search, Icons.Default.LibraryMusic, Icons.Default.Person)
        items.forEachIndexed { i, item ->
            val r = item.lowercase()
            NavigationBarItem(
                icon = { Icon(imageVector = icons[i], contentDescription = null) },
                label = { Text(item) },
                selected = currentRoute == r,
                onClick = { if (currentRoute != r) navController.navigate(r) { popUpTo(navController.graph.startDestinationId); launchSingleTop = true } },
                colors = NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFF1DB954), unselectedIconColor = Color.Gray, indicatorColor = Color.Transparent)
            )
        }
    }
}