package com.example.animeverso.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.animeverso.audio.AnimeAudioManager
import com.example.animeverso.audio.AnimeNarrator
import com.example.animeverso.game.AppScreen
import com.example.animeverso.game.GameItem
import com.example.animeverso.game.GameViewModel
import com.example.animeverso.game.Player
import kotlinx.coroutines.delay

private val Ink = Color(0xFF090A1C)
private val Panel = Color(0xFF171A3B)
private val Cyan = Color(0xFF51E4FF)
private val Pink = Color(0xFFFF4FD8)
private val Gold = Color(0xFFFFD166)
private val Green = Color(0xFF76F7B2)
private val Red = Color(0xFFFF697D)
private val muted = Color(0xFFB5BBDF)
private val avatars = listOf("🍥", "🐉", "🎋", "🏴‍☠️", "⚡", "🌸", "🌀", "🗡️")

@Composable
fun AnimeVersoApp(viewModel: GameViewModel) {
    val state = viewModel.ui
    val context = LocalContext.current
    val preferences = remember { context.getSharedPreferences("animeverso", Context.MODE_PRIVATE) }
    var endpoint by rememberSaveable { mutableStateOf(preferences.getString("voice_endpoint", "") ?: "") }
    var showVoiceSettings by remember { mutableStateOf(false) }
    val narrator = remember(context) { AnimeNarrator(context.applicationContext) }
    val audio = remember { AnimeAudioManager() }

    LaunchedEffect(state.soundEnabled) { audio.setEnabled(state.soundEnabled) }

    DisposableEffect(Unit) { onDispose { narrator.close(); audio.close() } }

    LaunchedEffect(state.soundEnabled, state.screen) {
        if (state.soundEnabled && state.screen in setOf(AppScreen.INTRO, AppScreen.RULES, AppScreen.SETUP)) audio.startOpeningMusic()
        else audio.stopOpeningMusic()
    }
    LaunchedEffect(state.handoffSerial, state.screen) {
        if (state.screen == AppScreen.HANDOFF) {
            audio.playTransition()
            narrator.speak(viewModel.handoffSpeech(), endpoint)
            repeat(10) {
                delay(1_000)
                if (viewModel.ui.screen != AppScreen.HANDOFF) return@LaunchedEffect
                viewModel.tickHandoff()
                if (viewModel.ui.soundEnabled) audio.playTick()
            }
            narrator.stop()
        }
    }
    LaunchedEffect(state.questionSerial, state.screen) {
        if (state.screen == AppScreen.GAME) {
            state.question?.let { narrator.speak(it.text, endpoint) }
            while (viewModel.ui.screen == AppScreen.GAME) {
                delay(1_000)
                if (viewModel.ui.screen != AppScreen.GAME) break
                viewModel.tickQuestion()
                if (viewModel.ui.soundEnabled) audio.playTick()
            }
        }
    }
    LaunchedEffect(state.feedbackSerial, state.screen) {
        if (state.screen == AppScreen.FEEDBACK) {
            if (viewModel.ui.soundEnabled) audio.playAnswer(viewModel.ui.answerWasCorrect)
            var finished = false
            narrator.speak(viewModel.ui.feedbackSpeech, endpoint) { finished = true }
            var elapsed = 0
            while (!finished && elapsed < 8_000) { delay(100); elapsed += 100 }
            viewModel.finishFeedback()
        }
    }
    LaunchedEffect(state.screen, state.winnerName) {
        if (state.screen == AppScreen.FINISHED) {
            val name = state.winnerName
            narrator.speak(if (name != null) "Parabéns, $name! Você venceu o torneio do Animeverso!" else "O torneio terminou!", endpoint)
        }
    }

    Column(
        Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF090A1C), Color(0xFF171635), Color(0xFF0C1028))))
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .imePadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        if (state.screen != AppScreen.INTRO) {
            Header(soundOn = state.soundEnabled, onSound = { viewModel.toggleSound() })
            Spacer(Modifier.height(12.dp))
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                OutlinedButton(onClick = { viewModel.toggleSound() }, shape = CircleShape) { Text(if (state.soundEnabled) "🔊" else "🔇") }
            }
        }
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            when (state.screen) {
                AppScreen.INTRO -> IntroScreen(onStart = viewModel::openRules, onVoice = { showVoiceSettings = true })
                AppScreen.RULES -> RulesScreen(onHome = viewModel::returnHome, onContinue = viewModel::openSetup)
                AppScreen.SETUP -> SetupScreen(viewModel, onStart = { viewModel.startMatch() })
                AppScreen.HANDOFF -> HandoffScreen(state.players.getOrNull(state.currentPlayerIndex), viewModel.playerHandoffPhrase(state.currentPlayerIndex), state.handoffIsFirst, state.handoffSeconds)
                AppScreen.GAME, AppScreen.FEEDBACK -> GameScreen(state.screen == AppScreen.FEEDBACK, state.players, state.currentPlayerIndex, state.question?.category ?: "Anime", state.question?.text.orEmpty(), state.cardNumber, state.secondsLeft, state.answerOptions, state.correctOptionIndex, state.eliminatedOptions, state.selectedOptionIndex, state.banner, state.hasUsedItemThisTurn, onHome = viewModel::returnHome, onAnswer = { viewModel.answer(it) }, onItem = { item -> if (state.soundEnabled) audio.playItem(); viewModel.useItem(item) })
                AppScreen.FINISHED -> ResultScreen(state.winnerName, viewModel.rankedPlayers(), onHome = viewModel::returnHome, onAgain = viewModel::openSetup)
            }
        }
    }

    if (showVoiceSettings) {
        AlertDialog(
            onDismissRequest = { showVoiceSettings = false },
            containerColor = Panel,
            title = { Text("Narração do torneio", color = Cyan, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Use o endereço HTTPS do proxy ElevenLabs. A chave fica no servidor e não deve ser colocada no app.", color = Color.White)
                    OutlinedTextField(value = endpoint, onValueChange = { endpoint = it }, label = { Text("Endpoint HTTPS (opcional)") }, placeholder = { Text("https://seu-dominio/api/voice/generate") }, singleLine = true)
                    Text("Voz: Narrador Animeverso · Herói Shōnen BR", color = Gold, fontSize = 13.sp)
                }
            },
            confirmButton = { TextButton(onClick = {
                val cleaned = endpoint.trim()
                if (cleaned.isEmpty() || cleaned.startsWith("https://", true)) {
                    endpoint = cleaned
                    preferences.edit().putString("voice_endpoint", cleaned).apply()
                    showVoiceSettings = false
                }
            }) { Text("Salvar", color = Cyan) } },
            dismissButton = { TextButton(onClick = { showVoiceSettings = false }) { Text("Cancelar", color = muted) } },
        )
    }
}

