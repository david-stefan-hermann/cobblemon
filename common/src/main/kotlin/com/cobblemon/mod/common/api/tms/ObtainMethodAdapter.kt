/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.api.tms

import com.google.gson.JsonDeserializer
import com.google.gson.JsonSerializer
import net.minecraft.resources.ResourceLocation
import kotlin.reflect.KClass

/**
 * A type adapter for [ObtainMethod]s.
 * For the default implementation see [CobblemonObtainMethodAdapter].
 *
 * @author Apion
 * @since November 22nd, 2023
 */
interface ObtainMethodAdapter : JsonDeserializer<ObtainMethod>, JsonSerializer<ObtainMethod> {
    /**
     * Register a [ObtainMethod] to be used by this adapter.
     *
     * @param type The [KClass] of the [ObtainMethod].
     * @param identifier The expected [ResourceLocation] in the parsed JSON.
     */
    fun register(type: KClass<out ObtainMethod>, identifier: ResourceLocation)
}
