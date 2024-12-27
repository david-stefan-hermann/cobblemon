package com.cobblemon.mod.common.net.messages.client.ui

import com.cobblemon.mod.common.api.net.NetworkPacket
import com.cobblemon.mod.common.api.tms.TechnicalMachine
import com.cobblemon.mod.common.api.tms.TechnicalMachines
import com.cobblemon.mod.common.util.cobblemonResource
import com.cobblemon.mod.common.util.readItemStack
import com.cobblemon.mod.common.util.writeItemStack
import net.minecraft.item.ItemStack
import net.minecraft.network.PacketByteBuf
import java.util.UUID
import net.minecraft.network.RegistryByteBuf

/**
 * Tells the server to attempt crafting a [TechnicalMachineItem] using the [TMBlock]
 *
 * Handled by [CraftTMPacketHandler]
 *
 * @author whatsy
 */
class CraftTMPacket(
    val tm: TechnicalMachine,
    val disc: ItemStack,
    val gem: ItemStack,
    val ingredient: ItemStack
): NetworkPacket<CraftTMPacket> {
    override val id = ID

    override fun encode(buffer: RegistryByteBuf) {
        buffer.writeIdentifier(tm.id)
        buffer.writeItemStack(disc)
        buffer.writeItemStack(gem)
        buffer.writeItemStack(ingredient)
    }

    companion object {
        val ID = cobblemonResource("craft_tm")

        fun decode(buffer: RegistryByteBuf) = CraftTMPacket(
            TechnicalMachines.tmMap[buffer.readIdentifier()]!!,
            buffer.readItemStack(), buffer.readItemStack(), buffer.readItemStack()
        )
    }

}