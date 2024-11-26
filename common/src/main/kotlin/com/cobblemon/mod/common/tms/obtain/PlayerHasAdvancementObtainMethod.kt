package com.cobblemon.mod.common.tms.obtain

import com.cobblemon.mod.common.api.tms.ObtainMethod
import com.cobblemon.mod.common.util.cobblemonResource
import net.minecraft.server.level.ServerPlayer
import net.minecraft.resources.ResourceLocation

/**
 * An [ObtainMethod] that triggers when the player has an advancement.
 */
class PlayerHasAdvancementObtainMethod : ObtainMethod {

    companion object {
        val ID = cobblemonResource("advancement")
    }

    override val passive = true
    val advancement: ResourceLocation? = null

    override fun matches(player: ServerPlayer): Boolean {
        if (advancement == null) return false

        for (entry in player.advancements.advancements) {
            if (entry.key.id == advancement && entry.value.isDone) return true
        }
        return false
    }
}
