package com.erik.despertar.ui.alarm

import android.content.Context
import android.content.Intent
import android.os.*
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.erik.despertar.service.AlarmService
import com.erik.despertar.ui.theme.DespertarTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

class AlarmChallengeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Configuración para mostrar sobre pantalla de bloqueo
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
        )

        setContent {
            DespertarTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    MathChallengeContent(
                        onFinished = {
                            stopService(Intent(this, AlarmService::class.java))
                            finish()
                        }
                    )
                }
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // No permitir cerrar con el botón atrás
    }
}

@Composable
fun MathChallengeContent(onFinished: () -> Unit) {
    val context = LocalContext.current
    val vibrator = remember { 
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    var num1 by remember { mutableIntStateOf(Random.nextInt(1, 20)) }
    var num2 by remember { mutableIntStateOf(Random.nextInt(1, 20)) }
    var correctAnswer by remember { mutableIntStateOf(num1 + num2) }
    var options by remember { mutableStateOf(generateOptions(correctAnswer)) }
    
    var problemsSolved by remember { mutableIntStateOf(0) }
    val totalProblems = 3
    
    var selectedOption by remember { mutableStateOf<Int?>(null) }
    var isError by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("¡DESPIERTA!", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Black)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Resuelve para apagar", style = MaterialTheme.typography.titleLarge)
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Text(text = "$num1 + $num2 =", fontSize = 56.sp, fontWeight = FontWeight.Bold)
        
        Spacer(modifier = Modifier.height(48.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(options) { option ->
                val isCorrect = option == correctAnswer
                val buttonColor = when {
                    selectedOption == option && isCorrect -> Color(0xFF4CAF50) // Verde
                    selectedOption == option && !isCorrect -> Color(0xFFF44336) // Rojo
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }

                Button(
                    onClick = {
                        if (selectedOption != null) return@Button
                        selectedOption = option
                        
                        if (isCorrect) {
                            scope.launch {
                                delay(500)
                                problemsSolved++
                                if (problemsSolved >= totalProblems) {
                                    onFinished()
                                } else {
                                    // Generar nuevo problema
                                    num1 = Random.nextInt(1, 10 * (problemsSolved + 1))
                                    num2 = Random.nextInt(1, 10 * (problemsSolved + 1))
                                    correctAnswer = num1 + num2
                                    options = generateOptions(correctAnswer)
                                    selectedOption = null
                                }
                            }
                        } else {
                            // Vibrar en error
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
                            } else {
                                @Suppress("DEPRECATION")
                                vibrator.vibrate(200)
                            }
                            
                            scope.launch {
                                isError = true
                                delay(500)
                                options = generateOptions(correctAnswer) // Regenerar opciones
                                selectedOption = null
                                isError = false
                            }
                        }
                    },
                    modifier = Modifier.height(90.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = buttonColor,
                        contentColor = if (selectedOption == option) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text(text = option.toString(), fontSize = 28.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "Problema ${problemsSolved + 1} de $totalProblems",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

private fun generateOptions(correct: Int): List<Int> {
    val options = mutableSetOf<Int>()
    options.add(correct)
    while (options.size < 4) {
        val distractor = correct + Random.nextInt(-10, 11)
        if (distractor != correct && distractor > 0) {
            options.add(distractor)
        }
    }
    return options.toList().shuffled()
}
