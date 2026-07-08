package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

// Enumeration of all Minecraft-styled Block Types with specific properties
enum class BlockType(
    val id: Int,
    val displayName: String,
    val isSolid: Boolean = true,
    val isLightEmitting: Boolean = false,
    val isLiquid: Boolean = false,
    val opacity: Float = 1.0f
) {
    AIR(0, "Air", isSolid = false),
    GRASS(1, "Grass Block"),
    DIRT(2, "Dirt"),
    STONE(3, "Stone"),
    COBBLESTONE(4, "Cobblestone"),
    OAK_LOG(5, "Oak Log"),
    OAK_LEAVES(6, "Oak Leaves", opacity = 0.5f),
    PLANKS(7, "Oak Planks"),
    SAND(8, "Sand"),
    GLASS(9, "Glass", opacity = 0.3f),
    WATER(10, "Water", isSolid = false, isLiquid = true, opacity = 0.6f),
    LAVA(11, "Lava", isSolid = false, isLiquid = true, isLightEmitting = true, opacity = 1.0f),
    COAL_ORE(12, "Coal Ore"),
    IRON_ORE(13, "Iron Ore"),
    GOLD_ORE(14, "Gold Ore"),
    DIAMOND_ORE(15, "Diamond Ore"),
    BRICK(16, "Bricks"),
    TNT(17, "TNT"),
    REDSTONE_BLOCK(18, "Redstone Block", isLightEmitting = true),
    BEDROCK(19, "Bedrock");

    companion object {
        fun fromId(id: Int): BlockType {
            return values().firstOrNull { it.id == id } ?: AIR
        }
    }
}

@Entity(tableName = "worlds")
data class WorldEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val seed: Long,
    val createdTime: Long = System.currentTimeMillis(),
    val lastPlayedTime: Long = System.currentTimeMillis(),
    val gameMode: String = "CREATIVE", // "CREATIVE", "SURVIVAL"
    val blockData: String = "", // Map serializations of "x,y,z:id;x,y,z:id"
    val playerX: Float = 8.0f,
    val playerY: Float = 8.0f,
    val playerZ: Float = 10.0f,
    val playerHealth: Int = 10, // Hearts * 2 (10 hearts max represented as 20 half-hearts or 10 full hearts)
    val playerHunger: Int = 10,
    val playerInventoryJson: String = "" // Quantities of each block type in inventory
)

@Dao
interface WorldDao {
    @Query("SELECT * FROM worlds ORDER BY lastPlayedTime DESC")
    fun getAllWorlds(): Flow<List<WorldEntity>>

    @Query("SELECT * FROM worlds WHERE id = :id LIMIT 1")
    suspend fun getWorldById(id: Long): WorldEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorld(world: WorldEntity): Long

    @Update
    suspend fun updateWorld(world: WorldEntity)

    @Query("DELETE FROM worlds WHERE id = :id")
    suspend fun deleteWorldById(id: Long)
}

@Database(entities = [WorldEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun worldDao(): WorldDao
}

class WorldRepository(private val worldDao: WorldDao) {
    val allWorlds: Flow<List<WorldEntity>> = worldDao.getAllWorlds()

    suspend fun getWorld(id: Long): WorldEntity? = worldDao.getWorldById(id)

    suspend fun insert(world: WorldEntity): Long = worldDao.insertWorld(world)

    suspend fun update(world: WorldEntity) = worldDao.updateWorld(world)

    suspend fun deleteById(id: Long) = worldDao.deleteWorldById(id)
}
