package com.cobblemon.mod.common.client.net.gui

import com.cobblemon.mod.common.api.net.ClientNetworkPacketHandler
import com.cobblemon.mod.common.net.messages.client.ui.OpenTMMPacket
import net.minecraft.client.Minecraft

object OpenTMMPacketHandler : ClientNetworkPacketHandler<OpenTMMPacket> {
    override fun handle(packet: OpenTMMPacket, client: Minecraft) {
        //client.setScreen()
    }
}
