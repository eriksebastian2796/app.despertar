package com.erik.despertar.ui

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.ComponentActivity
import androidx.compose.ui.window.Popup
import androidx.core.graphics.drawable.toBitmap
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

@Composable
fun LauncherScreen(viewModel: LauncherViewModel = hiltViewModel(LocalContext.current as ComponentActivity)) {
    val isDrawerOpen by viewModel.isDrawerOpen.collectAsState()
    val backgroundColor = if (isSystemInDarkTheme()) Color(0xFF1C1C1E) else Color.White

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .pointerInput(isDrawerOpen) {
                detectVerticalDragGestures { _, dragAmount ->
                    if (dragAmount < -50 && !isDrawerOpen) viewModel.setDrawerOpen(true)
                    if (dragAmount > 50 && isDrawerOpen) viewModel.setDrawerOpen(false)
                }
            }
    ) {
        LauncherHome(viewModel)

        AnimatedVisibility(
            visible = isDrawerOpen,
            enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(400)) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(400)) + fadeOut()
        ) {
            Box(modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
            ) {
                AppDrawer(viewModel)
            }
        }
    }
}

@Composable
fun LauncherHome(viewModel: LauncherViewModel) {
    val favorites by viewModel.favorites.collectAsState()
    val dockApps by viewModel.dockApps.collectAsState()
    
    var time by remember { mutableStateOf(currentTime()) }
    var date by remember { mutableStateOf(currentDate()) }
    
    LaunchedEffect(Unit) {
        while(true) {
            time = currentTime()
            date = currentDate()
            delay(1000)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(60.dp))
        
        Text(text = time, style = MaterialTheme.typography.displayLarge.copy(fontSize = 72.sp), fontWeight = FontWeight.Bold)
        Text(text = date, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)

        Spacer(modifier = Modifier.height(64.dp))

        LazyColumn(modifier = Modifier.weight(1f)) {
            item {
                WeatherCard()
                Spacer(modifier = Modifier.height(24.dp))
            }
            items(favorites) { app ->
                FavoriteLauncherItem(app = app)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            dockApps.forEach { app ->
                DockIcon(app)
            }
        }
        Spacer(modifier = Modifier.navigationBarsPadding().height(16.dp))
    }
}

@Composable
fun FavoriteLauncherItem(app: AppInfo) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { 
                app.intent?.let { 
                    it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(it) 
                }
            }
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = app.label,
            style = MaterialTheme.typography.headlineSmall,
            fontSize = 24.sp
        )
    }
}

@Composable
fun DockIcon(app: AppInfo) {
    val context = LocalContext.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(70.dp)
            .clickable { 
                app.intent?.let { 
                    it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(it) 
                }
            }
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            app.icon?.let {
                Image(
                    bitmap = it.toBitmap().asImageBitmap(),
                    contentDescription = app.label,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = app.label,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            color = if (isSystemInDarkTheme()) Color.White.copy(alpha = 0.8f) else Color.Black.copy(alpha = 0.8f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDrawer(viewModel: LauncherViewModel) {
    val apps by viewModel.installedApps.collectAsState()
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    
    var activeLetter by remember { mutableStateOf<Char?>(null) }
    var showPopup by remember { mutableStateOf(false) }
    
    val filteredApps = if (searchQuery.isEmpty()) apps else apps.filter { it.label.contains(searchQuery, ignoreCase = true) }
    val groupedApps = filteredApps.groupBy { it.label.first().uppercaseChar() }.toSortedMap()
    
    val textColor = if (isSystemInDarkTheme()) Color.White else Color.Black

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Buscar en el cajón...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Row(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(0.85f).fillMaxHeight(),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp)
                ) {
                    groupedApps.forEach { (letter, appsInGroup) ->
                        item(key = letter) {
                            Text(
                                text = letter.toString(),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                        }
                        items(appsInGroup) { app ->
                            Text(
                                text = app.label,
                                style = MaterialTheme.typography.bodyLarge,
                                fontSize = 18.sp,
                                color = textColor,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { 
                                        app.intent?.let { 
                                            it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            context.startActivity(it) 
                                            viewModel.setDrawerOpen(false)
                                        }
                                    }
                                    .padding(vertical = 14.dp)
                            )
                        }
                    }
                }

                val alphabet = ('A'..'Z').toList()
                var scrollerHeight by remember { mutableIntStateOf(0) }
                
                Column(
                    modifier = Modifier
                        .weight(0.15f)
                        .fillMaxHeight()
                        .onGloballyPositioned { scrollerHeight = it.size.height }
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { showPopup = true },
                                onDragEnd = { 
                                    scope.launch {
                                        delay(1000)
                                        showPopup = false
                                        activeLetter = null
                                    }
                                },
                                onDragCancel = { showPopup = false },
                                onDrag = { change, _ ->
                                    val touchY = change.position.y
                                    val percent = (touchY / scrollerHeight).coerceIn(0f, 1f)
                                    val index = (percent * (alphabet.size - 1)).roundToInt()
                                    val letter = alphabet[index]
                                    
                                    if (letter != activeLetter) {
                                        activeLetter = letter
                                        if (groupedApps.containsKey(letter)) {
                                            scope.launch {
                                                listState.scrollToItem(calculateScrollIndex(groupedApps, letter))
                                            }
                                        }
                                    }
                                }
                            )
                        },
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    alphabet.forEach { char ->
                        val isAvailable = groupedApps.containsKey(char)
                        Text(
                            text = char.toString(),
                            fontSize = 11.sp,
                            fontWeight = if (char == activeLetter) FontWeight.ExtraBold else FontWeight.Normal,
                            color = when {
                                char == activeLetter -> MaterialTheme.colorScheme.primary
                                isAvailable -> MaterialTheme.colorScheme.secondary
                                else -> Color.Gray.copy(alpha = 0.3f)
                            },
                            modifier = Modifier.padding(vertical = 1.dp)
                        )
                    }
                }
            }
        }

        if (showPopup && activeLetter != null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier.size(120.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = Color.Black.copy(alpha = 0.6f),
                    contentColor = Color.White
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = activeLetter.toString(),
                            fontSize = 60.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }
    }
}

private fun calculateScrollIndex(groupedApps: Map<Char, List<AppInfo>>, target: Char): Int {
    var count = 0
    for ((letter, apps) in groupedApps) {
        if (letter == target) return count
        count += apps.size + 1
    }
    val nextAvailable = groupedApps.keys.find { it > target }
    if (nextAvailable != null) return calculateScrollIndex(groupedApps, nextAvailable)
    return count
}

@Composable
fun WeatherCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "22°", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = "Ciudad de México", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(text = "Parcialmente nublado", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

private fun currentTime(): String = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
private fun currentDate(): String = SimpleDateFormat("EEEE, d 'de' MMMM", Locale.getDefault()).format(Date())
