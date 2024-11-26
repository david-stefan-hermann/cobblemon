package com.cobblemon.mod.common.tms.obtain

import com.cobblemon.mod.common.api.tms.ObtainMethod
import com.cobblemon.mod.common.util.cobblemonResource
import net.minecraft.server.level.ServerPlayer

/**
 * An [ObtainMethod] that unlocks immediately.
 * Used for TMs that aren't unlocked and are available immediately.
 * TMs with this [ObtainMethod] will not display an unlock text.
 *
 * @author whatsy
 */
class NoneObtainMethod : ObtainMethod {

    override val passive = true

    override fun matches(player: ServerPlayer) = true

    companion object {
        val ID = cobblemonResource("none")
    }
}
