/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.api.pokemon.feature

import com.cobblemon.mod.common.api.pokemon.PokemonProperties
import com.cobblemon.mod.common.api.pokemon.aspect.AspectProvider
import com.cobblemon.mod.common.api.properties.CustomPokemonPropertyType
import com.cobblemon.mod.common.client.gui.summary.featurerenderers.SummarySpeciesFeatureRenderer
import com.cobblemon.mod.common.pokemon.Pokemon
import com.cobblemon.mod.common.util.readString
import com.cobblemon.mod.common.util.substitute
import com.cobblemon.mod.common.util.writeString
import com.google.gson.JsonObject
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.world.entity.ai.behavior.ShufflingList

/**
 * A [SpeciesFeatureProvider] which is a string value selected from a fixed map of choices and their weights. Parameters exist
 * to change default behaviour, aspects, and the available choices. Choices must be lowercase.
 *
 * @author Hiroku
 * @since November 30th, 2022
 */
class WeightedChoiceSpeciesFeatureProvider(
    override var keys: List<String>,
    var choices: Map<String, Int> = mutableMapOf(),
    var isAspect: Boolean = true,
    var aspectFormat: String = "{{choice}}"
) : SynchronizedSpeciesFeatureProvider<StringSpeciesFeature>, CustomPokemonPropertyType<StringSpeciesFeature>,
    AspectProvider {
    override var needsKey = true
    override var visible = false
    fun getAspect(feature: StringSpeciesFeature) = aspectFormat.substitute("choice", feature.value)

    override fun saveToBuffer(buffer: RegistryFriendlyByteBuf, toClient: Boolean) {
        buffer.writeCollection(keys) { _, value -> buffer.writeString(value) }
        buffer.writeMap(
            choices,
            { _, value -> buffer.writeString(value) },
            { _, value -> buffer.writeInt(value) }
        )
        buffer.writeBoolean(isAspect)
        buffer.writeString(aspectFormat)
        buffer.writeBoolean(needsKey)
    }

    override fun loadFromBuffer(buffer: RegistryFriendlyByteBuf) {
        keys = buffer.readList { buffer.readString() }
        choices = buffer.readMap({ buffer.readString() }, { buffer.readInt() })
        isAspect = buffer.readBoolean()
        aspectFormat = buffer.readString()
        needsKey = buffer.readBoolean()
    }

    override fun getRenderer(pokemon: Pokemon): SummarySpeciesFeatureRenderer<StringSpeciesFeature>? {
        return null
    }

    override fun invoke(buffer: RegistryFriendlyByteBuf, name: String): StringSpeciesFeature? {
        return if (name in keys) {
            StringSpeciesFeature(name, "").also { it.loadFromBuffer(buffer) }
        } else {
            null
        }
    }

    fun getAllAspects(): ShufflingList<String> {
        val aspects = ShufflingList<String>()

        choices.forEach { (choice, weight) ->
            aspects.add(aspectFormat.substitute("choice", choice), weight)
        }

        return aspects
    }

    override fun examples() = choices.map { it.key }

    internal constructor() : this(emptyList())

    override fun get(pokemon: Pokemon): StringSpeciesFeature? {
        return pokemon.features.filterIsInstance<StringSpeciesFeature>().find { it.name in keys }
    }

    override fun invoke(pokemon: Pokemon): StringSpeciesFeature? {
        val existing = get(pokemon)
        return if (existing != null && existing.value in choices) {
            existing
        } else {
            val value = with(ShufflingList<String>()) {
                choices.forEach { (choice, weight) -> this.add(choice, weight) }
                return@with this.shuffle().first()
                    ?: throw IllegalStateException("The 'choices' list is empty for species feature provider: ${keys.joinToString()}")
            }

            fromString(value)
        }
    }

    override fun invoke(nbt: CompoundTag): StringSpeciesFeature? {
        val key = keys.find { nbt.contains(it) }
        if (key == null) return null
        return StringSpeciesFeature(key, "").also { it.loadFromNBT(nbt) }
    }

    override fun invoke(json: JsonObject): StringSpeciesFeature? {
        val key = keys.find { json.has(it) }
        if (key == null) return null
        return StringSpeciesFeature(key, "").also { it.loadFromJSON(json) }
    }

    override fun fromString(value: String?): StringSpeciesFeature? {
        val lower = value?.lowercase()
        if (lower == null || lower !in choices) {
            return null
        }

        return StringSpeciesFeature(keys.first(), lower)
    }

    override fun provide(pokemon: Pokemon): Set<String> {
        return if (isAspect) {
            get(pokemon)?.let { setOf(getAspect(it)) } ?: emptySet()
        } else {
            emptySet()
        }
    }

    override fun provide(properties: PokemonProperties): Set<String> {
        return if (isAspect) {
            val feature = properties.customProperties.filterIsInstance<StringSpeciesFeature>().find { it.name in keys }
            if (feature != null) {
                setOf(getAspect(feature))
            } else {
                emptySet()
            }
        } else {
            emptySet()
        }
    }
}
