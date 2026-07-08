package com.example.game

import com.example.data.BlockType

// Represent all distinct items in the player's inventory (including blocks and tools/materials)
sealed class InventoryItem(val id: Int, val name: String, val isBlock: Boolean) {
    class Block(val blockType: BlockType) : InventoryItem(blockType.id, blockType.displayName, true)
    
    class Special(id: Int, name: String) : InventoryItem(id, name, false)

    companion object {
        // Special items range
        const val STICK = 100
        const val COAL = 101
        const val IRON_INGOT = 102
        const val GOLD_INGOT = 103
        const val DIAMOND = 104
        const val WOODEN_PICKAXE = 105
        const val STONE_PICKAXE = 106
        const val IRON_PICKAXE = 107
        const val DIAMOND_PICKAXE = 108

        fun fromId(id: Int): InventoryItem {
            return if (id < 100) {
                Block(BlockType.fromId(id))
            } else {
                when (id) {
                    STICK -> Special(STICK, "Stick")
                    COAL -> Special(COAL, "Coal")
                    IRON_INGOT -> Special(IRON_INGOT, "Iron Ingot")
                    GOLD_INGOT -> Special(GOLD_INGOT, "Gold Ingot")
                    DIAMOND -> Special(DIAMOND, "Diamond Gems")
                    WOODEN_PICKAXE -> Special(WOODEN_PICKAXE, "Wooden Pickaxe")
                    STONE_PICKAXE -> Special(STONE_PICKAXE, "Stone Pickaxe")
                    IRON_PICKAXE -> Special(IRON_PICKAXE, "Iron Pickaxe")
                    DIAMOND_PICKAXE -> Special(DIAMOND_PICKAXE, "Diamond Pickaxe")
                    else -> Special(999, "Unknown Item")
                }
            }
        }
    }
}

// Crafting Recipe definition
data class CraftingRecipe(
    val id: String,
    val name: String,
    val ingredients: List<Pair<Int, Int>>, // ItemId to Quantity required
    val outputId: Int,
    val outputCount: Int,
    val description: String
)

object CraftingRecipes {
    val recipes = listOf(
        // Oak Log -> Planks
        CraftingRecipe(
            id = "log_to_planks",
            name = "Oak Planks",
            ingredients = listOf(BlockType.OAK_LOG.id to 1),
            outputId = BlockType.PLANKS.id,
            outputCount = 4,
            description = "Basic building material processed from raw wood logs."
        ),
        // Planks -> Sticks
        CraftingRecipe(
            id = "planks_to_sticks",
            name = "Sticks",
            ingredients = listOf(BlockType.PLANKS.id to 2),
            outputId = InventoryItem.STICK,
            outputCount = 4,
            description = "Wooden handles required for crafting digging tools."
        ),
        // Wooden Pickaxe
        CraftingRecipe(
            id = "wooden_pickaxe",
            name = "Wooden Pickaxe",
            ingredients = listOf(
                BlockType.PLANKS.id to 3,
                InventoryItem.STICK to 2
            ),
            outputId = InventoryItem.WOODEN_PICKAXE,
            outputCount = 1,
            description = "Allows breaking cobblestone to gather minerals."
        ),
        // Stone Pickaxe
        CraftingRecipe(
            id = "stone_pickaxe",
            name = "Stone Pickaxe",
            ingredients = listOf(
                BlockType.COBBLESTONE.id to 3,
                InventoryItem.STICK to 2
            ),
            outputId = InventoryItem.STONE_PICKAXE,
            outputCount = 1,
            description = "More durable. Speeds up mining and can harvest Iron Ore."
        ),
        // Iron Pickaxe
        CraftingRecipe(
            id = "iron_pickaxe",
            name = "Iron Pickaxe",
            ingredients = listOf(
                InventoryItem.IRON_INGOT to 3,
                InventoryItem.STICK to 2
            ),
            outputId = InventoryItem.IRON_PICKAXE,
            outputCount = 1,
            description = "High tier tool. Required to harvest Gold and Diamonds."
        ),
        // Diamond Pickaxe
        CraftingRecipe(
            id = "diamond_pickaxe",
            name = "Diamond Pickaxe",
            ingredients = listOf(
                InventoryItem.DIAMOND to 3,
                InventoryItem.STICK to 2
            ),
            outputId = InventoryItem.DIAMOND_PICKAXE,
            outputCount = 1,
            description = "The ultimate indestructible mining implement."
        ),
        // Cobblestone -> Bricks
        CraftingRecipe(
            id = "cobble_to_brick",
            name = "Bricks Block",
            ingredients = listOf(
                BlockType.COBBLESTONE.id to 4
            ),
            outputId = BlockType.BRICK.id,
            outputCount = 2,
            description = "Durable decorative building brick."
        ),
        // TNT Crafting
        CraftingRecipe(
            id = "craft_tnt",
            name = "TNT Explosive",
            ingredients = listOf(
                BlockType.SAND.id to 4,
                InventoryItem.COAL to 2
            ),
            outputId = BlockType.TNT.id,
            outputCount = 1,
            description = "Explosive block. Light next to any structure to detonate!"
        ),
        // Smash Ore -> Smelt (Furnace Action)
        CraftingRecipe(
            id = "smelt_iron",
            name = "Smelt Iron Ingot",
            ingredients = listOf(
                BlockType.IRON_ORE.id to 1,
                InventoryItem.COAL to 1
            ),
            outputId = InventoryItem.IRON_INGOT,
            outputCount = 1,
            description = "Smelts raw iron ore inside a charcoal-fired furnace."
        ),
        // Smelt Gold
        CraftingRecipe(
            id = "smelt_gold",
            name = "Smelt Gold Ingot",
            ingredients = listOf(
                BlockType.GOLD_ORE.id to 1,
                InventoryItem.COAL to 1
            ),
            outputId = InventoryItem.GOLD_INGOT,
            outputCount = 1,
            description = "Smelts raw gold ore into shining gold bars."
        ),
        // Process Diamonds
        CraftingRecipe(
            id = "refine_diamond",
            name = "Refine Diamond",
            ingredients = listOf(
                BlockType.DIAMOND_ORE.id to 1
            ),
            outputId = InventoryItem.DIAMOND,
            outputCount = 1,
            description = "Extracts pure sparkling diamond gems from rough matrix ores."
        ),
        // Redstone
        CraftingRecipe(
            id = "refine_redstone",
            name = "Synthesize Lava",
            ingredients = listOf(
                BlockType.STONE.id to 4,
                InventoryItem.COAL to 4
            ),
            outputId = BlockType.LAVA.id,
            outputCount = 1,
            description = "Infuses molten lava from compressed high-carbon stones."
        )
    )
}
