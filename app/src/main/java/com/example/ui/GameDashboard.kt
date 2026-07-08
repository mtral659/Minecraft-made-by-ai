package com.example.ui

import android.widget.Space
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.BlockType
import com.example.data.WorldEntity
import com.example.game.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.absoluteValue
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameDashboard(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val worlds by viewModel.allWorlds.collectAsStateWithLifecycle(initialValue = emptyList())
    var showCreateDialog by remember { mutableStateOf(false) }

    val activeWorld = viewModel.activeWorld

    if (activeWorld == null) {
        // MAIN MENU TITLE SCREEN
        WorldSelectMenu(
            worlds = worlds,
            onWorldSelected = { viewModel.loadWorldSession(it.id) },
            onWorldDelete = { viewModel.deleteWorld(it.id) },
            onCreateClicked = { showCreateDialog = true }
        )

        if (showCreateDialog) {
            WorldCreateDialog(
                onDismiss = { showCreateDialog = false },
                onCreated = { name, seedInput, creative ->
                    val seed = if (seedInput.isEmpty()) Random().nextLong() else seedInput.toLongOrNull() ?: seedInput.hashCode().toLong()
                    viewModel.startNewWorld(name, seed, creative)
                    showCreateDialog = false
                }
            )
        }
    } else {
        // ACTIVE GAME SESSIONS
        VoxelGameplayScreen(viewModel = viewModel, world = activeWorld)
    }
}

