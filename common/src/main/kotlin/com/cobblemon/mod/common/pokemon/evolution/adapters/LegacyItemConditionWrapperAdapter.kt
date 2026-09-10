/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.pokemon.evolution.adapters

import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.util.adapters.CodecBackedAdapter
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.cobblemon.mod.common.util.DeferredItemTagHolderSet
import net.minecraft.advancements.predicates.DataComponentMatchers
import net.minecraft.advancements.predicates.ItemPredicate
import net.minecraft.advancements.predicates.MinMaxBounds
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier
import net.minecraft.tags.TagKey
import java.lang.reflect.Type
import java.util.Optional

/**
 * Reads the short form Cobblemon's own data is written in - `"cobblemon:sun_stone"` or `"#c:foods"` -
 * as well as a full [ItemPredicate] object.
 *
 * port/26.2: this had been reduced to the codec-backed path alone, which reads only the object form,
 * so every species that evolves by item died on "Not a JSON object". The builder takes a HolderGetter
 * now rather than the item or tag directly, and the legacy `{item, nbt}` pair has no counterpart at
 * all: item NBT became data components, and ItemSubPredicates.CUSTOM_DATA is gone with it. That form
 * is reported rather than silently matching everything.
 */
object LegacyItemConditionWrapperAdapter : JsonDeserializer<ItemPredicate>, JsonSerializer<ItemPredicate> {

    private const val TAG_PREFIX = "#"
    private const val LEGACY_ITEM = "item"
    private const val LEGACY_NBT = "nbt"

    private val codecAdapter = CodecBackedAdapter(ItemPredicate.CODEC)

    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): ItemPredicate {
        if (json.isJsonPrimitive) {
            return this.predicateFor(json.asString)
        }
        if (json.isJsonObject) {
            val jObject = json.asJsonObject
            if (jObject.size() == 2 && jObject.has(LEGACY_ITEM) && jObject.has(LEGACY_NBT)) {
                Cobblemon.LOGGER.error(
                    "The legacy item + nbt predicate on {} cannot be read on 26.2 - item NBT became data components. Matching on the item alone.",
                    jObject.get(LEGACY_ITEM).asString
                )
                return this.predicateFor(jObject.get(LEGACY_ITEM).asString)
            }
        }
        return this.codecAdapter.deserialize(json, typeOfT, context)
    }

    override fun serialize(src: ItemPredicate, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        return this.codecAdapter.serialize(src, typeOfSrc, context)
    }

    private fun predicateFor(raw: String): ItemPredicate {
        val isTag = raw.startsWith(TAG_PREFIX)
        if (isTag) {
            val id = Identifier.read(raw.substring(1)).result().orElse(null)
                ?: return ItemPredicate.Builder.item().build()
            return ItemPredicate(
                Optional.of(DeferredItemTagHolderSet(TagKey.create(Registries.ITEM, id))),
                MinMaxBounds.Ints.ANY,
                DataComponentMatchers.ANY
            )
        }
        return this.itemPredicateFor(raw)
    }

    private fun itemPredicateFor(raw: String): ItemPredicate {
        val builder = ItemPredicate.Builder.item()
        val id = Identifier.read(raw).result().orElse(null)
        if (id == null) {
            Cobblemon.LOGGER.error("{} is not a valid item id in an item predicate", raw)
            return builder.build()
        }
        val item = BuiltInRegistries.ITEM.getOptional(id).orElse(null)
        if (item == null) {
            Cobblemon.LOGGER.error("Unknown item {} in an item predicate", raw)
            return builder.build()
        }
        return builder.of(BuiltInRegistries.ITEM, item).build()
    }

}
