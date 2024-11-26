package com.cobblemon.mod.common.tms.obtain

import com.cobblemon.mod.common.api.tms.ObtainMethod
import com.cobblemon.mod.common.util.cobblemonResource
import net.minecraft.server.level.ServerPlayer

/**
 * An [ObtainMethod] that checks the player's Y level.
 *
 * @param yLevel The Y level to check for.
 * @param operator The operator to use. Can be "equals", "less", or "greater".
 * @author whatsy
 */
class PlayerYObtainMethod(val yLevel: Int, val operator: String) : ObtainMethod {

    override val passive = true
    override fun matches(player: ServerPlayer): Boolean {
        when (operator) {
            "equals" -> return (player.y.toInt() == yLevel)
            "less" -> return (yLevel < player.y)
            "greater" -> return (yLevel > player.y)
        }
        return false
    }

    companion object {
        val ID = cobblemonResource("y_level")
    }

}