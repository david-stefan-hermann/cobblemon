/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.util

import com.google.gson.Gson
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import java.util.Locale

/**
 * port/26.2: net.minecraft.util.LowerCaseEnumTypeAdapterFactory was removed from vanilla. Cobblemon's
 * data files serialise enums in lower case, so an equivalent factory is provided here.
 *
 * Enum constants are written as their lower-cased name and read back case-insensitively.
 */
class LowerCaseEnumTypeAdapterFactory : TypeAdapterFactory {
    override fun <T : Any?> create(gson: Gson, type: TypeToken<T>): TypeAdapter<T>? {
        @Suppress("UNCHECKED_CAST")
        var rawType = type.rawType as Class<in T>
        if (!rawType.isEnum) {
            // Constants with a class body compile to an anonymous subclass of the enum.
            rawType = rawType.superclass ?: return null
            if (!rawType.isEnum) {
                return null
            }
        }

        val byLowerCaseName = HashMap<String, T>()
        for (constant in rawType.enumConstants ?: return null) {
            @Suppress("UNCHECKED_CAST")
            val value = constant as T
            byLowerCaseName[toLowerCaseName(value)] = value
        }

        return object : TypeAdapter<T>() {
            override fun write(out: JsonWriter, value: T) {
                if (value == null) {
                    out.nullValue()
                } else {
                    out.value(toLowerCaseName(value))
                }
            }

            override fun read(reader: JsonReader): T? {
                if (reader.peek() == JsonToken.NULL) {
                    reader.nextNull()
                    return null
                }
                return byLowerCaseName[reader.nextString().lowercase(Locale.ROOT)]
            }
        }
    }

    private fun <T> toLowerCaseName(value: T): String =
        (value as Enum<*>).name.lowercase(Locale.ROOT)
}
