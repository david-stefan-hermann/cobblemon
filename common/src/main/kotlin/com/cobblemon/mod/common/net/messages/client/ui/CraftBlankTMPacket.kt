package com.cobblemon.mod.common.net.messages.client.ui

import com.cobblemon.mod.common.api.net.NetworkPacket
import com.cobblemon.mod.common.util.cobblemonResource
import com.cobblemon.mod.common.util.readItemStack
import com.cobblemon.mod.common.util.writeItemStack
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.world.item.ItemStack

/**
 * Tells the server to attempt crafting a Blank TM using the [TMBlock]
 *
 * Handled by [CraftBlankTMPacketHandler]
 *
 * @author whatsy
 */
class CraftBlankTMPacket(
    val ingredient: ItemStack
): NetworkPacket<CraftBlankTMPacket> {
    override val id = ID

    override fun encode(buffer: RegistryFriendlyByteBuf) {
        buffer.writeItemStack(ingredient)
    }

    companion object {
        val ID = cobblemonResource("craft_blank_tm")

        fun decode(buffer: RegistryFriendlyByteBuf) = CraftBlankTMPacket(
            buffer.readItemStack()
        )
    }

}