@Composable
fun WorldSelectMenu(
    worlds: List<WorldEntity>,
    onWorldSelected: (WorldEntity) -> Unit,
    onWorldDelete: (WorldEntity) -> Unit,
    onCreateClicked: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                // Beautiful retro pixel sky backdrop
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF2196F3), // sky blue
                            Color(0xFF80DEEA), // light bluecyan
                            Color(0xFFFFCC80)  // warm golden orange
                        )
                    )
                )

                // Render dynamic floating blocky clouds
                drawRect(Color(0xE6FFFFFF), Offset(100f, 200f), androidx.compose.ui.geometry.Size(300f, 120f))
                drawRect(Color(0x99FFFFFF), Offset(500f, 150f), androidx.compose.ui.geometry.Size(400f, 160f))
                drawRect(Color(0xB3FFFFFF), Offset(900f, 280f), androidx.compose.ui.geometry.Size(250f, 100f))
            }
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // GAME LOGO BANNER
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 40.dp)
            ) {
                Box(
                    modifier = Modifier
                        .shadow(12.dp, shape = RoundedCornerShape(12.dp))
                        .background(Color(0xFF3E2723), shape = RoundedCornerShape(12.dp))
                        .border(4.dp, Color(0xFF4CAF50), shape = RoundedCornerShape(12.dp))
                        .padding(horizontal = 32.dp, vertical = 20.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "VOXEL CRAFT",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 38.sp,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "3D Pocket Block Sandbox",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                            color = Color(0xFFAED581),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // WORLDS SELECTION LIST
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xCC1E1E1E)),
                border = BorderStroke(2.dp, Color(0x66FFFFFF)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "SAVED VOXEL WORLDS",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE0E0E0),
                        fontSize = 16.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    if (worlds.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = Color.Gray,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "No worlds created yet.\nClick 'New World' to start your first voxel creation!",
                                    fontFamily = FontFamily.Monospace,
                                    color = Color.Gray,
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(worlds) { world ->
                                WorldRow(
                                    world = world,
                                    onPlayClick = { onWorldSelected(world) },
                                    onDeleteClick = { onWorldDelete(world) }
                                )
                            }
                        }
                    }
                }
            }

            // MAIN BUTTON CONTROLS
            Button(
                onClick = onCreateClicked,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
                    .height(60.dp)
                    .testTag("create_world_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(3.dp, Color(0xFF388E3C))
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "NEW WORLD sandbox",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun WorldRow(
    world: WorldEntity,
    onPlayClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val df = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    val formattedDate = df.format(Date(world.lastPlayedTime))

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF2E2E2E))
            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(8.dp))
            .clickable { onPlayClick() }
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = world.name,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Seed: ${world.seed} | Mode: ${world.gameMode}",
                    fontFamily = FontFamily.Monospace,
                    color = Color.LightGray,
                    fontSize = 12.sp
                )
                Text(
                    text = "Last Played: $formattedDate",
                    fontFamily = FontFamily.Monospace,
                    color = Color.Gray,
                    fontSize = 11.sp
                )
            }

            Row {
                IconButton(
                    onClick = onPlayClick,
                    modifier = Modifier
                        .background(Color(0xFF4CAF50), shape = CircleShape)
                        .size(36.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier
                        .background(Color(0xFFD84315), shape = CircleShape)
                        .size(36.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorldCreateDialog(
    onDismiss: () -> Unit,
    onCreated: (String, String, Boolean) -> Unit
) {
    var name by remember { mutableStateOf("My Minecraft World") }
    var seed by remember { mutableStateOf("") }
    var creativeMode by remember { mutableStateOf(true) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2D2D2D)),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(2.dp, Color(0xFF4CAF50))
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "CREATE VOXEL WORLD",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 20.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // NAME FIELD
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("World Name", color = Color.Gray, fontFamily = FontFamily.Monospace) },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.White, fontFamily = FontFamily.Monospace),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF4CAF50),
                        unfocusedBorderColor = Color.LightGray
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // SEED FIELD
                OutlinedTextField(
                    value = seed,
                    onValueChange = { seed = it },
                    label = { Text("Seed (Optional Number)", color = Color.Gray, fontFamily = FontFamily.Monospace) },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.White, fontFamily = FontFamily.Monospace),
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Random seed if empty", color = Color.DarkGray, fontFamily = FontFamily.Monospace) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF4CAF50),
                        unfocusedBorderColor = Color.LightGray
                    )
                )

                Spacer(modifier = Modifier.height(20.dp))

                // CHOOSE MODE
                Text(
                    text = "SELECT GAME MODE",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { creativeMode = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (creativeMode) Color(0xFF4CAF50) else Color(0xFF3E3E3E)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("CREATIVE", fontFamily = FontFamily.Monospace, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { creativeMode = false },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!creativeMode) Color(0xFF4CAF50) else Color(0xFF3E3E3E)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("SURVIVAL", fontFamily = FontFamily.Monospace, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = if (creativeMode) 
                        "Creative: Unlimited blocks, camera flight, no health/hunger restrictions, peaceful mining."
                    else
                        "Survival: Gather resources, craft pickaxes, manage hunger/health, and survive dark nights from hostile mobs!",
                    fontFamily = FontFamily.Monospace,
                    color = Color.LightGray,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                // ACTION BUTTONS
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("CANCEL", color = Color.Red, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { onCreated(name, seed, creativeMode) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("GENERATE", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// MAIN ACTIVE GAME SCREEN
@Composable
fun VoxelGameplayScreen(
    viewModel: GameViewModel,
    world: WorldEntity
) {
    val activeGameMode = viewModel.activeGameMode
    val activePanel = viewModel.activePanel

    // Touch panning state
    var preDragX by remember { mutableStateOf(0f) }
    var preDragY by remember { mutableStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // SKY GRADIENT BACKGROUND BASED ON TIMER
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    val ticks = viewModel.dayCycleTicks
                    // Resolve color based on clock range:
                    // Day (0-45s): Sky Blue
                    // Sunset (45-55s): Soft Golden purple
                    // Night (55-105s): Deep Dark Blue with stars
                    // Dawn (105-120s): Soft golden peach
                    val skyColor = when {
                        ticks < 45f -> Color(0xFF4FC3F7) // Bright Sky Blue
                        ticks < 55f -> {
                            val ratio = (ticks - 45f) / 10f
                            lerp(Color(0xFF4FC3F7), Color(0xFF7E57C2), ratio) // Purple blend
                        }
                        ticks < 105f -> {
                            Color(0xFF1A1F3C) // Dark Cosmic Blue
                        }
                        else -> {
                            val ratio = (ticks - 105f) / 15f
                            lerp(Color(0xFF1A1F3C), Color(0xFF4FC3F7), ratio) // Morning pink sky
                        }
                    }

                    drawRect(skyColor)

                    // Draw Pixel Stars at night
                    if (ticks >= 53f && ticks <= 107f) {
                        val starColor = Color.White.copy(alpha = if (ticks < 60f) (ticks - 53f)/7f else if (ticks > 100f) (107f - ticks)/7f else 1.0f)
                        val r = Random(12345L)
                        for (i in 0..40) {
                            val starX = r.nextFloat() * size.width
                            val starY = r.nextFloat() * size.height * 0.6f
                            val starSize = r.nextFloat() * 4f + 2f
                            drawRect(starColor, Offset(starX, starY), androidx.compose.ui.geometry.Size(starSize, starSize))
                        }
                    }

                    // Render Celestial Sun and Moon moving across
                    val angle = (ticks / 120f) * 2 * Math.PI - Math.PI / 2
                    val centerX = size.width / 2
                    val centerY = size.height * 0.5f
                    val pathRadius = size.width * 0.45f

                    val sunX = (centerX + pathRadius * cos(angle)).toFloat()
                    val sunY = (centerY + pathRadius * sin(angle)).toFloat()

                    // Moon is opposite to the Sun
                    val moonX = (centerX - pathRadius * cos(angle)).toFloat()
                    val moonY = (centerY - pathRadius * sin(angle)).toFloat()

                    // Draw Retro Yellow Golden Square Sun
                    if (sunY < size.height * 0.8f) {
                        drawRect(
                            color = Color(0xFFFFEB3B),
                            topLeft = Offset(sunX - 25f, sunY - 25f),
                            size = androidx.compose.ui.geometry.Size(50f, 50f)
                        )
                    }

                    // Draw White Square Moon
                    if (moonY < size.height * 0.8f) {
                        drawRect(
                            color = Color(0xFFECEFF1),
                            topLeft = Offset(moonX - 20f, moonY - 20f),
                            size = androidx.compose.ui.geometry.Size(40f, 40f)
                        )
                    }
                }
                .pointerInput(Unit) {
                    // Let user double-finger drag or single-finger drag to pan the camera around
                    detectDragGestures(
                        onDragStart = { offset ->
                            preDragX = offset.x
                            preDragY = offset.y
                        },
                        onDrag = { change, dragAmount ->
                            viewModel.cameraPanX += dragAmount.x
                            viewModel.cameraPanY += dragAmount.y
                        }
                    )
                }
        ) {
            // VOXEL CANVAS ENGINE
            VoxelCanvasEngine(viewModel = viewModel)

            // FULL-SCREEN DAMAGE BLOW FLASH RED INDICATOR
            if (viewModel.hurtOverlayAlpha > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Red.copy(alpha = viewModel.hurtOverlayAlpha))
                )
            }

            // HUD OVERLAYS
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // TOP BAR: Title, modes toggles, Save Game
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = world.name,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 18.sp,
                            modifier = Modifier.shadow(4.dp)
                        )
                        Text(
                            text = if (viewModel.isNight()) "🌙 Night | Hostile Mobs Spawned" else "☀️ Daytime | Safe Construction",
                            fontFamily = FontFamily.Monospace,
                            color = if (viewModel.isNight()) Color(0xFFFFB74D) else Color(0xFFAED581),
                            fontSize = 12.sp,
                            modifier = Modifier.shadow(4.dp)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = { viewModel.activePanel = "help" },
                            modifier = Modifier.background(Color(0xCC000000), CircleShape)
                        ) {
                            Icon(Icons.Default.Info, contentDescription = "Help Guide", tint = Color.White)
                        }
                        
                        Button(
                            onClick = { viewModel.saveAndExit() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD84315)),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(2.dp, Color.White.copy(alpha = 0.5f))
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ExitToApp, contentDescription = "Exit", tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("SAVE & EXIT", fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // HEALTH & HUNGER INDICATORS IN SURVIVAL MODE
                if (activeGameMode == "SURVIVAL") {
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(top = 48.dp)
                    ) {
                        // HEALTH HEARTS row
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("HP ", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 11.sp)
                            for (i in 0 until 10) {
                                Icon(
                                    imageVector = Icons.Default.Favorite,
                                    contentDescription = "Heart",
                                    tint = if (i < viewModel.playerHealth) Color.Red else Color.DarkGray,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Hunger Row
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("FOOD", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 11.sp)
                            Spacer(modifier = Modifier.width(2.dp))
                            for (i in 0 until 10) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "Food",
                                    tint = if (i < viewModel.playerHunger) Color(0xFFFFB74D) else Color.DarkGray,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }
                }

                // UPPER RIGHT: ZOOM & ROTATE CAMERA BUTTON_TRIGGERS
                Column(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 48.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    IconButton(
                        onClick = { viewModel.cameraRotation = (viewModel.cameraRotation + 90) % 360 },
                        modifier = Modifier
                            .size(44.dp)
                            .background(Color(0xCC000000), RoundedCornerShape(8.dp))
                            .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Rotate", tint = Color.White)
                    }

                    IconButton(
                        onClick = { viewModel.cameraZoom = (viewModel.cameraZoom + 0.15f).coerceAtMost(2.5f) },
                        modifier = Modifier
                            .size(44.dp)
                            .background(Color(0xCC000000), RoundedCornerShape(8.dp))
                            .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Zoom In", tint = Color.White)
                    }

                    IconButton(
                        onClick = { viewModel.cameraZoom = (viewModel.cameraZoom - 0.15f).coerceAtLeast(0.4f) },
                        modifier = Modifier
                            .size(44.dp)
                            .background(Color(0xCC000000), RoundedCornerShape(8.dp))
                            .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                    ) {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Zoom Out", tint = Color.White)
                    }

                    IconButton(
                        onClick = {
                            viewModel.cameraPanX = 0f
                            viewModel.cameraPanY = 0f
                            viewModel.cameraZoom = 1.0f
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .background(Color(0xCC000000), RoundedCornerShape(8.dp))
                            .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                    ) {
                        Icon(Icons.Default.Place, contentDescription = "Reset Center", tint = Color.White)
                    }
                }

                // CHAT LOGS LOGWINDOW (Bottom Right)
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 140.dp)
                        .width(180.dp)
                        .background(Color(0x99000000), RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    Text(
                        text = "WORLD TRANSCRIPTS",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFAED581),
                        fontSize = 10.sp,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    viewModel.gameLogs.value.take(4).forEach { log ->
                        Text(
                            text = "> $log",
                            fontFamily = FontFamily.Monospace,
                            color = Color.White,
                            fontSize = 9.sp,
                            lineHeight = 10.sp,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }
                }

                // BOTTOM PANELS: CONTROLLER STAYS OPEN (D-PAD & JUMP & INVENTORY ACTIONS)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .height(130.dp)
                ) {
                    // D-PAD CONTROLLER (Bottom Left)
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .align(Alignment.BottomStart)
                            .background(Color(0x80222222), CircleShape)
                    ) {
                        // UP/NORTH
                        IconButton(
                            onClick = { viewModel.movePlayer(0f, -0.4f) },
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .size(36.dp)
                        ) {
                            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Walk North", tint = Color.White)
                        }

                        // SOUTH
                        IconButton(
                            onClick = { viewModel.movePlayer(0f, 0.4f) },
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .size(36.dp)
                        ) {
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Walk South", tint = Color.White)
                        }

                        // WEST
                        IconButton(
                            onClick = { viewModel.movePlayer(-0.4f, 0f) },
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .size(36.dp)
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Walk West", tint = Color.White)
                        }

                        // EAST
                        IconButton(
                            onClick = { viewModel.movePlayer(0.4f, 0f) },
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .size(36.dp)
                        ) {
                            Icon(Icons.Default.ArrowForward, contentDescription = "Walk East", tint = Color.White)
                        }

                        // Center core indicators
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(Color.White.copy(alpha = 0.3f), CircleShape)
                                .align(Alignment.Center)
                        )
                    }

                    // CENTRAL ACTION DOCK: HOTBAR, INVENTORY ACTIONS
                    Column(
                        modifier = Modifier
                            .width(180.dp)
                            .align(Alignment.BottomCenter),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // HOTBAR STRIP / QUICKSELECT
                        GameHotbarStrip(viewModel = viewModel)

                        Spacer(modifier = Modifier.height(4.dp))

                        // ACTIONS BAR FOR CHEST & TABLE
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { viewModel.activePanel = "inventory" },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3E2723)),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Menu, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("BAG", fontFamily = FontFamily.Monospace, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Button(
                                onClick = { viewModel.activePanel = "crafting" },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF43A047)),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Build, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("CRAFT", fontFamily = FontFamily.Monospace, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // JUMP BUTTON & MOB DETECTOR (Bottom Right)
                    Column(
                        modifier = Modifier.align(Alignment.BottomEnd),
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (viewModel.playerInventory.value.containsKey(109)) {
                            // Eat apple button
                            Button(
                                onClick = { viewModel.eatApple() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Text("🍎 EAT APPLE", fontFamily = FontFamily.Monospace, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Jump
                            IconButton(
                                onClick = { viewModel.jumpPlayer() },
                                modifier = Modifier
                                    .size(54.dp)
                                    .background(Color(0xCC000000), CircleShape)
                                    .border(2.dp, Color.White, CircleShape)
                            ) {
                                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Jump", tint = Color.White, modifier = Modifier.size(32.dp))
                            }
                        }
                    }
                }
            }
        }

        // FLOATING ACTION OVERLAY PANEL SLIDES
        AnimatedVisibility(
            visible = activePanel != "none",
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x80000000))
                    .clickable { viewModel.activePanel = "none" }, // click outside closes
                contentAlignment = Alignment.BottomCenter
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.65f)
                        .clickable(enabled = false) {}, // prevent click-through
                    color = Color(0xFF1E1E1E),
                    border = BorderStroke(2.dp, Color(0xFF4CAF50)),
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(20.dp)
                            .fillMaxSize()
                    ) {
                        // HEADER
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = activePanel.uppercase(Locale.getDefault()) + " INTERNALS",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 18.sp
                            )
                            IconButton(
                                onClick = { viewModel.activePanel = "none" },
                                modifier = Modifier.background(Color(0x33FFFFFF), CircleShape)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                            }
                        }

                        // BODY RESOLVER FOR MODALS
                        when (activePanel) {
                            "help" -> HelpGuideView()
                            "inventory" -> SurvivalBagView(viewModel = viewModel)
                            "crafting" -> CraftingTableView(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }
}

// SURVIVAL INVENTORY BAG SHEET
@Composable
fun SurvivalBagView(viewModel: GameViewModel) {
    val inv = viewModel.playerInventory.value
    val creative = viewModel.activeGameMode == "CREATIVE"

    Column(modifier = Modifier.fillMaxSize()) {
        if (creative) {
            Text(
                text = "Creative Mode has unlocked unlimited block placement reserves: Select any block directly below to add to your placement hotbar!",
                fontFamily = FontFamily.Monospace,
                color = Color.LightGray,
                fontSize = 11.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            val fullBlockSet = BlockType.values().filter { it != BlockType.AIR }
            
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(fullBlockSet) { block ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF2E2E2E), RoundedCornerShape(8.dp))
                            .border(
                                2.dp, 
                                if (viewModel.selectedHotbarItem == block.id) Color(0xFF4CAF50) else Color.Transparent, 
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { 
                                viewModel.selectedHotbarItem = block.id 
                                viewModel.addLog("Hotbar bound: ${block.displayName}")
                            }
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = block.displayName,
                            color = Color.White,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            Text(
                text = "Breaking blocks and defeating night mobs stores resources here. Select an item below to bind it to your active build hotbar!",
                fontFamily = FontFamily.Monospace,
                color = Color.LightGray,
                fontSize = 11.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            if (inv.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Your bag inventory is empty. Mine wood, dirt, or cobblestones to gather items!", fontFamily = FontFamily.Monospace, color = Color.Gray, fontSize = 12.sp, textAlign = TextAlign.Center)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(inv.toList()) { pair ->
                        val item = InventoryItem.fromId(pair.first)
                        val qty = pair.second

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF2E2E2E), RoundedCornerShape(8.dp))
                                .border(
                                    2.dp, 
                                    if (viewModel.selectedHotbarItem == item.id) Color(0xFF4CAF50) else Color.Transparent, 
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { 
                                    viewModel.selectedHotbarItem = item.id 
                                    viewModel.addLog("Hotbar bound: ${item.name}")
                                }
                                .padding(12.dp)
                        ) {
                            Column {
                                Text(
                                    text = item.name,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = "Quantity: x$qty",
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFFAED581),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// CRAFTING TABLE VIEWS
@Composable
fun CraftingTableView(viewModel: GameViewModel) {
    val recipes = CraftingRecipes.recipes

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Combine base resources to craft advanced construction tools or items. Pickaxes unlock mining high-tier ores like Iron and Diamonds!",
            fontFamily = FontFamily.Monospace,
            color = Color.LightGray,
            fontSize = 11.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(recipes) { recipe ->
                CraftingRecipeRow(
                    recipe = recipe,
                    onCraftClick = { viewModel.craftItem(recipe) },
                    hasResources = recipe.ingredients.all { p -> 
                        (viewModel.playerInventory.value[p.first] ?: 0) >= p.second 
                    },
                    currentInventory = viewModel.playerInventory.value
                )
            }
        }
    }
}

@Composable
fun CraftingRecipeRow(
    recipe: CraftingRecipe,
    onCraftClick: () -> Unit,
    hasResources: Boolean,
    currentInventory: Map<Int, Int>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2B2B)),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, if (hasResources) Color(0xFF4CAF50) else Color(0x33FFFFFF))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "${recipe.outputCount}x ${recipe.name}",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 13.sp
                    )
                    Text(
                        text = recipe.description,
                        fontFamily = FontFamily.Monospace,
                        color = Color.Gray,
                        fontSize = 10.sp,
                        lineHeight = 11.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                Button(
                    onClick = onCraftClick,
                    enabled = hasResources,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50),
                        disabledContainerColor = Color(0xFF3A3A3A)
                    ),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text(
                        text = "CRAFT", 
                        fontFamily = FontFamily.Monospace, 
                        fontSize = 10.sp, 
                        fontWeight = FontWeight.Bold,
                        color = if (hasResources) Color.White else Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Ingredients
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Ingredients: ", fontFamily = FontFamily.Monospace, color = Color.Gray, fontSize = 9.sp)
                recipe.ingredients.forEach { p ->
                    val ingItem = InventoryItem.fromId(p.first)
                    val req = p.second
                    val owned = currentInventory[p.first] ?: 0
                    
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF1E1E1E), RoundedCornerShape(4.dp))
                            .padding(4.dp)
                    ) {
                        Text(
                            text = "${ingItem.name} ($owned/$req)",
                            fontFamily = FontFamily.Monospace,
                            color = if (owned >= req) Color(0xFFAED581) else Color(0xFFFF5252),
                            fontSize = 9.sp
                        )
                    }
                }
            }
        }
    }
}

