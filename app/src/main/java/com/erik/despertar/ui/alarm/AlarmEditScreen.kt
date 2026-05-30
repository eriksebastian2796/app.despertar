package com.erik.despertar.ui.alarm

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.erik.despertar.data.AlarmEntity
import com.erik.despertar.data.ChallengeType
import com.erik.despertar.ui.AlarmViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmEditScreen(
    alarmId: Int?,
    viewModel: AlarmViewModel,
    onSave: () -> Unit,
    onBack: () -> Unit
) {
    val alarms by viewModel.alarms.collectAsStateWithLifecycle()
    val alarmToEdit = remember(alarmId, alarms) {
        alarms.find { it.id == alarmId } ?: AlarmEntity(hour = 8, minute = 0)
    }

    var hour by remember(alarmToEdit) { mutableIntStateOf(alarmToEdit.hour) }
    var minute by remember(alarmToEdit) { mutableIntStateOf(alarmToEdit.minute) }
    var label by remember(alarmToEdit) { mutableStateOf(alarmToEdit.label) }
    var repeatDays by remember(alarmToEdit) { mutableStateOf(alarmToEdit.repeatDays) }
    var challengeType by remember(alarmToEdit) { mutableStateOf(alarmToEdit.challengeType) }
    var difficulty by remember(alarmToEdit) { mutableIntStateOf(alarmToEdit.difficulty) }
    var problemsCount by remember(alarmToEdit) { mutableIntStateOf(alarmToEdit.problemsCount) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (alarmId == null || alarmId == 0) "Nueva Alarma" else "Editar Alarma") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            
            // 1. Selector de Tambor (Drum Picker)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                // Indicador central
                Surface(
                    modifier = Modifier.fillMaxWidth(0.6f).height(60.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(12.dp)
                ) {}
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DrumPicker(
                        items = (0..23).toList(),
                        initialItem = hour,
                        onItemSelected = { hour = it },
                        modifier = Modifier.width(80.dp)
                    )
                    Text(":", fontSize = 40.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp))
                    DrumPicker(
                        items = (0..59).toList(),
                        initialItem = minute,
                        onItemSelected = { minute = it },
                        modifier = Modifier.width(80.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 2. Selector de días mejorado
            Text("Repetir días", style = MaterialTheme.typography.titleSmall, modifier = Modifier.align(Alignment.Start))
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val days = listOf("L", "M", "M", "J", "V", "S", "D")
                days.forEachIndexed { index, day ->
                    val dayNum = index + 1
                    DayChip(
                        text = day,
                        isSelected = repeatDays.contains(dayNum),
                        onClick = {
                            repeatDays = if (repeatDays.contains(dayNum)) repeatDays - dayNum else repeatDays + dayNum
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                label = { Text("Nombre de la alarma") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text("Método de apagado", style = MaterialTheme.typography.titleSmall, modifier = Modifier.align(Alignment.Start))
            
            ChallengeTypeOption("Ninguno", challengeType == ChallengeType.NONE) { challengeType = ChallengeType.NONE }
            ChallengeTypeOption("Matemáticas", challengeType == ChallengeType.MATH) { challengeType = ChallengeType.MATH }
            
            if (challengeType == ChallengeType.MATH) {
                MathSettings(
                    difficulty = difficulty,
                    onDifficultyChange = { difficulty = it },
                    problemsCount = problemsCount,
                    onCountChange = { problemsCount = it }
                )
            }
            
            ChallengeTypeOption("Código de barras / QR", challengeType == ChallengeType.BARCODE) { challengeType = ChallengeType.BARCODE }
            if (challengeType == ChallengeType.BARCODE) {
                Text("Vas a registrar el código al guardar", style = MaterialTheme.typography.bodySmall, color = Color.Gray, modifier = Modifier.padding(start = 48.dp))
            }
            
            ChallengeTypeOption("Foto de objeto", challengeType == ChallengeType.PHOTO) { challengeType = ChallengeType.PHOTO }
            if (challengeType == ChallengeType.PHOTO) {
                Text("Vas a registrar los objetos al guardar", style = MaterialTheme.typography.bodySmall, color = Color.Gray, modifier = Modifier.padding(start = 48.dp))
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    val finalAlarm = alarmToEdit.copy(
                        hour = hour,
                        minute = minute,
                        label = label,
                        repeatDays = repeatDays,
                        challengeType = challengeType,
                        difficulty = difficulty,
                        problemsCount = problemsCount,
                        isEnabled = true
                    )
                    viewModel.saveAlarm(finalAlarm)
                    onSave()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("GUARDAR ALARMA", modifier = Modifier.padding(8.dp), fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun DrumPicker(
    items: List<Int>,
    initialItem: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val itemHeight = 60.dp
    // Usamos un índice muy grande para simular scroll infinito (loop)
    // El item real es index % items.size
    val totalItems = 10000
    val middleIndex = totalItems / 2
    // Ajustamos el índice inicial para que coincida con el item seleccionado
    val startIndex = middleIndex - (middleIndex % items.size) + items.indexOf(initialItem)
    
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = startIndex)

    // Sincronizar selección al finalizar el scroll
    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) {
            val centerIndex = listState.firstVisibleItemIndex
            onItemSelected(items[centerIndex % items.size])
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.height(itemHeight * 3), // Mostramos siempre 3 items (arriba, seleccionado, abajo)
        contentPadding = PaddingValues(vertical = itemHeight),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        items(totalItems) { index ->
            val item = items[index % items.size]
            val isSelected by remember {
                derivedStateOf { listState.firstVisibleItemIndex == index }
            }
            
            Box(
                modifier = Modifier.height(itemHeight),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = String.format(Locale.getDefault(), "%02d", item),
                    fontSize = if (isSelected) 40.sp else 26.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.alpha(if (isSelected) 1f else 0.3f)
                )
            }
        }
    }
}

@Composable
fun DayChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(40.dp)
            .clickable { onClick() },
        shape = CircleShape,
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text = text, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ChallengeTypeOption(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun MathSettings(
    difficulty: Int,
    onDifficultyChange: (Int) -> Unit,
    problemsCount: Int,
    onCountChange: (Int) -> Unit
) {
    Column(modifier = Modifier.padding(start = 32.dp, end = 16.dp, top = 8.dp, bottom = 16.dp)) {
        Text("Dificultad:", style = MaterialTheme.typography.bodyMedium)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val levels = listOf("Fácil", "Medio", "Difícil")
            levels.forEachIndexed { index, level ->
                val levelValue = index + 1
                FilterChip(
                    selected = difficulty == levelValue,
                    onClick = { onDifficultyChange(levelValue) },
                    label = { Text(level) }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text("Cantidad de problemas", style = MaterialTheme.typography.bodyMedium)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(top = 8.dp)
        ) {
            IconButton(
                onClick = { if (problemsCount > 1) onCountChange(problemsCount - 1) },
                colors = IconButtonDefaults.filledIconButtonColors()
            ) {
                Icon(Icons.Default.Remove, contentDescription = null)
            }
            
            Text(
                text = problemsCount.toString(),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            IconButton(
                onClick = { if (problemsCount < 5) onCountChange(problemsCount + 1) },
                colors = IconButtonDefaults.filledIconButtonColors()
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
            }
        }
    }
}
