package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sin

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        Scaffold(
          modifier = Modifier.fillMaxSize(),
          containerColor = SlateBlack
        ) { innerPadding ->
          VoiceAssistantApp(modifier = Modifier.padding(innerPadding))
        }
      }
    }
  }
}

// Data Classes representing Assistant Logs
data class CommandLog(
  val id: Long,
  val commandText: String,
  val responseText: String,
  val latencyMs: Int,
  val timestamp: String,
  val localProcessed: Boolean,
  val ancActive: Boolean
)

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun VoiceAssistantApp(modifier: Modifier = Modifier) {
  val coroutineScope = rememberCoroutineScope()

  // Tab State: 0 = Dashboard & Ambient ANC, 1 = Task Center & Security, 2 = Hotwords
  var currentTab by remember { mutableStateOf(0) }

  // System Toggles
  var modelProcessingTypeLocal by remember { mutableStateOf(true) } // true = Local Core 25ms, false = Cloud 240ms
  var activeNoiseCancellation by remember { mutableStateOf(true) }
  var allowVoiceLockUnlock by remember { mutableStateOf(true) }
  var thresholdSensitivityDb by remember { mutableStateOf(42f) } // sensitivity

  // UI States
  var isLocked by remember { mutableStateOf(false) }
  var assistantStatusText by remember { mutableStateOf("Ready & Monitoring Hotwords") }
  var isListening by remember { mutableStateOf(false) }
  var typedMockCommand by remember { mutableStateOf("") }
  var simulatedReelMediaPlaying by remember { mutableStateOf(true) } // Simulating background media
  var lastInstaAction by remember { mutableStateOf("Playing Reel #2481") }

  // Hotwords Database
  val defaultHotwords = remember { mutableStateListOf("Jarvis", "Computer", "Hey Assistant") }
  var newHotwordInput by remember { mutableStateOf("") }

  // Logs Database
  val assistantLogList = remember {
    mutableStateListOf(
      CommandLog(1, "Pause Instagram reel", "Social feed playback suspended.", 24, "03:12:10", true, true),
      CommandLog(2, "Call Brother", "Initiating call to contacts: Brother.", 25, "03:13:02", true, true),
      CommandLog(3, "Skip media", "Skipped next reel video successfully.", 212, "03:14:15", false, false)
    )
  }

  // Animation ticks for waveforms
  var animationTick by remember { mutableStateOf(0f) }
  LaunchedEffect(key1 = isListening, key2 = simulatedReelMediaPlaying) {
    while (true) {
      animationTick += 0.15f
      delay(30)
    }
  }

  // Function to process commands
  val processVoiceCommand: (String) -> Unit = { command ->
    if (command.trim().isNotEmpty()) {
      isListening = true
      assistantStatusText = "Analyzing audio stream..."
      
      coroutineScope.launch {
        delay(400) // Simulate voice sampling time
        assistantStatusText = "Noise filtering applied..."
        delay(500) // Simulate processing delay
        
        val commandLower = command.lowercase()
        val responseText: String
        val successFlag: Boolean
        
        when {
          commandLower.contains("pause") || commandLower.contains("stop") -> {
            simulatedReelMediaPlaying = false
            lastInstaAction = "Paused via Vocal Override"
            responseText = "Skipped background sound. Instagram reel paused."
            successFlag = true
          }
          commandLower.contains("skip") || commandLower.contains("next") -> {
            simulatedReelMediaPlaying = true
            lastInstaAction = "Skipped to Reel #2482"
            responseText = "Social media skipped successfully with noise cancel."
            successFlag = true
          }
          commandLower.contains("call") -> {
            val person = commandLower.substringAfter("call", "Contact")
            responseText = "Calling $person..."
            successFlag = true
          }
          commandLower.contains("message") || commandLower.contains("text") -> {
            val body = commandLower.substringAfter("message", "Target message sent.")
            responseText = "Sent message: $body"
            successFlag = true
          }
          commandLower.contains("unlock") -> {
            isLocked = false
            responseText = "biometric voice match certified. Device unlocked."
            successFlag = true
          }
          else -> {
            responseText = "Command accepted. Executing localized subtask."
            successFlag = true
          }
        }

        val latency = if (modelProcessingTypeLocal) (15..32).random() else (210..290).random()
        
        assistantLogList.add(
          0,
          CommandLog(
            id = System.currentTimeMillis(),
            commandText = command,
            responseText = responseText,
            latencyMs = latency,
            timestamp = "03:" + (10..59).random() + ":" + (10..59).random(),
            localProcessed = modelProcessingTypeLocal,
            ancActive = activeNoiseCancellation
          )
        )

        assistantStatusText = responseText
        isListening = false
      }
    }
  }

  // Locked mode automatic unlocking simulation helper
  val runLockScreenSimulation: (String) -> Unit = { voiceCommand ->
    isLocked = true
    assistantStatusText = "Device Locked securely. Listening for biometric bypass..."
    coroutineScope.launch {
      delay(1200)
      assistantStatusText = "Biometrics match! Authenticating voice..."
      delay(1000)
      isLocked = false
      assistantStatusText = "Executing task secure bypass..."
      delay(500)
      processVoiceCommand(voiceCommand)
    }
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(SlateBlack)
      .padding(16.dp),
    verticalArrangement = Arrangement.Top,
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    // Title header
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Column {
        Text(
          text = "NEURAL ASSISTANT",
          color = Color.White,
          fontSize = 20.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace,
          letterSpacing = 1.sp
        )
        Text(
          text = "Local Processing Edge AI • Offline Active ANC",
          color = CyberCyan,
          fontSize = 11.sp,
          fontWeight = FontWeight.SemiBold,
          fontFamily = FontFamily.Monospace
        )
      }

      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(6.dp))
          .background(if (modelProcessingTypeLocal) CyberGreen.copy(alpha = 0.15f) else CyberOrange.copy(alpha = 0.15f))
          .border(1.dp, if (modelProcessingTypeLocal) CyberGreen else CyberOrange, RoundedCornerShape(6.dp))
          .padding(horizontal = 8.dp, vertical = 4.dp)
      ) {
        Text(
          text = if (modelProcessingTypeLocal) "LOCAL ENGINE (25ms)" else "CLOUD ENGINE (240ms)",
          color = if (modelProcessingTypeLocal) CyberGreen else CyberOrange,
          fontSize = 10.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace
        )
      }
    }

    // Audio Cancellation Waves Panel
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .height(130.dp)
        .padding(vertical = 4.dp),
      colors = CardDefaults.cardColors(containerColor = SlateCard),
      border = BorderStroke(1.dp, CardBorder),
      shape = RoundedCornerShape(12.dp)
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(12.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.GraphicEq,
              contentDescription = "Waveforms",
              tint = CyberCyan,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = if (activeNoiseCancellation) "ANC Real-Time Interception (Phase Negative)" else "Standard Acoustic Isolation Engine",
              color = Color.White,
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Monospace
            )
          }

          Box(
            modifier = Modifier
              .size(8.dp)
              .clip(CircleShape)
              .background(if (activeNoiseCancellation) CyberGreen else CyberOrange)
          )
        }

        // The Custom Waveform Canvas indicating Loud noise cancel out
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .padding(top = 8.dp)
        ) {
          Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val centerY = height / 2f

            // 1. Loud media device sound (High volume red/purple spike)
            val mediaPath = Path()
            mediaPath.moveTo(0f, centerY)
            for (i in 0..100) {
              val x = (width / 100f) * i
              val phase = animationTick * 2.0f
              val amp = if (simulatedReelMediaPlaying) 32f else 4f
              val y = centerY + sin(i * 0.18f + phase) * amp
              if (i == 0) mediaPath.moveTo(x, y) else mediaPath.lineTo(x, y)
            }
            drawPath(
              path = mediaPath,
              color = CyberViolet.copy(alpha = if (activeNoiseCancellation) 0.35f else 0.8f),
              style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
            )

            // 2. Anti-Phase cancellation wave (Superimposed green negative phase)
            if (activeNoiseCancellation) {
              val cancPath = Path()
              cancPath.moveTo(0f, centerY)
              for (i in 0..100) {
                val x = (width / 100f) * i
                val phase = animationTick * 2.0f
                val amp = if (simulatedReelMediaPlaying) -32f else -4f // exact negative inverse
                val y = centerY + sin(i * 0.18f + phase) * amp
                if (i == 0) cancPath.moveTo(x, y) else cancPath.lineTo(x, y)
              }
              drawPath(
                path = cancPath,
                color = CyberCyan.copy(alpha = 0.5f),
                style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
              )
            }

            // 3. Crisp Vocal command extraction wave (Clear clean yellow command peak)
            val vocalPath = Path()
            vocalPath.moveTo(0f, centerY)
            for (i in 0..100) {
              val x = (width / 100f) * i
              var amp = 0f
              // Build wave pulse where microphone detects vocal command
              if (i in 35..65) {
                val envelope = sin((i - 35f) / 30f * Math.PI.toFloat())
                amp = envelope * (if (isListening) 28f else 10f)
              }
              val y = centerY + sin(i * 0.4f - animationTick * 2.5f) * amp
              if (i == 0) vocalPath.moveTo(x, y) else vocalPath.lineTo(x, y)
            }
            drawPath(
              path = vocalPath,
              color = if (isListening) CyberGreen else Color.White.copy(alpha = 0.5f),
              style = Stroke(width = if (isListening) 3.dp.toPx() else 1.5.dp.toPx(), cap = StrokeCap.Round)
            )

            // Center straight dynamic line indicator
            drawLine(
              color = CardBorder.copy(alpha = 0.3f),
              start = Offset(0f, centerY),
              end = Offset(width, centerY),
              strokeWidth = 1.dp.toPx()
            )
          }
        }

        // Legend label
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text(text = "🟣 High Ambient Vol (Music/Reels)", color = MutedText, fontSize = 7.sp, fontFamily = FontFamily.Monospace)
          if (activeNoiseCancellation) {
            Text(text = "🔵 Anti-Phase Neutralizer", color = CyberCyan, fontSize = 7.sp, fontFamily = FontFamily.Monospace)
          }
          Text(text = "🟢 Isolated Clear Command Voice", color = CyberGreen, fontSize = 7.sp, fontFamily = FontFamily.Monospace)
        }
      }
    }

    // Dynamic Central Status Console
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 4.dp),
      colors = CardDefaults.cardColors(containerColor = SlateCard.copy(alpha = 0.85f)),
      border = BorderStroke(1.dp, if (isListening) CyberGreen else CardBorder),
      shape = RoundedCornerShape(12.dp)
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(text = "INTELLIGENT TELEMETRY FEED", color = MutedText, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
          Text(
            text = assistantStatusText,
            color = if (isListening) CyberGreen else Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(top = 2.dp)
          )
        }
        
        Spacer(modifier = Modifier.width(8.dp))

        // Pulse microphone trigger button
        val scale by animateFloatAsState(
          targetValue = if (isListening) 1.25f else 1.0f,
          animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
          label = "ScaleMic"
        )

        Box(
          contentAlignment = Alignment.Center,
          modifier = Modifier
            .size(54.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(
              Brush.radialGradient(
                colors = if (isListening) {
                  listOf(CyberGreen, CyberGreen.copy(alpha = 0.4f), Color.Transparent)
                } else {
                  listOf(CyberCyan, CyberCyan.copy(alpha = 0.15f), Color.Transparent)
                }
              )
            )
            .clickable {
              processVoiceCommand("Execute standard sound analysis")
            }
            .border(1.dp, if (isListening) CyberGreen else CyberCyan, CircleShape)
            .testTag("action_mic_trigger")
        ) {
          Icon(
            imageVector = if (isListening) Icons.Default.Mic else Icons.Default.MicNone,
            contentDescription = "Trigger Speech Check",
            tint = if (isListening) SlateBlack else Color.White,
            modifier = Modifier.size(24.dp)
          )
        }
      }
    }

    // Text sandbox Command Entry Bar
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 4.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      OutlinedTextField(
        value = typedMockCommand,
        onValueChange = { typedMockCommand = it },
        placeholder = { Text("Simulate speaking to Assistant...", fontSize = 12.sp, color = MutedText) },
        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp, color = Color.White, fontFamily = FontFamily.Monospace),
        modifier = Modifier
          .weight(1f)
          .testTag("text_input_voice"),
        shape = RoundedCornerShape(10.dp),
        colors = OutlinedTextFieldDefaults.colors(
          focusedContainerColor = SlateCard,
          unfocusedContainerColor = SlateCard,
          focusedBorderColor = CyberCyan,
          unfocusedBorderColor = CardBorder,
          cursorColor = CyberCyan
        ),
        singleLine = true,
        trailingIcon = {
          if (typedMockCommand.isNotEmpty()) {
            IconButton(onClick = { typedMockCommand = "" }) {
              Icon(Icons.Default.Cancel, contentDescription = "Clear", tint = MutedText, modifier = Modifier.size(16.dp))
            }
          }
        }
      )
      
      Spacer(modifier = Modifier.width(8.dp))

      Button(
        onClick = {
          if (typedMockCommand.isNotBlank()) {
            processVoiceCommand(typedMockCommand)
            typedMockCommand = ""
          }
        },
        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
          .height(54.dp)
          .testTag("button_vocal_submit")
      ) {
        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Speak Command", tint = SlateBlack)
      }
    }

    // Category Tabs selectors for features
    TabRow(
      selectedTabIndex = currentTab,
      containerColor = Color.Transparent,
      contentColor = CyberCyan,
      indicator = { tabPositions ->
        TabRowDefaults.SecondaryIndicator(
          modifier = Modifier.tabIndicatorOffset(tabPositions[currentTab]),
          color = CyberCyan
        )
      },
      divider = { HorizontalDivider(color = CardBorder) },
      modifier = Modifier.padding(vertical = 6.dp)
    ) {
      Tab(
        selected = currentTab == 0,
        onClick = { currentTab = 0 },
        text = { Text("Dashboard", fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold) },
        icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard", modifier = Modifier.size(16.dp)) }
      )
      Tab(
        selected = currentTab == 1,
        onClick = { currentTab = 1 },
        text = { Text("Task Center", fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold) },
        icon = { Icon(Icons.Default.LockOpen, contentDescription = "Tasks", modifier = Modifier.size(16.dp)) }
      )
      Tab(
        selected = currentTab == 2,
        onClick = { currentTab = 2 },
        text = { Text("Hotwords", fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold) },
        icon = { Icon(Icons.Default.Settings, contentDescription = "Hotwords", modifier = Modifier.size(16.dp)) }
      )
    }

    // Dynamic Tab Scopes
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .weight(1f)
    ) {
      when (currentTab) {
        0 -> DashboardTabPane(
          modelProcessingTypeLocal = modelProcessingTypeLocal,
          onModelTypeChange = { modelProcessingTypeLocal = it },
          activeNoiseCancellation = activeNoiseCancellation,
          onAncChange = { activeNoiseCancellation = it },
          simulatedReelMediaPlaying = simulatedReelMediaPlaying,
          lastInstaAction = lastInstaAction,
          onReelToggle = { simulatedReelMediaPlaying = !simulatedReelMediaPlaying },
          onSimulateCommand = { processVoiceCommand(it) }
        )
        1 -> TaskCenterTabPane(
          isLocked = isLocked,
          onLockToggle = { isLocked = it },
          allowVoiceLockUnlock = allowVoiceLockUnlock,
          onAllowUnlockToggle = { allowVoiceLockUnlock = it },
          onExecuteVoiceLockUnlockCmd = { runLockScreenSimulation(it) },
          onTriggerCallMsgCmd = { processVoiceCommand(it) }
        )
        2 -> HotwordsTabPane(
          hotwordsList = defaultHotwords,
          newHotwordInput = newHotwordInput,
          onWordInputChange = { newHotwordInput = it },
          onAddWord = {
            if (newHotwordInput.isNotBlank() && !defaultHotwords.contains(newHotwordInput)) {
              defaultHotwords.add(newHotwordInput)
              newHotwordInput = ""
            }
          },
          onRemoveWord = { defaultHotwords.remove(it) },
          thresholdSensitivityDb = thresholdSensitivityDb,
          onSensitivityChange = { thresholdSensitivityDb = it },
          logsList = assistantLogList
        )
      }
    }
  }
}

