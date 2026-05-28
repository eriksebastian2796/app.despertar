package com.erik.despertar.ui

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.accompanist.drawablepainter.rememberDrawablePainter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun StatsScreen(
    viewModel: StatsViewModel = hiltViewModel(),
    onLimitClick: (String, String) -> Unit
) {
    val usageStats by viewModel.usageStats.collectAsState()
    val dailyUsageData by viewModel.dailyUsageData.collectAsState()
    val hasPermission by viewModel.hasPermission.collectAsState()
    val currentFilter by viewModel.currentFilter.collectAsState()
    val totalTimeMillis by viewModel.totalTime.collectAsState()
    val periodLabel by viewModel.periodLabel.collectAsState()
    val offset by viewModel.offset.collectAsState()
    val summary by viewModel.summary.collectAsState()
    
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    var selectedApp by remember { mutableStateOf<AppUsageInfo?>(null) }
    val sheetState = rememberModalBottomSheetState()
    var showSheet by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.checkPermission()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (!hasPermission) {
                PermissionRequiredView {
                    context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    item {
                        FilterTabs(
                            selectedFilter = currentFilter,
                            onFilterSelected = { viewModel.setFilter(it) }
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        PeriodSelector(
                            label = periodLabel,
                            canGoNext = offset < 0,
                            onPrevious = { viewModel.navigatePeriod(-1) },
                            onNext = { viewModel.navigatePeriod(1) }
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        TotalTimeView(totalTimeMillis)
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        ChartContainer(
                            dailyUsageData = dailyUsageData,
                            filter = currentFilter,
                            onSwipeLeft = { viewModel.navigatePeriod(1) },
                            onSwipeRight = { viewModel.navigatePeriod(-1) }
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        CategoryLegend()
                        
                        if (currentFilter == StatsFilter.TODAY) {
                            Spacer(modifier = Modifier.height(24.dp))
                            ScreenTimeCard(totalTimeMillis)
                        }
                        
                        if (currentFilter != StatsFilter.TODAY) {
                            Spacer(modifier = Modifier.height(16.dp))
                            UsageSummaryCard(summary)
                        }
                        
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        Text(
                            text = "Más usadas",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    items(usageStats) { info ->
                        AppUsageItem(
                            info = info,
                            maxTime = usageStats.firstOrNull()?.totalTimeInForeground ?: 1L,
                            onClick = {
                                selectedApp = info
                                showSheet = true
                            }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }

    if (showSheet && selectedApp != null) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState = sheetState
        ) {
            AppOptionsSheet(
                app = selectedApp!!,
                onLimitClick = {
                    showSheet = false
                    onLimitClick(selectedApp!!.packageName, selectedApp!!.appName)
                },
                onDetailClick = {
                    showSheet = false
                }
            )
        }
    }
}

@Composable
fun TotalTimeView(timeMillis: Long) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Tiempo total",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline
        )
        Text(
            text = formatMillis(timeMillis),
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun ScreenTimeCard(onTimeMillis: Long) {
    val totalAvailableMillis = 24 * 60 * 60 * 1000L
    val offTimeMillis = (totalAvailableMillis - onTimeMillis).coerceAtLeast(0L)
    val progress = (onTimeMillis.toFloat() / totalAvailableMillis).coerceIn(0f, 1f)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Tiempo de pantalla",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Encendida", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    Text(formatMillis(onTimeMillis), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Apagada", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    Text(formatMillis(offTimeMillis), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun UsageSummaryCard(summary: UsageSummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            SummaryRow("Promedio diario", formatMillis(summary.averageDaily))
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
            SummaryRow("Día de más uso", "${formatMillis(summary.maxUsage.second)} (${summary.maxUsage.first})")
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
            SummaryRow("Día de menos uso", "${formatMillis(summary.minUsage.second)} (${summary.minUsage.first})")
        }
    }
}

@Composable
fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
    }
}

@Composable
fun PeriodSelector(
    label: String,
    canGoNext: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Anterior")
        }
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        IconButton(onClick = onNext, enabled = canGoNext) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Siguiente")
        }
    }
}

