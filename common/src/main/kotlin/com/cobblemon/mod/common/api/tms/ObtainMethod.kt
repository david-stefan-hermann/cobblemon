/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.api.tms

import net.minecraft.server.level.ServerPlayer

interface ObtainMethod {
    /**
     * Whether to check this [ObtainMethod] once every second.
     */
    val passive: Boolean

    /**
     * Checks if the [ObtainMethod] condition is true.
     *
     * @param player The player to use when checking.
     * @return Whether the check succeeded.
     */
    fun matches(player: ServerPlayer): Boolean
}