// ==================== TAB 0: DASHBOARD & ACTIVE NOISE CANCELLATION ====================
@Composable
fun DashboardTabPane(
  modelProcessingTypeLocal: Boolean,
  onModelTypeChange: (Boolean) -> Unit,
  activeNoiseCancellation: Boolean,
  onAncChange: (Boolean) -> Unit,
  simulatedReelMediaPlaying: Boolean,
  lastInstaAction: String,
  onReelToggle: () -> Unit,
  onSimulateCommand: (String) -> Unit
) {
  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    verticalArrangement = Arrangement.spacedBy(8.dp),
    contentPadding = PaddingValues(vertical = 4.dp)
  ) {
    // Noise cancellation & Privacy speed configuration card
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SlateCard),
        border = BorderStroke(1.dp, CardBorder)
      ) {
        Column(modifier = Modifier.padding(12.dp)) {
          Text(
            text = "HARDWARE & PRIVACY PREFERENCES",
            color = CyberCyan,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(bottom = 8.dp)
          )

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text("Offline Offline-First DSP", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
              Text("Run 100% locally on neural hardware for zero latency limits & private local voice biometrics", color = MutedText, fontSize = 10.sp)
            }
            Switch(
              checked = modelProcessingTypeLocal,
              onCheckedChange = onModelTypeChange,
              colors = SwitchDefaults.colors(
                checkedThumbColor = CyberGreen,
                checkedTrackColor = CyberGreen.copy(alpha = 0.3f),
                uncheckedThumbColor = MutedText,
                uncheckedTrackColor = CardBorder
              )
            )
          }

          HorizontalDivider(color = CardBorder, modifier = Modifier.padding(vertical = 10.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text("Acoustic Phase Inversion (ANC)", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
              Text("Subtracts loud background noise from YouTube/Reels in high volume noise environments", color = MutedText, fontSize = 10.sp)
            }
            Switch(
              checked = activeNoiseCancellation,
              onCheckedChange = onAncChange,
              colors = SwitchDefaults.colors(
                checkedThumbColor = CyberCyan,
                checkedTrackColor = CyberCyan.copy(alpha = 0.3f),
                uncheckedThumbColor = MutedText,
                uncheckedTrackColor = CardBorder
              )
            )
          }
        }
      }
    }

    // Instagram Reels Interactive Sandbox
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SlateCard),
        border = BorderStroke(1.dp, CardBorder)
      ) {
        Column(modifier = Modifier.padding(12.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "SOCIAL FEED OVERLAY SIMULATOR",
              color = CyberViolet,
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Monospace
            )
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(if (simulatedReelMediaPlaying) CyberViolet.copy(alpha = 0.2f) else CardBorder)
                .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
              Text(
                text = if (simulatedReelMediaPlaying) "REEL AUDIO AT 100% VOL" else "REEL MUTED/PAUSED",
                color = if (simulatedReelMediaPlaying) CyberViolet else MutedText,
                fontSize = 8.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
              )
            }
          }

          Text(
            text = "Play reels on Instagram and try instantly saying 'Pause' or 'Next'—the microphone's voice-priority isolation cuts through speaker output immediately.",
            color = MutedText,
            fontSize = 11.sp,
            modifier = Modifier.padding(vertical = 6.dp)
          )

          // State visual of Instagram Overlay
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .background(SlateBlack, RoundedCornerShape(8.dp))
              .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
              .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = if (simulatedReelMediaPlaying) Icons.Default.PlayArrow else Icons.Default.Pause,
              contentDescription = "Status",
              tint = CyberViolet,
              modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(text = "Instagram Reel Player", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
              Text(text = lastInstaAction, color = CyberCyan, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }
            Spacer(modifier = Modifier.weight(1f))
            Button(
              onClick = onReelToggle,
              colors = ButtonDefaults.buttonColors(containerColor = CardBorder),
              contentPadding = PaddingValues(horizontal = 10.dp),
              modifier = Modifier.height(30.dp)
            ) {
              Text(text = if (simulatedReelMediaPlaying) "Mute/Pause" else "Trigger Play", fontSize = 10.sp, color = Color.White)
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          // Quick simulation voice commands buttons
          Text(text = "Trigger Quick Vocal Commands Cut-through:", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(top = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Button(
              onClick = { onSimulateCommand("INSTANT OVERRIDE: Pause Instagram reel") },
              colors = ButtonDefaults.buttonColors(containerColor = SlateBlack),
              border = BorderStroke(1.dp, CyberViolet),
              modifier = Modifier.weight(1f),
              shape = RoundedCornerShape(8.dp)
            ) {
              Text(text = "🎙️ 'Pause Reel'", fontSize = 11.sp, color = CyberViolet, fontWeight = FontWeight.Bold)
            }

            Button(
              onClick = { onSimulateCommand("INSTANT OVERRIDE: Next Instagram reel video") },
              colors = ButtonDefaults.buttonColors(containerColor = SlateBlack),
              border = BorderStroke(1.dp, CyberCyan),
              modifier = Modifier.weight(1f),
              shape = RoundedCornerShape(8.dp)
            ) {
              Text(text = "🎙️ 'Skip Reel'", fontSize = 11.sp, color = CyberCyan, fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }
  }
}

// ==================== TAB 1: ACTIONS & SECURITY ====================
@Composable
fun TaskCenterTabPane(
  isLocked: Boolean,
  onLockToggle: (Boolean) -> Unit,
  allowVoiceLockUnlock: Boolean,
  onAllowUnlockToggle: (Boolean) -> Unit,
  onExecuteVoiceLockUnlockCmd: (String) -> Unit,
  onTriggerCallMsgCmd: (String) -> Unit
) {
  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    verticalArrangement = Arrangement.spacedBy(8.dp),
    contentPadding = PaddingValues(vertical = 4.dp)
  ) {
    // Locked State voice unlock simulation card
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SlateCard),
        border = BorderStroke(1.dp, if (isLocked) CyberOrange else CardBorder)
      ) {
        Column(modifier = Modifier.padding(12.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "VOICE SECURE BIOMETRICS & LOCK CONTROL",
              color = CyberOrange,
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Monospace
            )

            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(if (isLocked) CyberOrange.copy(alpha = 0.2f) else CyberGreen.copy(alpha = 0.2f))
                .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
              Text(
                text = if (isLocked) "DEVICE LOCKED" else "DEVICE UNLOCKED",
                color = if (isLocked) CyberOrange else CyberGreen,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
              )
            }
          }

          Text(
            text = "With Secure Vocal Enclave, you can unlock your device hands-free simply with acoustic biometrics and immediately carry out deep background tasks.",
            color = MutedText,
            fontSize = 11.sp,
            modifier = Modifier.padding(vertical = 6.dp)
          )

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text("Automatic Voice Authentication", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
              Text("Allows acoustic signatures to unlock the locked system safely", color = MutedText, fontSize = 10.sp)
            }
            Switch(
              checked = allowVoiceLockUnlock,
              onCheckedChange = onAllowUnlockToggle,
              colors = SwitchDefaults.colors(
                checkedThumbColor = CyberOrange,
                checkedTrackColor = CyberOrange.copy(alpha = 0.3f),
                uncheckedThumbColor = MutedText,
                uncheckedTrackColor = CardBorder
              )
            )
          }

          HorizontalDivider(color = CardBorder, modifier = Modifier.padding(vertical = 10.dp))

          // Lock Device Simulator Action
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .background(SlateBlack, RoundedCornerShape(8.dp))
              .border(1.dp, if (isLocked) CyberOrange else CardBorder, RoundedCornerShape(8.dp))
              .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
              contentDescription = "Lock state",
              tint = if (isLocked) CyberOrange else CyberGreen,
              modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))

            Column {
              Text(
                text = if (isLocked) "Biometric Lock Screen Engaged" else "System Unlocked",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = if (isLocked) "Testing voice signature bypass..." else "Awaiting daily verbal request...",
                color = MutedText,
                fontSize = 10.sp
              )
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
              onClick = { onLockToggle(!isLocked) },
              colors = ButtonDefaults.buttonColors(containerColor = if (isLocked) CyberGreen else CyberOrange),
              contentPadding = PaddingValues(horizontal = 12.dp),
              modifier = Modifier.height(32.dp)
            ) {
              Text(
                text = if (isLocked) "Unlock MOCK" else "Lock MOCK",
                color = SlateBlack,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }

          if (isLocked) {
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
              modifier = Modifier.fillMaxWidth(),
              color = CyberOrange.copy(alpha = 0.1f),
              border = BorderStroke(1.dp, CyberOrange.copy(alpha = 0.3f)),
              shape = RoundedCornerShape(8.dp)
            ) {
              Column(modifier = Modifier.padding(8.dp)) {
                Text(
                  text = "🔑 RUN VOID UNLOCK SIMULATOR:",
                  color = CyberOrange,
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold,
                  fontFamily = FontFamily.Monospace
                )
                Text(
                  text = "Click to feed a voice command. The system will perform biometric signature validation, safely bypass the lock status, and complete the command:",
                  color = Color.White,
                  fontSize = 10.sp,
                  modifier = Modifier.padding(vertical = 4.dp)
                )

                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                  Button(
                    onClick = { onExecuteVoiceLockUnlockCmd("Unlock and call Mom immediately") },
                    colors = ButtonDefaults.buttonColors(containerColor = SlateBlack),
                    border = BorderStroke(1.dp, CyberOrange),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(2.dp)
                  ) {
                    Text("🎙️ Call Mom (Hands-free)", fontSize = 9.sp, color = CyberOrange)
                  }

                  Button(
                    onClick = { onExecuteVoiceLockUnlockCmd("Unlock and message Brother that I am on my way") },
                    colors = ButtonDefaults.buttonColors(containerColor = SlateBlack),
                    border = BorderStroke(1.dp, CyberCyan),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(2.dp)
                  ) {
                    Text("🎙️ Msg Brother (Lock Screen)", fontSize = 9.sp, color = CyberCyan)
                  }
                }
              }
            }
          }
        }
      }
    }

    // Daily tasks simulator hub
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SlateCard),
        border = BorderStroke(1.dp, CardBorder)
      ) {
        Column(modifier = Modifier.padding(12.dp)) {
          Text(
            text = "DAY-TO-DAY TASK ACTIONS CENTER",
            color = CyberCyan,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(bottom = 8.dp)
          )

          // Call simulation
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clickable { onTriggerCallMsgCmd("Perform secure dial: Call Sister Mary") }
              .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Box(
              modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(CyberGreen.copy(alpha = 0.2f)),
              contentAlignment = Alignment.Center
            ) {
              Icon(Icons.Default.Phone, contentDescription = "Call", tint = CyberGreen, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
              Text("Voice Call Automation", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
              Text("Simulates instant dialing using clear-voice biometric isolation", color = MutedText, fontSize = 10.sp)
            }
            Spacer(modifier = Modifier.weight(1f))
            Icon(Icons.Default.PlayArrow, contentDescription = "Run", tint = MutedText, modifier = Modifier.size(16.dp))
          }

          HorizontalDivider(color = CardBorder, modifier = Modifier.padding(vertical = 6.dp))

          // Messaging simulation
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clickable { onTriggerCallMsgCmd("Send message to Frank: I will arrive in 15 minutes") }
              .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Box(
              modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(CyberCyan.copy(alpha = 0.2f)),
              contentAlignment = Alignment.Center
            ) {
              Icon(Icons.AutoMirrored.Filled.Message, contentDescription = "Message", tint = CyberCyan, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
              Text("Voice Message Synthesis", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
              Text("Dictate text message streams instantly without hands-on typing", color = MutedText, fontSize = 10.sp)
            }
            Spacer(modifier = Modifier.weight(1f))
            Icon(Icons.Default.PlayArrow, contentDescription = "Run", tint = MutedText, modifier = Modifier.size(16.dp))
          }
        }
      }
    }
  }
}

