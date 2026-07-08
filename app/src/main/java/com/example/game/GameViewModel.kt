package com.example.game

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// Represents dynamic visual particles (for TNT explosions or mining breaks)
data class GameParticle(
    val id: String = UUID.randomUUID().toString(),
    var x: Float,
    var y: Float,
    var z: Float,
    val vx: Float,
    val vy: Float,
    val vz: Float,
    val color: Int, // Hex ARGB
    var life: Float = 1.0f, // Fades to 0
    val size: Float
)

// Dynamic enemy mob
data class Mob(
    val id: String = UUID.randomUUID().toString(),
    val type: MobType,
    var x: Float,
    var y: Float,
    var z: Float,
    var health: Int,
    var hurtTimer: Int = 0 // Displays red flash when attacked
)

enum class MobType(val displayName: String, val maxHealth: Int, val damage: Int, val dropItemId: Int) {
    ZOMBIE("Zombie", 10, 1, InventoryItem.COAL),
    CREEPER("Creeper", 6, 2, BlockType.SAND.id),
    SPIDER("Cave Spider", 8, 1, InventoryItem.STICK)
}

// Block coordinate position
data class BlockPos(val x: Int, val y: Int, val z: Int)

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val db = androidx.room.Room.databaseBuilder(
        application,
        AppDatabase::class.java,
        "voxelcraft_db"
    ).build()
    
    private val repository = WorldRepository(db.worldDao())

    // Database Reactive Worlds Flow
    val allWorlds: Flow<List<WorldEntity>> = repository.allWorlds

    // ACTIVE GAME SESSIONS STATE
    var activeWorld by mutableStateOf<WorldEntity?>(null)
        private set

    // Camera Parameters
    var cameraRotation by mutableStateOf(0) // 0, 90, 180, 270 degrees
    var cameraZoom by mutableStateOf(1.0f) // Scale multiplier (0.4f to 2.5f)
    var cameraPanX by mutableStateOf(0f)
    var cameraPanY by mutableStateOf(0f)

    // Player Stats
    var playerX by mutableStateOf(8.0f)
    var playerY by mutableStateOf(8.0f)
    var playerZ by mutableStateOf(8.0f)
    var playerHealth by mutableStateOf(10) // Number of hearts (max 10)
    var playerHunger by mutableStateOf(10) // Max 10 drumsticks
    var playerIsGrounded by mutableStateOf(true)

    // Player Inventory: ItemId -> Quantity
    var playerInventory = mutableStateOf<Map<Int, Int>>(emptyMap())

    // Currently Selected Item in the Hotbar
    var selectedHotbarItem by mutableStateOf<Int>(BlockType.PLANKS.id)

    // Active Game Mode: "CREATIVE" or "SURVIVAL"
    var activeGameMode by mutableStateOf("CREATIVE")

    // World Map Modifications (X,Y,Z -> Override Type)
    // Keys are stored as "x,y,z"
    var modifiedBlocks = mutableStateOf<Map<String, BlockType>>(emptyMap())

    // Day/Night Timer (0 to 120 seconds cycle. Day is 0-45, Dusk is 45-55, Night is 55-105, Dawn is 105-120)
    var dayCycleTicks by mutableStateOf(20f) // Float representing seconds elapsed

    // Hostile Mobs
    var activeMobs = mutableStateOf<List<Mob>>(emptyList())

    // Particle Simulations
    var activeParticles = mutableStateOf<List<GameParticle>>(emptyList())

    // Game Logs
    var gameLogs = mutableStateOf<List<String>>(listOf("Welcome to Voxel Craft! Click, build, and survive."))

    // Active ticking TNTs: BlockPos -> Time remaining in ticks
    var ignitedTNTs = mutableStateOf<Map<BlockPos, Int>>(emptyMap())

    private var gameLoopJob: Job? = null
    var activePanel by mutableStateOf("none") // "none", "inventory", "crafting", "help", "pause"

    // Red damage flash trigger
    var hurtOverlayAlpha by mutableStateOf(0f)

    init {
        // Run simple ticking cleanups
        addLog("Procedural engine loaded. Build details in creative or survive the night!")
    }

    // CREATE OR LAUNCH WORLD
    fun startNewWorld(name: String, seed: Long, creative: Boolean) {
        viewModelScope.launch {
            val initialInventory = if (creative) {
                // Creative gets initial basic blocks unlimited virtual
                emptyMap()
            } else {
                // Survival defaults
                mapOf(
                    BlockType.PLANKS.id to 8,
                    BlockType.SAND.id to 4,
                    BlockType.OAK_LOG.id to 4,
                    InventoryItem.STICK to 6,
                    InventoryItem.WOODEN_PICKAXE to 1
                )
            }

            val inventoryString = initialInventory.map { "${it.key}:${it.value}" }.joinToString(";")

            // Find ground height at start coordinates (8,8)
            val groundZ = getTerrainHeightAt(8, 8, seed).toFloat() + 1.0f

            val newWorld = WorldEntity(
                name = name,
                seed = seed,
                gameMode = if (creative) "CREATIVE" else "SURVIVAL",
                playerX = 8.0f,
                playerY = 8.0f,
                playerZ = groundZ,
                playerHealth = 10,
                playerHunger = 10,
                playerInventoryJson = inventoryString
            )
            val id = repository.insert(newWorld)
            loadWorldSession(id)
        }
    }

    fun loadWorldSession(worldId: Long) {
        viewModelScope.launch {
            val world = repository.getWorld(worldId) ?: return@launch
            
            // Apply loaded parameters
            activeWorld = world
            activeGameMode = world.gameMode
            playerX = world.playerX
            playerY = world.playerY
            playerZ = world.playerZ
            playerHealth = world.playerHealth
            playerHunger = world.playerHunger
            
            // Deserialize Inventor
            val inv = mutableMapOf<Int, Int>()
            if (world.playerInventoryJson.isNotEmpty()) {
                world.playerInventoryJson.split(";").forEach { item ->
                    val pts = item.split(":")
                    if (pts.size == 2) {
                        val id = pts[0].toIntOrNull()
                        val qty = pts[1].toIntOrNull()
                        if (id != null && qty != null) inv[id] = qty
                    }
                }
            }
            playerInventory.value = inv

            // Hotbar select defaults
            if (activeGameMode == "SURVIVAL") {
                selectedHotbarItem = inv.keys.firstOrNull() ?: BlockType.PLANKS.id
            } else {
                selectedHotbarItem = BlockType.GRASS.id
            }

            // Deserialize modifications
            val mods = mutableMapOf<String, BlockType>()
            if (world.blockData.isNotEmpty()) {
                world.blockData.split(";").forEach { mod ->
                    val parts = mod.split(":")
                    if (parts.size == 2) {
                        val coords = parts[0]
                        val blockId = parts[1].toIntOrNull()
                        if (blockId != null) {
                            mods[coords] = BlockType.fromId(blockId)
                        }
                    }
                }
            }
            modifiedBlocks.value = mods

            cameraPanX = 0f
            cameraPanY = 0f
            cameraZoom = 1.0f
            cameraRotation = 0
            dayCycleTicks = 20f
            activeMobs.value = emptyList()
            activeParticles.value = emptyList()
            ignitedTNTs.value = emptyMap()
            activePanel = "none"

            addLog("Loaded world '${world.name}' successfully.")
            addLog("Game Mode: $activeGameMode. Terrain generated securely from seed: ${world.seed}")

            // Launch ticking loops
            startGameLoop()
        }
    }

    fun deleteWorld(worldId: Long) {
        viewModelScope.launch {
            repository.deleteById(worldId)
        }
    }

    fun saveAndExit() {
        val currentWorld = activeWorld ?: return
        viewModelScope.launch {
            // Serialize Map
            val blockDataStr = modifiedBlocks.value.map { "${it.key}:${it.value.id}" }.joinToString(";")
            val inventoryStr = playerInventory.value.map { "${it.key}:${it.value}" }.joinToString(";")

            val updated = currentWorld.copy(
                lastPlayedTime = System.currentTimeMillis(),
                playerX = playerX,
                playerY = playerY,
                playerZ = playerZ,
                playerHealth = playerHealth,
                playerHunger = playerHunger,
                blockData = blockDataStr,
                playerInventoryJson = inventoryStr
            )

            repository.update(updated)
            
            // Terminate loops
            stopGameLoop()
            activeWorld = null
            addLog("Saved world results.")
        }
    }

    private fun startGameLoop() {
        stopGameLoop()
        gameLoopJob = viewModelScope.launch(Dispatchers.Default) {
            var mobSpawnTicker = 0
            while (isActive) {
                // Day Night Clock Ticking
                dayCycleTicks += 0.2f // Full cycle takes 120 seconds
                if (dayCycleTicks >= 120f) {
                    dayCycleTicks = 0f
                }

                // Smooth out flashing hurt overlay
                if (hurtOverlayAlpha > 0f) {
                    hurtOverlayAlpha = (hurtOverlayAlpha - 0.05f).coerceAtLeast(0f)
                }

                // Simulate Gravity on player
                simulatePlayerPhysics()

                // Sim particles
                simulateParticles()

                // Sim TNT explosions
                simulateTNT()

                // Sim hostile monsters
                simulateMobs()

                // Mob spawner: at night, spawn occasionally
                mobSpawnTicker++
                if (activeGameMode == "SURVIVAL" && isNight() && activeMobs.value.size < 4 && mobSpawnTicker >= 150) {
                    mobSpawnTicker = 0
                    spawnNightMob()
                }

                delay(50) // Runs at ~20 ticks/sec
            }
        }
    }

    private fun stopGameLoop() {
        gameLoopJob?.cancel()
        gameLoopJob = null
    }

    // GAMEPLAY INTERACTIONS
    fun addLog(msg: String) {
        val current = gameLogs.value.toMutableList()
        current.add(0, msg)
        if (current.size > 8) current.removeAt(current.size - 1)
        gameLogs.value = current
    }

    // TERRAIN RESOLUTION ENGINE
    fun getBlockAt(x: Int, y: Int, z: Int): BlockType {
        // Enforce physical boundary limits
        if (x !in 0..15 || y !in 0..15 || z !in 0..15) return BlockType.AIR

        // 1. Resolve placed modifications
        val key = "$x,$y,$z"
        modifiedBlocks.value[key]?.let { return it }

        // 2. Deterministic Seed terrain generator
        val seed = activeWorld?.seed ?: 12345L
        
        // Custom deterministic trees check
        if (hasTreeAt(x, y, seed)) {
            val h = getBaseHeight(x, y, seed)
            if (z in h + 1..h + 3) {
                return BlockType.OAK_LOG
            }
            if (z in h + 3..h + 5) {
                // Leaves cluster: a beautiful pixel star crown!
                val dx = Math.abs(x - x)
                val dy = Math.abs(y - y) // wait, tree trunk is at (x, y)
                // Let's check leaf spread around the trunk coordinate
                return BlockType.OAK_LEAVES
            }
        }

        // Check neighboring樹 leaf bounds!
        // To build complete trees, check if adjacent cells contain a tree trunk
        for (tx in (x - 2).coerceAtLeast(0)..(x + 2).coerceAtMost(15)) {
            for (ty in (y - 2).coerceAtLeast(0)..(y + 2).coerceAtMost(15)) {
                if (tx == x && ty == y) continue
                if (hasTreeAt(tx, ty, seed)) {
                    val th = getBaseHeight(tx, ty, seed)
                    val leafZStart = th + 3
                    val distSq = (tx - x) * (tx - x) + (ty - y) * (ty - y)
                    if (z in leafZStart..leafZStart + 1) {
                        if (distSq <= 2) return BlockType.OAK_LEAVES
                    } else if (z == leafZStart + 2) {
                        if (distSq <= 1) return BlockType.OAK_LEAVES
                    }
                }
            }
        }

        val h = getBaseHeight(x, y, seed)

        if (z <= h) {
            if (z == 0) return BlockType.BEDROCK
            
            if (z < h - 3) {
                // Generate ore pockets deep under the earth
                val rHash = (x * 373L + y * 983L + z * 509L + seed).coerceAtLeast(0)
                return when {
                    z <= 3 && rHash % 80 == 0L -> BlockType.DIAMOND_ORE
                    z <= 5 && rHash % 50 == 0L -> BlockType.GOLD_ORE
                    z <= 8 && rHash % 30 == 0L -> BlockType.IRON_ORE
                    z <= 11 && rHash % 18 == 0L -> BlockType.COAL_ORE
                    else -> BlockType.STONE
                }
            }
            
            if (z >= h - 3 && z < h) return BlockType.DIRT
            
            // Topmost block layer
            return if (h < 5) BlockType.SAND else BlockType.GRASS
        }

        // Spawn Water below level 5
        if (z in h + 1..5) {
            return BlockType.WATER
        }

        return BlockType.AIR
    }

    // Get simple column boundary
    fun getTerrainHeightAt(x: Int, y: Int, seed: Long): Int {
        val h = getBaseHeight(x, y, seed)
        // Adjust for trees
        if (hasTreeAt(x, y, seed)) {
            return h + 4
        }
        return h.coerceAtLeast(5) // include water/sand height
    }

    private fun getBaseHeight(x: Int, y: Int, seed: Long): Int {
        val r = Random(seed)
        val o1 = r.nextDouble() * 200.0
        val o2 = r.nextDouble() * 200.0
        val scale = 0.15
        val h = sin((x + o1) * scale) * cos((y + o2) * scale) * 3.5 + 5.5
        return h.toInt().coerceIn(1, 14)
    }

    private fun hasTreeAt(tx: Int, ty: Int, seed: Long): Boolean {
        // Safe trees boundary
        if (tx < 2 || tx > 13 || ty < 2 || ty > 13) return false
        val h = getBaseHeight(tx, ty, seed)
        if (h < 5) return false // No marine trees
        val hash = (tx * 123457L + ty * 9876543L + seed).coerceAtLeast(0)
        return (hash % 100) < 6 // 6% density
    }

    fun isNight(): Boolean {
        return dayCycleTicks in 55.0f..105.0f
    }

    // PLAYER PHYSICS SIMULATOR
    private fun simulatePlayerPhysics() {
        val ix = playerX.toInt().coerceIn(0, 15)
        val iy = playerY.toInt().coerceIn(0, 15)
        
        // Find highest solid block underneath the player
        var groundZ = 0
        for (z in 15 downTo 0) {
            val block = getBlockAt(ix, iy, z)
            if (block.isSolid) {
                groundZ = z + 1
                break
            }
        }

        val targetZ = groundZ.toFloat()

        if (playerZ > targetZ) {
            // Apply simple Gravity
            playerZ -= 0.18f
            playerIsGrounded = false
            if (playerZ < targetZ) {
                playerZ = targetZ
                playerIsGrounded = true
            }
        } else {
            playerZ = targetZ
            playerIsGrounded = true
        }

        // Suffocation safe-checks: push player up if stuck inside walls
        val headBlock = getBlockAt(ix, iy, playerZ.toInt() + 1)
        val bodyBlock = getBlockAt(ix, iy, playerZ.toInt())
        if (bodyBlock.isSolid || headBlock.isSolid) {
            playerZ = (playerZ.toInt() + 1).toFloat()
        }
    }

    // DIRECTIONAL PLAYERWALK
    fun movePlayer(dx: Float, dy: Float) {
        val nx = (playerX + dx).coerceIn(0.1f, 14.9f)
        val ny = (playerY + dy).coerceIn(0.1f, 14.9f)

        val nix = nx.toInt()
        val niy = ny.toInt()
        val niz = playerZ.toInt()

        // Wall collisions
        val blockBody = getBlockAt(nix, niy, niz)
        val blockHead = getBlockAt(nix, niy, niz + 1)

        if (!blockBody.isSolid && !blockHead.isSolid) {
            // Unimpeded path
            playerX = nx
            playerY = ny
        } else {
            // Attempt Auto Step Climb (Step block climb allows climbing 1-high walls easily)
            val stepHeightBlock = getBlockAt(nix, niy, niz + 1)
            val stepHeadBlock = getBlockAt(nix, niy, niz + 2)
            if (!stepHeightBlock.isSolid && !stepHeadBlock.isSolid && getBlockAt(nix, niy, niz).isSolid) {
                playerX = nx
                playerY = ny
                playerZ = (niz + 1).toFloat() // climbed up 1 step!
            }
        }
    }

    fun jumpPlayer() {
        if (playerIsGrounded) {
            playerZ += 1.4f
            playerIsGrounded = false
        }
    }

    // PLACE SOLID BLOCK
    fun placeBlock(x: Int, y: Int, z: Int) {
        if (x !in 0..15 || y !in 0..15 || z !in 0..15) return
        
        val blockToPlace = BlockType.fromId(selectedHotbarItem)
        if (blockToPlace == BlockType.AIR) return

        // Survival consumption checks
        if (activeGameMode == "SURVIVAL") {
            val qty = playerInventory.value[selectedHotbarItem] ?: 0
            if (qty <= 0) {
                addLog("Cannot place: out of ${blockToPlace.displayName}")
                return
            }
            adjustInventory(selectedHotbarItem, -1)
        }

        val key = "$x,$y,$z"
        val updated = modifiedBlocks.value.toMutableMap()
        updated[key] = blockToPlace
        modifiedBlocks.value = updated

        // Check TNT triggers
        if (blockToPlace == BlockType.TNT) {
            addLog("Planted TNT charges. Interact on it to ignite!")
        }

        // Particle accents
        generateBreakParticles(x.toFloat(), y.toFloat(), z.toFloat(), 0xFF7CFC00.toInt()) // grass green
        addLog("Placed ${blockToPlace.displayName} at ($x, $y, $z)")
    }

    // MINING / BREAKING
    fun breakBlock(x: Int, y: Int, z: Int) {
        if (x !in 0..15 || y !in 0..15 || z !in 0..15) return

        val currentBlock = getBlockAt(x, y, z)
        if (currentBlock == BlockType.AIR || currentBlock == BlockType.BEDROCK) return

        // Survival harvesting
        if (activeGameMode == "SURVIVAL") {
            // Check tool mining speed / rules
            val hasPickaxe = playerInventory.value.keys.any { 
                it == InventoryItem.WOODEN_PICKAXE || it == InventoryItem.STONE_PICKAXE || 
                it == InventoryItem.IRON_PICKAXE || it == InventoryItem.DIAMOND_PICKAXE 
            }
            
            if (currentBlock == BlockType.STONE && !hasPickaxe) {
                addLog("Gathering Stone/Stone minerals requires a Pickaxe tool!")
                return
            }

            // Drop appropriate survival yields
            val yieldId = when (currentBlock) {
                BlockType.GRASS -> BlockType.DIRT.id
                BlockType.OAK_LEAVES -> {
                    // Chance for apples
                    if (Random.nextFloat() < 0.2f) {
                        109 // Custom apple resource ID in panel
                    } else if (Random.nextFloat() < 0.15f) {
                        InventoryItem.STICK
                    } else {
                        BlockType.OAK_LEAVES.id
                    }
                }
                BlockType.COAL_ORE -> InventoryItem.COAL
                BlockType.DIAMOND_ORE -> InventoryItem.DIAMOND
                else -> currentBlock.id
            }

            adjustInventory(yieldId, 1)
            addLog("Harvested: ${InventoryItem.fromId(yieldId).name}")
        }

        val key = "$x,$y,$z"
        val updated = modifiedBlocks.value.toMutableMap()
        updated[key] = BlockType.AIR
        modifiedBlocks.value = updated

        // Particle accents
        generateBreakParticles(x.toFloat(), y.toFloat(), z.toFloat(), 0xFF808080.toInt())
    }

    // TNT IGNITION ENGINE
    fun interactBlock(x: Int, y: Int, z: Int) {
        val block = getBlockAt(x, y, z)
        if (block == BlockType.TNT) {
            val pos = BlockPos(x, y, z)
            val updated = ignitedTNTs.value.toMutableMap()
            updated[pos] = 50 // 50 ticks = 2.5 seconds
            ignitedTNTs.value = updated
            addLog("TNT ignited! RUN!")
            
            // Sparkles
            generateBreakParticles(x.toFloat(), y.toFloat(), z.toFloat() + 0.5f, 0xFFFF0000.toInt())
        }
    }

    // TNT ACTUATORS
    private fun simulateTNT() {
        val active = ignitedTNTs.value.toMutableMap()
        if (active.isEmpty()) return

        val explodedPos = mutableListOf<BlockPos>()

        active.keys.forEach { pos ->
            val ticks = active[pos] ?: 0
            if (ticks <= 1) {
                explodedPos.add(pos)
            } else {
                active[pos] = ticks - 1
                // Sparkle visual dynamic particles
                if (Random.nextFloat() < 0.4f) {
                    val pList = activeParticles.value.toMutableList()
                    pList.add(
                        GameParticle(
                            x = pos.x + 0.5f, y = pos.y + 0.5f, z = pos.z + 1.1f,
                            vx = Random.nextFloat() * 0.1f - 0.05f,
                            vy = Random.nextFloat() * 0.1f - 0.05f,
                            vz = Random.nextFloat() * 0.15f,
                            color = 0xFFFFFF00.toInt(),
                            size = 12f
                        )
                    )
                    activeParticles.value = pList
                }
            }
        }

        explodedPos.forEach { pos ->
            active.remove(pos)
            triggerExplosion(pos)
        }

        ignitedTNTs.value = active
    }

    private fun triggerExplosion(pos: BlockPos) {
        addLog("BOOM! TNT detonated.")
        
        // Remove TNT block
        val updated = modifiedBlocks.value.toMutableMap()
        updated["${pos.x},${pos.y},${pos.z}"] = BlockType.AIR

        // Blow up solid blocks in a 3-block radius
        val radius = 3
        for (dx in -radius..radius) {
            for (dy in -radius..radius) {
                for (dz in -radius..radius) {
                    if (dx*dx + dy*dy + dz*dz <= radius*radius) {
                        val tx = pos.x + dx
                        val ty = pos.y + dy
                        val tz = pos.z + dz
                        
                        if (tx in 0..15 && ty in 0..15 && tz in 0..15) {
                            val block = getBlockAt(tx, ty, tz)
                            if (block != BlockType.BEDROCK && block != BlockType.AIR) {
                                updated["$tx,$ty,$tz"] = BlockType.AIR
                            }
                        }
                    }
                }
            }
        }
        modifiedBlocks.value = updated

        // Check Player damage
        val pDistSq = (playerX - pos.x)*(playerX - pos.x) + (playerY - pos.y)*(playerY - pos.y) + (playerZ - pos.z)*(playerZ - pos.z)
        if (pDistSq < 16 && activeGameMode == "SURVIVAL") {
            damagePlayer(3) // 3 hearts damage
            addLog("You caught in the TNT explosion blast!")
        }

        // Check Mobs damage
        val list = activeMobs.value.toMutableList()
        list.forEach { mob ->
            val mDistSq = (mob.x - pos.x)*(mob.x - pos.x) + (mob.y - pos.y)*(mob.y - pos.y) + (mob.z - pos.z)*(mob.z - pos.z)
            if (mDistSq < 16) {
                mob.health -= 6
                mob.hurtTimer = 6
            }
        }
        activeMobs.value = list.filter { it.health > 0 }

        // Explode massive particle burst
        val pList = activeParticles.value.toMutableList()
        for (i in 0..35) {
            pList.add(
                GameParticle(
                    x = pos.x + 0.5f, y = pos.y + 0.5f, z = pos.z + 0.5f,
                    vx = (Random.nextFloat() - 0.5f) * 0.5f,
                    vy = (Random.nextFloat() - 0.5f) * 0.5f,
                    vz = Random.nextFloat() * 0.4f + 0.1f,
                    color = if (Random.nextBoolean()) 0xFFFF4500.toInt() else 0xFFFFD700.toInt(), // Orange/Yellow
                    size = Random.nextFloat() * 15f + 10f,
                    life = 1.0f
                )
            )
        }
        activeParticles.value = pList
    }

    // SURVIVAL SPAWNING & MOB ACTIONS
    private fun spawnNightMob() {
        val seed = activeWorld?.seed ?: 456L
        val r = Random(System.nanoTime())
        val type = MobType.values()[r.nextInt(MobType.values().size)]
        
        // Spawn along perimeter bounds
        val rx = if (r.nextBoolean()) 0f else 15f
        val ry = if (r.nextBoolean()) r.nextFloat() * 15f else 15f
        val rz = getTerrainHeightAt(rx.toInt(), ry.toInt(), seed).toFloat() + 1.0f

        val list = activeMobs.value.toMutableList()
        list.add(
            Mob(type = type, x = rx, y = ry, z = rz, health = type.maxHealth)
        )
        activeMobs.value = list
        addLog("A ${type.displayName} has emerged in the night shadows!")
    }

    private fun simulateMobs() {
        if (activeMobs.value.isEmpty()) return
        val list = activeMobs.value.toMutableList()
        val playerVecX = playerX
        val playerVecY = playerY
        
        list.forEach { mob ->
            if (mob.hurtTimer > 0) mob.hurtTimer--

            // Simple crawl speed towards player
            val dx = playerVecX - mob.x
            val dy = playerVecY - mob.y
            val len = Math.sqrt((dx*dx + dy*dy).toDouble()).toFloat()
            if (len > 0.4f) {
                val speed = 0.05f
                mob.x += (dx / len) * speed
                mob.y += (dy / len) * speed
                
                // Align to ground surface
                val ix = mob.x.toInt().coerceIn(0, 15)
                val iy = mob.y.toInt().coerceIn(0, 15)
                val seed = activeWorld?.seed ?: 1234L
                mob.z = getTerrainHeightAt(ix, iy, seed).toFloat()
            }

            // Test player contact damage
            val pDistSq = (playerX - mob.x)*(playerX - mob.x) + (playerY - mob.y)*(playerY - mob.y) + (playerZ - mob.z)*(playerZ - mob.z)
            if (pDistSq < 1.21f && activeGameMode == "SURVIVAL") {
                // Damage Player
                damagePlayer(1)
                // Knockback player slightly
                val kx = (playerX - mob.x) * 1.5f
                val ky = (playerY - mob.y) * 1.5f
                movePlayer(kx, ky)
            }
        }
        activeMobs.value = list
    }

    fun attackMob(mobId: String) {
        val list = activeMobs.value.toMutableList()
        val mob = list.firstOrNull { it.id == mobId } ?: return

        // Deal blow
        val dmg = if (playerInventory.value.keys.any { it == InventoryItem.DIAMOND_PICKAXE }) 4 
                  else if (playerInventory.value.keys.any { it == InventoryItem.IRON_PICKAXE }) 3
                  else 2
        
        mob.health -= dmg
        mob.hurtTimer = 4
        
        // Push enemy back
        val dx = mob.x - playerX
        val dy = mob.y - playerY
        val len = Math.sqrt((dx*dx + dy*dy).toDouble()).toFloat()
        if (len > 0) {
            mob.x = (mob.x + (dx / len) * 1.5f).coerceIn(0f, 15f)
            mob.y = (mob.y + (dy / len) * 1.5f).coerceIn(0f, 15f)
        }

        generateBreakParticles(mob.x, mob.y, mob.z + 0.5f, 0xFF8B0000.toInt()) // Blood red pixels

        if (mob.health <= 0) {
            list.remove(mob)
            // Reward survival player
            if (activeGameMode == "SURVIVAL") {
                adjustInventory(mob.type.dropItemId, 1)
                addLog("You defeated ${mob.type.displayName}! Harvested ${InventoryItem.fromId(mob.type.dropItemId).name}.")
            } else {
                addLog("Defeated standard ${mob.type.displayName}")
            }
        } else {
            addLog("Hit ${mob.type.displayName}! (HP: ${mob.health}/${mob.type.maxHealth})")
        }

        activeMobs.value = list
    }

    // SURVIVAL DAMAGE HEALTH ENGINE
    private fun damagePlayer(count: Int) {
        if (activeGameMode == "CREATIVE") return
        playerHealth = (playerHealth - count).coerceIn(0, 10)
        hurtOverlayAlpha = 0.5f // flashes dark red
        
        if (playerHealth <= 0) {
            addLog("You died! Respawning in safe grass coordinates...")
            // Respawn mechanics
            playerHealth = 10
            playerHunger = 10
            playerX = 8f
            playerY = 8f
            val seed = activeWorld?.seed ?: 1234L
            playerZ = getTerrainHeightAt(8, 8, seed).toFloat() + 2f
        }
    }

    fun eatApple() {
        val appQty = playerInventory.value[109] ?: 0
        if (appQty <= 0) return
        adjustInventory(109, -1)
        playerHealth = (playerHealth + 3).coerceAtMost(10)
        addLog("Ate a juicy apple. Recovered +3 Hearts!")
    }

    // INVENTORY MUTATION
    private fun adjustInventory(itemId: Int, delta: Int) {
        val map = playerInventory.value.toMutableMap()
        val currentQty = map[itemId] ?: 0
        val final = currentQty + delta
        if (final <= 0) {
            map.remove(itemId)
            if (selectedHotbarItem == itemId) {
                selectedHotbarItem = map.keys.firstOrNull() ?: BlockType.PLANKS.id
            }
        } else {
            map[itemId] = final
        }
        playerInventory.value = map
    }

    // CRAFTING IMPLEMENTATIONS
    fun craftItem(recipe: CraftingRecipe) {
        if (activeGameMode == "CREATIVE") {
            addLog("Infinite blocks in Creative Mode. No crafting needed.")
            return
        }

        // Test ingredient criteria
        recipe.ingredients.forEach { pair ->
            val id = pair.first
            val req = pair.second
            val current = playerInventory.value[id] ?: 0
            if (current < req) {
                addLog("Insufficient materials for ${recipe.name}.")
                return
            }
        }

        // Deduct items
        recipe.ingredients.forEach { pair ->
            adjustInventory(pair.first, -pair.second)
        }

        // Add crafted outputs
        adjustInventory(recipe.outputId, recipe.outputCount)
        addLog("Crafted: ${recipe.outputCount}x ${recipe.name}")
    }

    // PARTICLE RENDER TICKS
    private fun generateBreakParticles(x: Float, y: Float, z: Float, colorHex: Int) {
        val list = activeParticles.value.toMutableList()
        val r = Random(System.nanoTime())
        for (i in 0..8) {
            list.add(
                GameParticle(
                    x = x + 0.5f, y = y + 0.5f, z = z + 0.5f,
                    vx = (r.nextFloat() - 0.5f) * 0.15f,
                    vy = (r.nextFloat() - 0.5f) * 0.15f,
                    vz = r.nextFloat() * 0.2f + 0.05f,
                    color = colorHex,
                    size = r.nextFloat() * 6f + 4f,
                    life = 1.0f
                )
            )
        }
        activeParticles.value = list
    }

    private fun simulateParticles() {
        val list = activeParticles.value.toMutableList()
        if (list.isEmpty()) return

        val it = list.iterator()
        while (it.hasNext()) {
            val p = it.next()
            p.x += p.vx
            p.y += p.vy
            p.z += p.vz
            p.life -= 0.12f // speed of fading
            if (p.life <= 0f) {
                it.remove()
            }
        }
        activeParticles.value = list
    }
}
