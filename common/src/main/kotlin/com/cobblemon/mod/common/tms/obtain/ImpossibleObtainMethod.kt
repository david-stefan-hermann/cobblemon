package com.cobblemon.mod.common.tms.obtain

import com.cobblemon.mod.common.api.tms.ObtainMethod
import com.cobblemon.mod.common.util.cobblemonResource
import net.minecraft.server.level.ServerPlayer

/**
 * An [ObtainMethod] that never triggers.
 * Useful since the obtainMethods field in TM JSONs cannot be blank.
 *
 * @author whatsy
 */
class ImpossibleObtainMethod : ObtainMethod {

    override val passive = false

    companion object {
        val ID = cobblemonResource("impossible")
    }

    override fun matches(player: ServerPlayer) = false
}