@Composable
private fun Header(soundOn: Boolean, onSound: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Column {
            Text("TORNEIO DOS MUNDOS", color = Cyan, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
            Text("ANIMEVERSO", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Black)
        }
        OutlinedButton(onClick = onSound, shape = CircleShape) { Text(if (soundOn) "🔊" else "🔇") }
    }
}

@Composable
private fun IntroScreen(onStart: () -> Unit, onVoice: () -> Unit) {
    val motion = rememberInfiniteTransition(label = "manga-home-motion")
    val drift by motion.animateFloat(-7f, 7f, infiniteRepeatable(tween(1_450, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "hero-drift")
    val pulse by motion.animateFloat(0.96f, 1.04f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "energy-pulse")
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(color = Color(0xFFF2F2F7))) { append("ANIME") }
                withStyle(SpanStyle(color = Color(0xFFFF344F))) { append("VERSO") }
            },
            fontSize = 38.sp, fontWeight = FontWeight.Black, letterSpacing = (-1.7).sp,
        )
        Text("TRIVIA DE ANIME E MANGÁ", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
        Box(
            Modifier.fillMaxWidth().height(340.dp)
                .background(Brush.linearGradient(listOf(Color(0xFF142A54), Color(0xFF44104C), Color(0xFF101020))), RoundedCornerShape(20.dp))
                .padding(2.dp)
                .background(Color(0xFF111225), RoundedCornerShape(18.dp)),
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val centerX = size.width * 0.5f
                val centerY = size.height * 0.56f
                val colors = listOf(Color(0xFF27D8FF), Color(0xFFFF3D9A), Color(0xFFFFC247), Color(0xFF8C67FF))
                for (i in 0 until 22) {
                    val x = size.width * i / 21f
                    val offset = ((i % 5) * 13f) + drift * (if (i % 2 == 0) 1 else -1)
                    drawLine(colors[i % colors.size].copy(alpha = 0.48f), Offset(centerX, centerY), Offset(x, if (i % 2 == 0) 0f else size.height), strokeWidth = if (i % 4 == 0) 3f else 1.2f, cap = StrokeCap.Round)
                    drawCircle(colors[i % colors.size].copy(alpha = 0.45f), radius = 2.5f + (i % 3), center = Offset(x, (offset + size.height * ((i * 17 % 100) / 100f)).coerceIn(8f, size.height - 8f)))
                }
            }
            Column(Modifier.fillMaxSize().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceBetween) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    MangaBadge("⚡ SHŌNEN", Cyan)
                    MangaBadge("✦ AVENTURA", Gold)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                    MangaHero("🥷", "NINJA", Color(0xFFFF4E64), drift)
                    MangaHero("🗡️", "ESPADACHINA", Color(0xFFFF4FCB), -drift)
                    MangaHero("🧙", "MAGA", Color(0xFF37DFFF), drift * 0.7f)
                }
                Text("QUADRO A QUADRO • UNIVERSO A UNIVERSO", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, textAlign = TextAlign.Center)
            }
        }
        Text("Monte sua equipe e prove que conhece os animes que marcaram gerações.", color = Color(0xFFE7E9F7), textAlign = TextAlign.Center, fontSize = 14.sp)
        Button(
            onClick = onStart,
            modifier = Modifier.fillMaxWidth().height(58.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE92242)),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = (8 * pulse).dp),
        ) { Text("▶  JOGAR", fontSize = 23.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp) }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = onVoice, modifier = Modifier.weight(1f)) { Text("⚙ NARRAÇÃO", color = Color.White, fontSize = 11.sp) }
            Text("MANGÁ • ANIME • DESAFIO", modifier = Modifier.weight(1.4f).align(Alignment.CenterVertically), color = Gold, fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun MangaBadge(label: String, color: Color) {
    Text(label, Modifier.background(color.copy(alpha = 0.16f), RoundedCornerShape(8.dp)).padding(horizontal = 9.dp, vertical = 6.dp), color = color, fontSize = 10.sp, fontWeight = FontWeight.Black)
}

@Composable
private fun MangaHero(emoji: String, label: String, accent: Color, offset: Float) {
    Column(
        Modifier.padding(top = offset.dp).background(Brush.verticalGradient(listOf(accent.copy(alpha = 0.40f), Color(0xFF17152B))), RoundedCornerShape(16.dp)).padding(horizontal = 9.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(emoji, fontSize = 48.sp)
        Text(label, color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Black, letterSpacing = 0.5.sp)
    }
}

@Composable
private fun RulesScreen(onHome: () -> Unit, onContinue: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("📜 Regras do torneio", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold)
        RuleCard("⏱️ Turnos e pontuação", listOf("Cada jogador tem 30 segundos para responder.", "Acerto com 21–30 SEGUNDOS: +3 pontos; 11–20 SEGUNDOS: +2; 1–10 SEGUNDOS: +1.", "Erro ou tempo esgotado: −1 ponto (mínimo 0).", "Após cada resposta, a narração anuncia o próximo jogador e há uma transição de 10 segundos."))
        RuleCard("🗺️ Trilha dos mundos", listOf("🚩 Início na etapa zero.", "🌌 Cada resposta certa avança uma etapa.", "⭐ Etapas 5 e 12: treino especial e +3 pontos extras.", "⚔️ Etapas 7 e 13: encontro rival, −3 pontos (mínimo 0).", "👑 Etapa 20: Desafio Final; acerte para vencer."))
        RuleCard("🎒 Técnicas e itens únicos", GameItem.entries.map { "${it.icon} ${it.label}: ${it.description}." } + "Use no máximo uma técnica por turno; cada item pode ser usado uma vez por jogador.")
        RuleCard("🌸 Conhecimento otaku", listOf("Perguntas de animes, mangás, filmes e cultura japonesa de animação.", "As alternativas mudam de ordem a cada partida.", "O banco contém 1.500 perguntas únicas."))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onHome, modifier = Modifier.weight(1f)) { Text("🏠 Início", color = Color.White) }
            PrimaryButton("🥋 Escolher jogadores", onContinue, Modifier.weight(1.6f))
        }
    }
}