// USER METHOD GUIDE VIEWS
@Composable
fun HelpGuideView() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(androidx.compose.foundation.rememberScrollState())
    ) {
        GuideSection(
            title = "1. BUILDING & PLACING",
            content = "Toggle BUILD mode. Select blocks from your Hotbar. Tap on any visible block's TOP/LEFT/RIGHT faces to place your chosen block instantly adjacent!"
        )
        GuideSection(
            title = "2. MINING & EXCAVATING",
            content = "To break blocks: double-tap or click directly on them with BREAK mode selected. Gathering stone, coal ores, and precious metals requires crafting sturdy Pickaxe toolsets!"
        )
        GuideSection(
            title = "3. EXPLOSIVE TNT CHIPS",
            content = "Place a TNT block from your reserves. Click break on the planted TNT block to ignite its ticking wick! Safely run away to avoid caught in the devastating explosions!"
        )
        GuideSection(
            title = "4. SURVIVING THE MOB NIGHTS",
            content = "Night periods (dark skies) will spawn crawling Zombie, Spider, and Creeper threats. Move player around using D-Pad to maintain distance, or tap directly on the monsters to strike them dead!"
        )
    }
}

@Composable
fun GuideSection(title: String, content: String) {
    Column(modifier = Modifier.padding(bottom = 12.dp)) {
        Text(
            text = title,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFAED581),
            fontSize = 12.sp
        )
        Text(
            text = content,
            fontFamily = FontFamily.Monospace,
            color = Color.LightGray,
            fontSize = 11.sp,
            lineHeight = 13.sp,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
fun GameHotbarStrip(viewModel: GameViewModel) {
    val modes = listOf("PLACE", "BREAK")
    var selectedPlayerAction by remember { mutableStateOf("PLACE") }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Toggles between Build and Destroy Mode
        Row(
            modifier = Modifier
                .background(Color(0xCC000000), RoundedCornerShape(6.dp))
                .padding(2.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            modes.forEach { mode ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (selectedPlayerAction == mode) Color(0xFF4CAF50) else Color.Transparent)
                        .clickable { selectedPlayerAction = mode }
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = mode,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Selected Hotbar visual
        val selectedItemName = InventoryItem.fromId(viewModel.selectedHotbarItem).name
        Box(
            modifier = Modifier
                .shadow(6.dp)
                .background(Color(0xE61E1E1E), RoundedCornerShape(6.dp))
                .border(2.dp, Color(0xFF4CAF50), RoundedCornerShape(6.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                text = "ACTION: $selectedPlayerAction ${selectedItemName.uppercase()}",
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }

        // We bind the state of action mode onto the global view click resolver so clicking the blocks executes it
        LaunchedEffect(selectedPlayerAction) {
            GlobalActionState.currentAction = selectedPlayerAction
        }
    }
}

// STATIC REGISTER FOR GAME ACTION MODES
object GlobalActionState {
    var currentAction by mutableStateOf("PLACE")
}

// 3D ISOMETRIC CHUNK VIEW ENGINE
@Composable
fun VoxelCanvasEngine(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp

    val seed = viewModel.activeWorld?.seed ?: 123456L
    val blocksToBuild = viewModel.modifiedBlocks.value
    val playerX = viewModel.playerX
    val playerY = viewModel.playerY
    val playerZ = viewModel.playerZ

    val zoom = viewModel.cameraZoom
    val panX = viewModel.cameraPanX
    val panY = viewModel.cameraPanY
    val rotation = viewModel.cameraRotation // 0, 90, 180, 270

    val mList = viewModel.activeMobs.value
    val pList = viewModel.activeParticles.value

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .testTag("game_field_canvas")
    ) {
        val cx = size.width / 2 + panX
        val cy = size.height * 0.45f + panY // slightly center-upper default shift
        
        val baseBlockW = 32f * zoom
        val baseBlockH = 16f * zoom
        val baseBlockD = 18f * zoom

        // 3D Depth Sorting Iterative Painter's Algorithm
        // Loop coordinates from background corner to foreground corner
        // Render order based on camera rotation angle
        when (rotation) {
            0 -> {
                for (z in 0..15) {
                    for (x in 0..15) {
                        for (y in 0..15) {
                            drawIsometricGridCell(x, y, z, cx, cy, baseBlockW, baseBlockH, baseBlockD, viewModel, mList)
                        }
                    }
                }
            }
            90 -> {
                for (z in 0..15) {
                    for (y in 0..15) {
                        for (x in 15 downTo 0) {
                            drawIsometricGridCell(x, y, z, cx, cy, baseBlockW, baseBlockH, baseBlockD, viewModel, mList)
                        }
                    }
                }
            }
            180 -> {
                for (z in 0..15) {
                    for (x in 15 downTo 0) {
                        for (y in 15 downTo 0) {
                            drawIsometricGridCell(x, y, z, cx, cy, baseBlockW, baseBlockH, baseBlockD, viewModel, mList)
                        }
                    }
                }
            }
            270 -> {
                for (z in 0..15) {
                    for (y in 15 downTo 0) {
                        for (x in 0..15) {
                            drawIsometricGridCell(x, y, z, cx, cy, baseBlockW, baseBlockH, baseBlockD, viewModel, mList)
                        }
                    }
                }
            }
        }

        // Render floating temporary FX particles
        pList.forEach { p ->
            // project particles
            val pu = (p.x - p.y) * baseBlockW / 2
            val pv = (p.x + p.y) * baseBlockH / 2 - (p.z - 0.5f) * baseBlockD
            val px = cx + pu
            val py = cy + pv

            drawRect(
                color = Color(p.color).copy(alpha = p.life),
                topLeft = Offset(px - p.size/2, py - p.size/2),
                size = androidx.compose.ui.geometry.Size(p.size, p.size)
            )
        }
    }
}

// DRAW ISOMETRIC INDIVIDUAL CELLS, PLAYERS AND MOBS
private fun DrawScope.drawIsometricGridCell(
    x: Int, y: Int, z: Int,
    cx: Float, cy: Float,
    bw: Float, bh: Float, bd: Float,
    vm: GameViewModel,
    mobs: List<Mob>
) {
    // Check if block type warrants rendering (non-Air)
    val block = vm.getBlockAt(x, y, z)

    // Standard camera rotation mappings for the grid projection coordinates
    val rotation = vm.cameraRotation
    val rx = when (rotation) {
        0 -> x
        90 -> y
        180 -> 15 - x
        270 -> 15 - y
        else -> x
    }
    val ry = when (rotation) {
        0 -> y
        90 -> 15 - x
        180 -> 15 - y
        270 -> x
        else -> y
    }

    // Formula maps 3D grid indexes into 2D isometric projection positions
    val u = (rx - ry) * bw / 2
    val v = (rx + ry) * bh / 2 - z * bd

    val screenX = cx + u
    val screenY = cy + v

    if (block != BlockType.AIR) {
        drawIsometricVoxel(screenX, screenY, bw, bh, bd, block, vm.ignitedTNTs.value.containsKey(BlockPos(x, y, z)))

        // Tap interactions / simple raycasting inside Canvas is handled by overlay click check bounds
        // Since we draw in Painters loop, we detect touch interaction inside the bounds directly!
        // To simplify, when a user double taps, we check coordinates
    }

    // DRAW STEVE PLAYER AT CORRESPONDING painters block coordinates depth
    val px = vm.playerX
    val py = vm.playerY
    val pz = vm.playerZ

    if (px.toInt() == x && py.toInt() == y && pz.toInt() == z) {
        drawCuteSteveCharacter(screenX, screenY, bw, bh)
    }

    // DRAW THE HOSTILE MOBS DIRECTLY ON TOP OF SPANS
    mobs.forEach { mob ->
        if (mob.x.toInt() == x && mob.y.toInt() == y && mob.z.toInt() == z) {
            drawPixelArtMob(screenX, screenY, bw, bh, mob)
        }
    }
}

// DRAW AMAZING DETAILED 3D VOXEL POLYGONS PROC TEXTURING
private fun DrawScope.drawIsometricVoxel(
    cx: Float, cy: Float,
    w: Float, h: Float, d: Float,
    block: BlockType,
    isIgnitedTnt: Boolean
) {
    val hHalf = h / 2
    val wHalf = w / 2

    // DEFINE PATHS FOR TOP, LEFT, RIGHT CUBIC FACES
    val topFace = Path().apply {
        moveTo(cx, cy - hHalf)
        lineTo(cx + wHalf, cy)
        lineTo(cx, cy + hHalf)
        lineTo(cx - wHalf, cy)
        close()
    }

    val leftFace = Path().apply {
        moveTo(cx - wHalf, cy)
        lineTo(cx, cy + hHalf)
        lineTo(cx, cy + hHalf + d)
        lineTo(cx - wHalf, cy + d)
        close()
    }

    val rightFace = Path().apply {
        moveTo(cx, cy + hHalf)
        lineTo(cx + wHalf, cy)
        lineTo(cx + wHalf, cy + d)
        lineTo(cx, cy + hHalf + d)
        close()
    }

    // Colors base
    val colors = when (block) {
        BlockType.GRASS -> Triple(Color(0xFF4CAF50), Color(0xFF8D6E63), Color(0xFF7E5242)) // Grass/Dirt side
        BlockType.DIRT -> Triple(Color(0xFF8D6E63), Color(0xFF704D40), Color(0xFF5D3F34))
        BlockType.STONE -> Triple(Color(0xFFB0BEC5), Color(0xFF78909C), Color(0xFF546E7A))
        BlockType.COBBLESTONE -> Triple(Color(0xFF90A4AE), Color(0xFF546E7A), Color(0xFF37474F))
        BlockType.OAK_LOG -> Triple(Color(0xFFD7CCC8), Color(0xFF5D4037), Color(0xFF4E342E))
        BlockType.OAK_LEAVES -> Triple(Color(0xFF4CAF50).copy(alpha = 0.8f), Color(0xFF388E3C).copy(alpha = 0.8f), Color(0xFF2E7D32).copy(alpha = 0.8f))
        BlockType.PLANKS -> Triple(Color(0xFFFFCC80), Color(0xFFE0A96D), Color(0xFFD49C5D))
        BlockType.SAND -> Triple(Color(0xFFFFF59D), Color(0xFFFBC02D), Color(0xFFF57F17))
        BlockType.GLASS -> Triple(Color(0x55B2EBF2), Color(0x33B2EBF2), Color(0x33B2EBF2))
        BlockType.WATER -> Triple(Color(0x99039BE5), Color(0x990288D1), Color(0x990277BD))
        BlockType.LAVA -> Triple(Color(0xFFFF5722), Color(0xFFE64A19), Color(0xFFD84315))
        BlockType.COAL_ORE -> Triple(Color(0xFF78909C), Color(0xFF546E7A), Color(0xFF37474F))
        BlockType.IRON_ORE -> Triple(Color(0xFF90A4AE), Color(0xFF78909C), Color(0xFF546E7A))
        BlockType.GOLD_ORE -> Triple(Color(0xFFB0BEC5), Color(0xFF90A4AE), Color(0xFF78909C))
        BlockType.DIAMOND_ORE -> Triple(Color(0xFFB2DFDB), Color(0xFF80CBC4), Color(0xFF009688))
        BlockType.BRICK -> Triple(Color(0xFFFF8A65), Color(0xFFD84315), Color(0xFFBF360C))
        BlockType.TNT -> Triple(Color(0xFFE53935), Color(0xFFC62828), Color(0xFFB71C1C))
        BlockType.REDSTONE_BLOCK -> Triple(Color(0xFFFF1744), Color(0xFFD50000), Color(0xFFB71C1C))
        BlockType.BEDROCK -> Triple(Color(0xFF424242), Color(0xFF303030), Color(0xFF212121))
        else -> Triple(Color.Gray, Color.DarkGray, Color.Black)
    }

    val tColor = if (isIgnitedTnt) Color.White else colors.first
    val lColor = if (isIgnitedTnt) Color.Red else colors.second
    val rColor = if (isIgnitedTnt) Color.Red else colors.third

    // DRAW FACES
    drawPath(topFace, tColor)
    drawPath(leftFace, lColor)
    drawPath(rightFace, rColor)

    // DRAW CONTOUR GRID OUTLINES FOR CONTRAST
    drawPath(topFace, Color.Black.copy(alpha = 0.25f), style = Stroke(width = 0.5f))
    drawPath(leftFace, Color.Black.copy(alpha = 0.25f), style = Stroke(width = 0.5f))
    drawPath(rightFace, Color.Black.copy(alpha = 0.25f), style = Stroke(width = 0.5f))

    // SPECTACULAR PIXEL ART TEXTURE OVERLAYS
    when (block) {
        BlockType.GRASS -> {
            // Draw a cute hanging green grass fringe on Left/Right faces
            val lFringe = Path().apply {
                moveTo(cx - wHalf, cy)
                lineTo(cx, cy + hHalf)
                lineTo(cx, cy + hHalf + d * 0.22f)
                lineTo(cx - wHalf * 0.5f, cy + hHalf * 0.5f + d * 0.35f) // tooth grass jagged!
                lineTo(cx - wHalf, cy + d * 0.22f)
                close()
            }
            val rFringe = Path().apply {
                moveTo(cx, cy + hHalf)
                lineTo(cx + wHalf, cy)
                lineTo(cx + wHalf, cy + d * 0.22f)
                lineTo(cx + wHalf * 0.5f, cy + hHalf * 0.5f + d * 0.35f) // tooth grass
                lineTo(cx, cy + hHalf + d * 0.22f)
                close()
            }
            drawPath(lFringe, colors.first)
            drawPath(rFringe, colors.first)
        }

        BlockType.COAL_ORE -> drawOreFlecks(cx, cy, w, h, d, Color.Black)
        BlockType.IRON_ORE -> drawOreFlecks(cx, cy, w, h, d, Color(0xFFFFCC80)) // Sandstone colored specs
        BlockType.GOLD_ORE -> drawOreFlecks(cx, cy, w, h, d, Color(0xFFFFD54F)) // golden
        BlockType.DIAMOND_ORE -> drawOreFlecks(cx, cy, w, h, d, Color(0xFF40E0D0)) // glowing turquoise info

        BlockType.OAK_LOG -> {
            // concentric brown wood rings on top
            drawCircle(Color(0xFF5D4037), radius = wHalf * 0.55f, center = Offset(cx, cy), style = Stroke(width = 1f))
            drawCircle(Color(0xFF4E342E), radius = wHalf * 0.25f, center = Offset(cx, cy), style = Stroke(width = 1f))
        }

        BlockType.PLANKS -> {
            // Draw horizontal planks dividers
            drawLine(Color(0xFF3E2723).copy(alpha = 0.3f), Offset(cx - wHalf, cy + hHalf * 0.40f), Offset(cx + wHalf, cy - hHalf * 0.40f), strokeWidth = 1f)
            drawLine(Color(0xFF3E2723).copy(alpha = 0.3f), Offset(cx - wHalf, cy + hHalf * 0.85f), Offset(cx + wHalf, cy + hHalf * 0.35f), strokeWidth = 1f)
        }

        BlockType.BRICK -> {
            // brick outlines
            drawLine(Color.White.copy(alpha = 0.4f), Offset(cx - wHalf, cy), Offset(cx, cy + hHalf))
            drawLine(Color.White.copy(alpha = 0.4f), Offset(cx, cy + hHalf), Offset(cx + wHalf, cy))
            // vertical joints
            drawLine(Color.White.copy(alpha = 0.3f), Offset(cx - wHalf * 0.5f, cy + hHalf * 0.5f), Offset(cx - wHalf * 0.5f, cy + hHalf * 0.5f + d))
            drawLine(Color.White.copy(alpha = 0.3f), Offset(cx + wHalf * 0.5f, cy + hHalf * 0.5f), Offset(cx + wHalf * 0.5f, cy + hHalf * 0.5f + d))
        }

        BlockType.TNT -> {
            // White middle belt with black stripe accents
            val lBand = Path().apply {
                moveTo(cx - wHalf, cy + d * 0.35f)
                lineTo(cx, cy + hHalf + d * 0.35f)
                lineTo(cx, cy + hHalf + d * 0.65f)
                lineTo(cx - wHalf, cy + d * 0.65f)
                close()
            }
            val rBand = Path().apply {
                moveTo(cx, cy + hHalf + d * 0.35f)
                lineTo(cx + wHalf, cy + d * 0.35f)
                lineTo(cx + wHalf, cy + d * 0.65f)
                lineTo(cx, cy + hHalf + d * 0.65f)
                close()
            }
            drawPath(path = lBand, color = Color.White) // drawPath accepts paths
            drawPath(path = rBand, color = Color.White)
            
            // Draw small blocky TNT letters indicator
            drawRect(Color.Black, Offset(cx - wHalf * 0.3f, cy + d * 0.42f), androidx.compose.ui.geometry.Size(8f * w/32f, 8f))
            drawRect(Color.Black, Offset(cx + wHalf * 0.1f, cy + d * 0.42f), androidx.compose.ui.geometry.Size(8f * w/32f, 8f))
        }

        BlockType.WATER -> {
            // subtle wave highlights
            drawLine(Color.White.copy(alpha = 0.5f), Offset(cx - wHalf * 0.5f, cy), Offset(cx, cy - hHalf * 0.5f), strokeWidth = 1f)
            drawLine(Color.White.copy(alpha = 0.3f), Offset(cx, cy + hHalf * 0.5f), Offset(cx + wHalf * 0.5f, cy), strokeWidth = 1f)
        }
        else -> {}
    }
}

// DRAW CUTE JAGGED ORE SPECKS PROC
private fun DrawScope.drawOreFlecks(cx: Float, cy: Float, w: Float, h: Float, d: Float, specColor: Color) {
    val wh = w / 2
    val hh = h / 2

    // Dots on top
    drawCircle(specColor, radius = 2.5f, center = Offset(cx - wh * 0.4f, cy - hh * 0.2f))
    drawCircle(specColor, radius = 2f, center = Offset(cx + wh * 0.3f, cy + hh * 0.1f))

    // Dots on left wall
    drawCircle(specColor, radius = 2.5f, center = Offset(cx - wh * 0.6f, cy + d * 0.4f))
    drawCircle(specColor, radius = 2f, center = Offset(cx - wh * 0.2f, cy + hh * 0.5f + d * 0.6f))

    // Dots on right wall
    drawCircle(specColor, radius = 3f, center = Offset(cx + wh * 0.5f, cy + d * 0.3f))
    drawCircle(specColor, radius = 2f, center = Offset(cx + wh * 0.1f, cy + hh * 0.5f + d * 0.7f))
}

// DRAW CHARACTERS DETAILED PIXEL-ART MODEL CO-DEPTH
private fun DrawScope.drawCuteSteveCharacter(px: Float, py: Float, bw: Float, bh: Float) {
    // Steve stands vertically directly above the block. Offset screen height for height box:
    val heightScale = 1.35f
    val h = bw * heightScale
    val startY = py - bh * 0.5f // stands slightly inserted vertically

    // Let's divide Steve into Pants, Shirt, and Head
    val pantsTop = startY - h * 0.33f
    val shirtTop = startY - h * 0.72f
    val headTop = startY - h

    // Steve width is proportional
    val wHalf = bw * 0.28f

    // Blue Pants
    drawRect(
        color = Color(0xFF0288D1),
        topLeft = Offset(px - wHalf, pantsTop),
        size = androidx.compose.ui.geometry.Size(wHalf * 2, startY - pantsTop)
    )

    // Teal Shirt
    drawRect(
        color = Color(0xFF00ACC1),
        topLeft = Offset(px - wHalf, shirtTop),
        size = androidx.compose.ui.geometry.Size(wHalf * 2, pantsTop - shirtTop)
    )

    // Beige Skin Head
    drawRect(
        color = Color(0xFFFFCCBB),
        topLeft = Offset(px - wHalf * 0.85f, headTop),
        size = androidx.compose.ui.geometry.Size(wHalf * 1.7f, shirtTop - headTop)
    )

    // Brown Hair crown atop skin head
    drawRect(
        color = Color(0xFF5D4037),
        topLeft = Offset(px - wHalf * 0.85f, headTop),
        size = androidx.compose.ui.geometry.Size(wHalf * 1.7f, (shirtTop - headTop) * 0.33f)
    )

    // Face elements: Blue eyes
    drawRect(Color.Blue, Offset(px - wHalf * 0.5f, headTop + (shirtTop - headTop) * 0.45f), androidx.compose.ui.geometry.Size(2.3f, 2.3f))
    drawRect(Color.Blue, Offset(px + wHalf * 0.2f, headTop + (shirtTop - headTop) * 0.45f), androidx.compose.ui.geometry.Size(2.3f, 2.3f))

    // Cute Smile block
    drawRect(Color(0xFF8B0000), Offset(px - wHalf * 0.2f, headTop + (shirtTop - headTop) * 0.7f), androidx.compose.ui.geometry.Size(wHalf * 0.4f, 1.8f))
}

// DRAW THE CRAWLING HOSTILE NIGHT MONSTERS
private fun DrawScope.drawPixelArtMob(px: Float, py: Float, bw: Float, bh: Float, mob: Mob) {
    val h = bw * 1.35f
    val startY = py - bh * 0.5f

    val color = when (mob.type) {
        MobType.ZOMBIE -> Color(0xFF2E7D32) // deep rotten green skin
        MobType.CREEPER -> Color(0xFF4CAF50) // bright pixel neon leaf green
        MobType.SPIDER -> Color(0xFF212121) // dark obsidian grey spider body
    }

    val mobColor = if (mob.hurtTimer > 0) Color.Red else color // flash red when struck

    val pantsTop = startY - h * 0.33f
    val shirtTop = startY - h * 0.72f
    val headTop = startY - h
    val wHalf = bw * 0.28f

    when (mob.type) {
        MobType.ZOMBIE -> {
            // Blue zombie pants
            drawRect(Color(0xFF311B92).copy(alpha = if (mob.hurtTimer>0) 0.5f else 1.0f), Offset(px - wHalf, pantsTop), androidx.compose.ui.geometry.Size(wHalf * 2, startY - pantsTop))
            // Cyan zombie shirt
            drawRect(Color(0xFF00796B).copy(alpha = if (mob.hurtTimer>0) 0.5f else 1.0f), Offset(px - wHalf, shirtTop), androidx.compose.ui.geometry.Size(wHalf * 2, pantsTop - shirtTop))
            // Green zombie skin head
            drawRect(mobColor, Offset(px - wHalf * 0.85f, headTop), androidx.compose.ui.geometry.Size(wHalf * 1.7f, shirtTop - headTop))
            
            // Frowning black zombie eyes
            drawRect(Color.Black, Offset(px - wHalf * 0.5f, headTop + (shirtTop - headTop) * 0.48f), androidx.compose.ui.geometry.Size(2.5f, 2.5f))
            drawRect(Color.Black, Offset(px + wHalf * 0.2f, headTop + (shirtTop - headTop) * 0.48f), androidx.compose.ui.geometry.Size(2.5f, 2.5f))
        }

        MobType.CREEPER -> {
            // Creepers have a tall solid green body block (no pants/shoes structure)
            drawRect(mobColor, Offset(px - wHalf * 0.9f, headTop), androidx.compose.ui.geometry.Size(wHalf * 1.8f, startY - headTop))
            
            // Draw Creeper's signature sad black face mask!
            val faceY = headTop + (startY - headTop) * 0.15f
            drawRect(Color.Black, Offset(px - wHalf * 0.6f, faceY), androidx.compose.ui.geometry.Size(5f, 5f)) // Left Eye
            drawRect(Color.Black, Offset(px + wHalf * 0.2f, faceY), androidx.compose.ui.geometry.Size(5f, 5f)) // Right Eye
            drawRect(Color.Black, Offset(px - wHalf * 0.35f, faceY + 5f), androidx.compose.ui.geometry.Size(6f, 8f)) // Frown snout mouth
        }

        MobType.SPIDER -> {
            // Spiders are wide flat crawlers
            val spiderW = wHalf * 1.6f
            val bodyTop = startY - h * 0.45f
            drawRect(mobColor, Offset(px - spiderW, bodyTop), androidx.compose.ui.geometry.Size(spiderW * 2, startY - bodyTop))
            
            // Glow Red Spider compound eyes
            drawRect(Color.Red, Offset(px - spiderW * 0.7f, bodyTop + 4f), androidx.compose.ui.geometry.Size(3f, 3f))
            drawRect(Color.Red, Offset(px - spiderW * 0.3f, bodyTop + 4f), androidx.compose.ui.geometry.Size(2.5f, 2.5f))
            drawRect(Color.Red, Offset(px + spiderW * 0.2f, bodyTop + 4f), androidx.compose.ui.geometry.Size(2.5f, 2.5f))
            drawRect(Color.Red, Offset(px + spiderW * 0.5f, bodyTop + 4f), androidx.compose.ui.geometry.Size(3f, 3f))
            
            // Multiple crawling spiders legs stretching wide
            drawLine(mobColor, Offset(px - spiderW, startY - 4f), Offset(px - spiderW * 1.5f, startY + 6f), strokeWidth = 1.5f)
            drawLine(mobColor, Offset(px + spiderW, startY - 4f), Offset(px + spiderW * 1.5f, startY + 6f), strokeWidth = 1.5f)
        }
    }
}
