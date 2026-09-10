/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.render.atlas

import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.texture.TextureAtlas
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.resources.Identifier
import kotlin.math.roundToInt

/**
 * port/26.2: TextureAtlasHolder is gone. Atlases are owned by the central [net.minecraft.client
 * .resources.model.sprite.AtlasManager], which stitches and reloads them, and a mod registers one by
 * handing the manager an AtlasConfig rather than by subclassing a holder.
 *
 * So this is a plain descriptor: it names the atlas and looks the stitched result up on demand. The
 * platform registers it - see CobblemonFabricClient - and nothing here needs a reload hook any more,
 * because the manager reloads every registered atlas itself.
 */
class CobblemonAtlas(
    val textureId: Identifier,
    val definitionLocation: Identifier
) {
    /** The stitched atlas. Only valid once resources have loaded. */
    val textureAtlas: TextureAtlas
        get() = Minecraft.getInstance().atlasManager.getAtlasOrThrow(textureId)

    fun getSprite(id: Identifier): TextureAtlasSprite = textureAtlas.getSprite(id)

    /**
     * port/26.2: TextureAtlas no longer exposes its own width and height. A sprite's UV span is its
     * pixel size divided by the atlas size, so the atlas size is recovered exactly from any sprite on it.
     */
    val width: Int
        get() = dimensionFrom { sprite -> sprite.contents().width() / (sprite.u1 - sprite.u0) }

    val height: Int
        get() = dimensionFrom { sprite -> sprite.contents().height() / (sprite.v1 - sprite.v0) }

    private inline fun dimensionFrom(measure: (TextureAtlasSprite) -> Float): Int {
        val sprite = textureAtlas.missingSprite()
        return measure(sprite).roundToInt()
    }
}
