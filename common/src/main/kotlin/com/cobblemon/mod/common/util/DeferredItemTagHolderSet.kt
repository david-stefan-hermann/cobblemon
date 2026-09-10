/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.util

import com.mojang.datafixers.util.Either
import net.minecraft.core.Holder
import net.minecraft.core.HolderOwner
import net.minecraft.core.HolderSet
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.tags.TagKey
import net.minecraft.util.RandomSource
import net.minecraft.world.item.Item
import java.util.Optional

/**
 * An item tag that is looked up when it is used rather than when it is named.
 *
 * port/26.2: ItemPredicate.Builder.of resolves a tag through a HolderGetter and throws "Missing tag"
 * if it is not loaded yet. Cobblemon reads species data with its own GSON adapters, which run before
 * the tag manager has bound anything, so any evolution keyed on a tag - Grotle's #cobblemon:azalea_tree,
 * for one - killed data loading. On 1.21.1 the tag was resolved lazily and this never came up; this
 * restores that, and an unloaded tag simply matches nothing instead of throwing.
 */
class DeferredItemTagHolderSet(private val tag: TagKey<Item>) : HolderSet<Item> {

    private fun resolved(): HolderSet<Item> =
        BuiltInRegistries.ITEM.get(tag).orElse(null) ?: HolderSet.empty()

    override fun iterator(): MutableIterator<Holder<Item>> = resolved().iterator()
    override fun stream() = resolved().stream()
    override fun size() = resolved().size()
    override fun isBound() = BuiltInRegistries.ITEM.get(tag).isPresent
    override fun unwrap(): Either<TagKey<Item>, List<Holder<Item>>> = Either.left(tag)
    override fun getRandomElement(random: RandomSource): Optional<Holder<Item>> = resolved().getRandomElement(random)
    override fun get(index: Int): Holder<Item> = resolved().get(index)
    override fun contains(holder: Holder<Item>) = resolved().contains(holder)
    override fun canSerializeIn(owner: HolderOwner<Item>) = true
    override fun unwrapKey(): Optional<TagKey<Item>> = Optional.of(tag)
}