@Composable
private fun SetupScreen(vm: GameViewModel, onStart: () -> Unit) {
    val state = vm.ui
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("👥 Monte sua equipe", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold)
        GameCard {
            Column(Modifier.padding(16.dp)) {
                Text("Competidores: ${state.playerCount}", color = Gold, fontWeight = FontWeight.Bold)
                Slider(value = state.playerCount.toFloat(), onValueChange = { vm.setPlayerCount(it.toInt()) }, valueRange = 2f..8f, steps = 5)
                for (i in 0 until state.playerCount) {
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { vm.rotateAvatar(i) }, modifier = Modifier.size(54.dp), shape = RoundedCornerShape(14.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) { Text(state.avatars[i], fontSize = 22.sp) }
                        OutlinedTextField(value = state.playerNames[i], onValueChange = { vm.setPlayerName(i, it) }, label = { Text("Jogador ${i + 1}") }, singleLine = true, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
        PrimaryButton("🚀 Começar o torneio", onStart)
        OutlinedButton(onClick = vm::openRules, modifier = Modifier.fillMaxWidth()) { Text("Voltar às regras", color = Color.White) }
    }
}

@Composable
private fun HandoffScreen(player: Player?, phrase: String, first: Boolean, seconds: Int) {
    GameCard {
        Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("🎙️", fontSize = 58.sp)
            Text(if (first) "PRIMEIRO JOGADOR" else "PRÓXIMO JOGADOR", color = Cyan, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
            Text("${player?.avatar.orEmpty()} ${player?.name.orEmpty()}", color = Gold, fontSize = 29.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
            Text(phrase, color = Color.White, fontSize = 18.sp, textAlign = TextAlign.Center)
            Text("A pergunta começa em", color = muted)
            Text("$seconds SEGUNDOS", color = Pink, fontSize = 28.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun GameScreen(feedbackMode: Boolean, players: List<Player>, currentIndex: Int, category: String, question: String, card: Int, seconds: Int, options: List<String>, correctIndex: Int, eliminated: Set<Int>, selected: Int, banner: String, usedItem: Boolean, onHome: () -> Unit, onAnswer: (Int) -> Unit, onItem: (GameItem) -> Unit) {
    val player = players.getOrNull(currentIndex) ?: return
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = onHome) { Text("🏠 Início", color = Color.White) }
            Text("Etapa ${if (player.finalChallenge) "FINAL" else player.position + 1}/20", color = Gold, fontWeight = FontWeight.Bold)
            Text("⏱ $seconds s", color = if (seconds <= 8) Red else Cyan, fontWeight = FontWeight.Black)
        }
        Text("Vez de ${player.avatar} ${player.name}", color = Gold, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        ScoreTrack(players)
        if (banner.isNotBlank()) Text(banner, color = Gold, fontSize = 13.sp)
        GameCard {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("${if (player.finalChallenge) "👑 DESAFIO FINAL" else "🎴 $category"}  ·  RODADA $card", color = Cyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text(question, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                options.forEachIndexed { index, option ->
                    val disabled = feedbackMode || eliminated.contains(index) || selected >= 0
                    val background = when {
                        feedbackMode && index == correctIndex -> Green
                        selected == index -> Red
                        eliminated.contains(index) -> Color(0xFF30324B)
                        else -> Color(0xFF24274D)
                    }
                    Button(onClick = { onAnswer(index) }, enabled = !disabled, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = background, disabledContainerColor = background, contentColor = Ink, disabledContentColor = Color.White), shape = RoundedCornerShape(12.dp)) {
                        Text(option, modifier = Modifier.fillMaxWidth(), fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
        Text("🎒 Arsenal · limite de uma técnica por turno", color = Gold, fontWeight = FontWeight.Bold)
        GameItem.entries.forEach { item ->
            val locked = player.finalChallenge || player.blockItems || usedItem || item in player.usedItems || selected >= 0
            OutlinedButton(onClick = { onItem(item) }, enabled = !locked, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Text("${item.icon} ${item.label}  ·  ${item.description}", color = if (locked) muted else Color.White, fontSize = 13.sp)
            }
        }
        if (selected >= 0) Text("⏳ Resposta registrada. A próxima vez começa após a narração e a transição.", color = Cyan, textAlign = TextAlign.Center)
    }
}

@Composable
private fun ScoreTrack(players: List<Player>) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("TRILHA DO ANIMEVERSO", color = Cyan, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp, letterSpacing = 1.5.sp)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            for (step in 0..20) {
                val occupant = players.filter { !it.eliminated && it.position == step }.joinToString("") { it.avatar }
                Surface(shape = RoundedCornerShape(8.dp), color = when (step) { 20 -> Color(0xFF3A2651); 5, 12 -> Color(0xFF433B27); else -> Panel }) {
                    Column(Modifier.size(width = 52.dp, height = 48.dp).padding(2.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Text(if (step == 0) "INÍCIO" else if (step == 20) "👑" else if (step == 5 || step == 12) "⭐" else if (step == 7 || step == 13) "⚔️" else step.toString(), color = Gold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(occupant.ifBlank { "·" }, fontSize = 11.sp)
                    }
                }
            }
        }
        Text(players.joinToString("   ") { "${it.avatar} ${it.name}: ${it.score} pts${if (it.eliminated) " · eliminado" else ""}" }, color = muted, fontSize = 11.sp)
    }
}

@Composable
private fun ResultScreen(winner: String?, players: List<Player>, onHome: () -> Unit, onAgain: () -> Unit) {
    GameCard {
        Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("🏆", fontSize = 60.sp)
            Text(if (winner != null) "Campeão do Animeverso!" else "Fim do torneio", color = Gold, fontSize = 26.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
            winner?.let { Text("${players.firstOrNull { p -> p.name == it }?.avatar.orEmpty()} $it venceu o Desafio Final!", color = Cyan, textAlign = TextAlign.Center) }
            players.forEachIndexed { index, player -> Text("${index + 1}. ${player.avatar} ${player.name} · ${player.score} pts · ${player.position}/20", color = Color.White) }
            PrimaryButton("🔄 Jogar novamente", onAgain)
            OutlinedButton(onClick = onHome) { Text("🏠 Voltar ao início", color = Color.White) }
        }
    }
}

@Composable
private fun RuleCard(title: String, lines: List<String>) {
    GameCard {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(title, color = Cyan, fontWeight = FontWeight.Bold)
            lines.forEach { Text("•  $it", color = Color.White, fontSize = 13.sp) }
        }
    }
}

@Composable
private fun GameCard(content: @Composable () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(18.dp), elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)) { content() }
}

@Composable
private fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(onClick = onClick, modifier = modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Ink), shape = RoundedCornerShape(13.dp)) {
        Text(text, fontWeight = FontWeight.Black, modifier = Modifier.padding(vertical = 4.dp))
    }
}

