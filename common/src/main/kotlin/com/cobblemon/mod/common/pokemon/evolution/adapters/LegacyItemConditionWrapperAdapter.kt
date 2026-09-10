/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.pokemon.evolution.adapters

import com.cobblemon.mod.common.util.adapters.CodecBackedAdapter
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import net.minecraft.advancements.predicates.ItemPredicate
import java.lang.reflect.Type

// PT137: This adapter is legacy compat for pre-data-component ItemPredicate JSON.
// MC 26.1.x removed ItemCustomDataPredicate / ItemSubPredicates; legacy NBT branch and
// builder.of(Tag/Item) calls require HolderGetter<Item> + new component system.
// Delegating fully to codec-backed adapter — legacy short-form unsupported in 26.1+.
object LegacyItemConditionWrapperAdapter : JsonDeserializer<ItemPredicate>, JsonSerializer<ItemPredicate> {

    private val codecAdapter = CodecBackedAdapter(ItemPredicate.CODEC)

    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): ItemPredicate {
        return this.codecAdapter.deserialize(json, typeOfT, context)
    }

    override fun serialize(src: ItemPredicate, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        return this.codecAdapter.serialize(src, typeOfSrc, context)
    }

}