// ==================== TAB 2: HOTWORDS & TELEMETRY LOGS ====================
@Composable
fun HotwordsTabPane(
  hotwordsList: List<String>,
  newHotwordInput: String,
  onWordInputChange: (String) -> Unit,
  onAddWord: () -> Unit,
  onRemoveWord: (String) -> Unit,
  thresholdSensitivityDb: Float,
  onSensitivityChange: (Float) -> Unit,
  logsList: List<CommandLog>
) {
  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    verticalArrangement = Arrangement.spacedBy(8.dp),
    contentPadding = PaddingValues(vertical = 4.dp)
  ) {
    // Hotword settings card
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SlateCard),
        border = BorderStroke(1.dp, CardBorder)
      ) {
        Column(modifier = Modifier.padding(12.dp)) {
          Text(
            text = "CUSTOMIZABLE VOICE HOTWORDS",
            color = CyberCyan,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(bottom = 6.dp)
          )

          Text(
            text = "Add custom words. Our on-device offline spectral analysis guarantees trigger match accuracy even with loud surrounding video noise.",
            color = MutedText,
            fontSize = 10.sp
          )

          // Word chips list
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            hotwordsList.forEach { word ->
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(30.dp))
                  .background(SlateBlack)
                  .border(1.dp, CyberCyan.copy(alpha = 0.5f), RoundedCornerShape(30.dp))
                  .padding(horizontal = 10.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Text(text = "🗣️ $word", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                  Spacer(modifier = Modifier.width(4.dp))
                  Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove Word",
                    tint = CyberOrange,
                    modifier = Modifier
                      .size(12.dp)
                      .clickable { onRemoveWord(word) }
                  )
                }
              }
            }
          }

          // Input field to add
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
          ) {
            OutlinedTextField(
              value = newHotwordInput,
              onValueChange = onWordInputChange,
              placeholder = { Text("Add word (e.g. Jarvis)", fontSize = 11.sp, color = MutedText) },
              modifier = Modifier.weight(1f),
              shape = RoundedCornerShape(8.dp),
              colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = SlateBlack,
                unfocusedContainerColor = SlateBlack,
                focusedBorderColor = CyberCyan,
                unfocusedBorderColor = CardBorder,
                cursorColor = CyberCyan
              ),
              singleLine = true
            )

            Spacer(modifier = Modifier.width(8.dp))

            Button(
              onClick = onAddWord,
              colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.height(52.dp)
            ) {
              Icon(Icons.Default.Add, contentDescription = "Add Hotword", tint = SlateBlack)
            }
          }

          HorizontalDivider(color = CardBorder, modifier = Modifier.padding(vertical = 10.dp))

          // Slider Sensitivity Threshold
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Vocal Trigger Sensitivity Model", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("${thresholdSensitivityDb.toInt()} dB", color = CyberCyan, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
          }
          Text("Lower dB matches quieter voices, higher dB prevents false-positive activation in loud bars/noise.", color = MutedText, fontSize = 10.sp)
          Slider(
            value = thresholdSensitivityDb,
            onValueChange = onSensitivityChange,
            valueRange = 10f..85f,
            colors = SliderDefaults.colors(
              thumbColor = CyberCyan,
              activeTrackColor = CyberCyan,
              inactiveTrackColor = CardBorder
            )
          )
        }
      }
    }

    // Live Execution Telemetry logs
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SlateCard),
        border = BorderStroke(1.dp, CardBorder)
      ) {
        Column(modifier = Modifier.padding(12.dp)) {
          Text(
            text = "LIVE ISOLATION & LATENCY TELEMETRY LOGS",
            color = CyberGreen,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(bottom = 6.dp)
          )

          if (logsList.isEmpty()) {
            Text(
              text = "No triggers recorded yet. Try screaming your command mockups!",
              color = MutedText,
              fontSize = 11.sp,
              textAlign = TextAlign.Center,
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
            )
          } else {
            logsList.forEach { log ->
              Column(
                modifier = Modifier
                  .padding(vertical = 4.dp)
                  .background(SlateBlack, RoundedCornerShape(8.dp))
                  .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
                  .padding(8.dp)
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                      modifier = Modifier
                        .clip(CircleShape)
                        .background(if (log.localProcessed) CyberGreen.copy(alpha = 0.2f) else CyberOrange.copy(alpha = 0.2f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                      Text(
                        text = if (log.localProcessed) "LOCAL WORKLOAD" else "CLOUD SERVER",
                        color = if (log.localProcessed) CyberGreen else CyberOrange,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                      )
                    }
                    
                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                      text = "${log.latencyMs} ms",
                      color = CyberCyan,
                      fontSize = 10.sp,
                      fontWeight = FontWeight.Bold,
                      fontFamily = FontFamily.Monospace
                    )
                  }

                  Text(text = log.timestamp, color = MutedText, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                }

                Text(
                  text = "INPUT: \"${log.commandText}\"",
                  color = Color.White,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.SemiBold,
                  fontFamily = FontFamily.Monospace,
                  modifier = Modifier.padding(top = 4.dp)
                )

                Text(
                  text = "RESOLVED: ${log.responseText}",
                  color = CyberGreen,
                  fontSize = 11.sp,
                  fontFamily = FontFamily.Monospace,
                  modifier = Modifier.padding(top = 2.dp)
                )
              }
            }
          }
        }
      }
    }
  }
}