@Composable
fun ChartContainer(
    dailyUsageData: List<DailyUsage>,
    filter: StatsFilter,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit
) {
    var offsetX by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (offsetX > 100) onSwipeRight()
                        else if (offsetX < -100) onSwipeLeft()
                        offsetX = 0f
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        offsetX += dragAmount
                    }
                )
            }
    ) {
        if (filter == StatsFilter.MONTH) {
            MonthlyAreaChart(dailyUsageData)
        } else {
            StackedBarChart(dailyUsageData, filter)
        }
    }
}

@Composable
fun StackedBarChart(data: List<DailyUsage>, filter: StatsFilter) {
    if (data.isEmpty()) return

    val maxTotalTime = if (filter == StatsFilter.TODAY) {
        val maxInFranja = data.maxOfOrNull { it.totalTime } ?: 1L
        val thirtyMin = 30 * 60 * 1000L
        ((maxInFranja / thirtyMin) + 1) * thirtyMin
    } else {
        24 * 60 * 60 * 1000L
    }

    Box(modifier = Modifier.fillMaxWidth().height(240.dp)) {
        // Líneas de referencia horizontales y etiquetas Y
        Canvas(modifier = Modifier.fillMaxSize().padding(bottom = 32.dp)) {
            val dashPathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
            val divisions = 4
            for (i in 0..divisions) {
                val y = size.height - (size.height * (i.toFloat() / divisions))
                if (i > 0) {
                    drawLine(
                        color = Color.LightGray.copy(alpha = 0.5f),
                        start = Offset(40.dp.toPx(), y),
                        end = Offset(size.width, y),
                        pathEffect = dashPathEffect,
                        strokeWidth = 1.dp.toPx()
                    )
                }
                
                // Etiquetas Y
                val label = if (filter == StatsFilter.TODAY) {
                    val minutes = (maxTotalTime / divisions * i) / (1000 * 60)
                    if (minutes >= 60) "${minutes / 60}h ${minutes % 60}m" else "${minutes}m"
                } else {
                    if (i == 0) "" else "${i * 6}h"
                }
                
                if (label.isNotEmpty()) {
                    drawContext.canvas.nativeCanvas.drawText(
                        label,
                        5.dp.toPx(),
                        y + 4.dp.toPx(),
                        android.graphics.Paint().apply {
                            color = android.graphics.Color.GRAY
                            textSize = 10.sp.toPx()
                        }
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxSize().padding(start = 40.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            data.forEachIndexed { index, daily ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth(0.7f),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            var currentY = size.height
                            val totalHeight = size.height
                            
                            val categories = AppCategory.entries.reversed()
                            categories.forEachIndexed { catIndex, category ->
                                val usage = daily.usageByCategory[category] ?: 0L
                                val rawHeight = (usage.toFloat() / maxTotalTime) * totalHeight
                                val barHeight = if (usage > 0) rawHeight.coerceAtLeast(1.dp.toPx()) else 0f
                                
                                if (barHeight > 0) {
                                    val isTop = catIndex == categories.indexOfLast { (daily.usageByCategory[it] ?: 0L) > 0 }
                                    
                                    drawRoundRect(
                                        color = category.color,
                                        topLeft = Offset(0f, (currentY - barHeight).coerceAtLeast(0f)),
                                        size = Size(size.width, barHeight),
                                        cornerRadius = if (isTop) CornerRadius(4.dp.toPx(), 4.dp.toPx()) else CornerRadius.Zero
                                    )
                                    currentY -= barHeight
                                }
                            }
                        }
                    }
                    
                    val showLabel = when(filter) {
                        StatsFilter.TODAY -> index % 6 == 0 || index == 23
                        StatsFilter.WEEK -> true
                        else -> false
                    }
                    
                    if (showLabel) {
                        val label = if (filter == StatsFilter.TODAY && index == 23) "12 AM" else daily.dayLabel
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.outline,
                            maxLines = 1
                        )
                    } else {
                        Spacer(modifier = Modifier.height(26.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun MonthlyAreaChart(data: List<DailyUsage>) {
    if (data.isEmpty()) return

    val maxTotalTime = 24 * 60 * 60 * 1000L
    val primaryColor = MaterialTheme.colorScheme.primary

    Box(modifier = Modifier.fillMaxWidth().height(240.dp)) {
        Canvas(modifier = Modifier.fillMaxSize().padding(bottom = 32.dp)) {
            val dashPathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
            val divisions = 4
            for (i in 0..divisions) {
                val y = size.height - (size.height * (i.toFloat() / divisions))
                if (i > 0) {
                    drawLine(
                        color = Color.LightGray.copy(alpha = 0.3f),
                        start = Offset(40.dp.toPx(), y),
                        end = Offset(size.width, y),
                        pathEffect = dashPathEffect,
                        strokeWidth = 1.dp.toPx()
                    )
                }

                // Etiquetas Y
                if (i > 0) {
                    drawContext.canvas.nativeCanvas.drawText(
                        "${i * 6}h",
                        5.dp.toPx(),
                        y + 4.dp.toPx(),
                        android.graphics.Paint().apply {
                            color = android.graphics.Color.GRAY
                            textSize = 10.sp.toPx()
                        }
                    )
                }
            }

            if (data.size < 2) return@Canvas

            val chartWidth = size.width - 60.dp.toPx() // padding lateral
            val spacing = chartWidth / (data.size - 1)
            val startX = 40.dp.toPx()
            
            val points = data.mapIndexed { index, daily ->
                val x = startX + index * spacing
                val y = size.height - (daily.totalTime.toFloat() / maxTotalTime) * size.height
                Offset(x, y.coerceIn(0f, size.height))
            }

            val fillPath = Path().apply {
                moveTo(points.first().x, size.height)
                points.forEach { lineTo(it.x, it.y) }
                lineTo(points.last().x, size.height)
                close()
            }

            val strokePath = Path().apply {
                moveTo(points.first().x, points.first().y)
                points.forEach { lineTo(it.x, it.y) }
            }

            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(primaryColor.copy(alpha = 0.4f), Color.Transparent),
                    startY = points.minOf { it.y },
                    endY = size.height
                )
            )

            drawPath(
                path = strokePath,
                color = primaryColor,
                style = Stroke(width = 3.dp.toPx())
            )

            val maxPoint = points.minByOrNull { it.y }
            if (maxPoint != null) {
                drawCircle(color = Color.White, radius = 5.dp.toPx(), center = maxPoint)
                drawCircle(color = primaryColor, radius = 5.dp.toPx(), center = maxPoint, style = Stroke(width = 2.dp.toPx()))
            }
        }

        Row(
            modifier = Modifier.fillMaxSize().padding(start = 40.dp, end = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            data.forEach { daily ->
                Text(
                    text = daily.dayLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryLegend() {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        AppCategory.entries.forEach { category ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(category.color)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = category.label, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
fun AppUsageItem(info: AppUsageInfo, maxTime: Long, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            info.icon?.let {
                androidx.compose.foundation.Image(
                    painter = rememberDrawablePainter(drawable = it),
                    contentDescription = null,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = info.appName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                Text(text = info.timeFormatted, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            val progress = (info.totalTimeInForeground.toFloat() / maxTime).coerceIn(0f, 1f)
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                color = info.category.color,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
        }
    }
}

@Composable
fun AppOptionsSheet(
    app: AppUsageInfo,
    onLimitClick: () -> Unit,
    onDetailClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .padding(bottom = 32.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                app.icon?.let {
                    androidx.compose.foundation.Image(
                        painter = rememberDrawablePainter(drawable = it),
                        contentDescription = null,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = app.appName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(24.dp))

        ListItem(
            headlineContent = { Text("Limitar tiempo de uso") },
            leadingContent = { Icon(Icons.Default.Timer, contentDescription = null) },
            modifier = Modifier.clickable { onLimitClick() }
        )
        ListItem(
            headlineContent = { Text("Ver detalle") },
            leadingContent = { Icon(Icons.Default.Info, contentDescription = null) },
            modifier = Modifier.clickable { onDetailClick() }
        )
    }
}

@Composable
fun FilterTabs(
    selectedFilter: StatsFilter,
    onFilterSelected: (StatsFilter) -> Unit
) {
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        StatsFilter.entries.forEachIndexed { index, filter ->
            val label = when (filter) {
                StatsFilter.TODAY -> "Hoy"
                StatsFilter.WEEK -> "Semana"
                StatsFilter.MONTH -> "Mes"
            }
            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(index = index, count = StatsFilter.entries.size),
                onClick = { onFilterSelected(filter) },
                selected = selectedFilter == filter
            ) {
                Text(label)
            }
        }
    }
}

@Composable
fun PermissionRequiredView(onGrantClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Se requiere acceso a las estadísticas de uso.",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Button(onClick = onGrantClick) {
            Text("Conceder Permiso")
        }
    }
}
