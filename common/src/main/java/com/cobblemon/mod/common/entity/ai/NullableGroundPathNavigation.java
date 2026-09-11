/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.entity.ai;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.Path;
import org.jetbrains.annotations.Nullable;

/**
 * port/26.2: GroundPathNavigation is null-marked, so Kotlin types createPath as returning a non-null Path - but
 * it still returns null whenever no path exists. A Kotlin override (or a Kotlin call to super) then throws
 * "createPath(...) must not be null" from the generated null check, which crashed the server tick for any
 * Pokémon with an unreachable walk target. This shim restores the nullable contract OmniPathNavigation had on
 * 1.21.1: Kotlin implements [createPathOrNull] and callers see Path? again.
 */
public abstract class NullableGroundPathNavigation extends GroundPathNavigation {
    protected NullableGroundPathNavigation(Mob mob, Level level) {
        super(mob, level);
    }

    @Override
    public @Nullable Path createPath(BlockPos pos, int reachRange) {
        return createPathOrNull(pos, reachRange);
    }

    @Override
    public @Nullable Path createPath(Entity target, int reachRange) {
        return createPathOrNull(target, reachRange);
    }

    protected abstract @Nullable Path createPathOrNull(BlockPos pos, int reachRange);

    protected abstract @Nullable Path createPathOrNull(Entity target, int reachRange);

    /** GroundPathNavigation's own createPath(BlockPos, int), with its real (nullable) result. */
    protected final @Nullable Path groundCreatePath(BlockPos pos, int reachRange) {
        return super.createPath(pos, reachRange);
    }
}
