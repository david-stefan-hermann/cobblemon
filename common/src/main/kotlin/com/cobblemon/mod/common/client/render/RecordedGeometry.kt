/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.render

import com.mojang.blaze3d.vertex.VertexConsumer

/**
 * port/26.2: a [VertexConsumer] that stores fully transformed vertices so they can be replayed later.
 *
 * The submit pipeline runs geometry callbacks in a later render pass, but Cobblemon poses shared model
 * instances in place: by the time a deferred callback ran, the bones held another entity's pose and even
 * another species' poser, which drew Pokémon as scrambled parts. Recording while the model is still posed
 * for this entity and replaying the finished vertices keeps each draw independent of later model state.
 *
 * Vertices are recorded with whatever attributes the drawing code set, and replayed in the same order.
 */
class RecordedGeometry : VertexConsumer {
    private var floats = FloatArray(INITIAL_VERTICES * FLOATS_PER_VERTEX)
    private var ints = IntArray(INITIAL_VERTICES * INTS_PER_VERTEX)
    private var vertexCount = 0

    val isEmpty: Boolean
        get() = vertexCount == 0

    private val f: Int
        get() = (vertexCount - 1) * FLOATS_PER_VERTEX
    private val i: Int
        get() = (vertexCount - 1) * INTS_PER_VERTEX

    override fun addVertex(x: Float, y: Float, z: Float): VertexConsumer {
        if (vertexCount * FLOATS_PER_VERTEX >= floats.size) {
            floats = floats.copyOf(floats.size * 2)
            ints = ints.copyOf(ints.size * 2)
        }
        vertexCount++
        val fo = f
        floats[fo] = x
        floats[fo + 1] = y
        floats[fo + 2] = z
        ints[i + FLAGS] = 0
        return this
    }

    override fun setColor(red: Int, green: Int, blue: Int, alpha: Int): VertexConsumer =
        setColor((alpha and 0xFF) shl 24 or ((red and 0xFF) shl 16) or ((green and 0xFF) shl 8) or (blue and 0xFF))

    override fun setColor(color: Int): VertexConsumer {
        ints[i + COLOR] = color
        ints[i + FLAGS] = ints[i + FLAGS] or HAS_COLOR
        return this
    }

    override fun setUv(u: Float, v: Float): VertexConsumer {
        floats[f + 3] = u
        floats[f + 4] = v
        ints[i + FLAGS] = ints[i + FLAGS] or HAS_UV
        return this
    }

    override fun setUv1(u: Int, v: Int): VertexConsumer {
        ints[i + UV1_U] = u
        ints[i + UV1_V] = v
        ints[i + FLAGS] = ints[i + FLAGS] or HAS_UV1
        return this
    }

    override fun setUv2(u: Int, v: Int): VertexConsumer {
        ints[i + UV2_U] = u
        ints[i + UV2_V] = v
        ints[i + FLAGS] = ints[i + FLAGS] or HAS_UV2
        return this
    }

    override fun setNormal(x: Float, y: Float, z: Float): VertexConsumer {
        val fo = f
        floats[fo + 5] = x
        floats[fo + 6] = y
        floats[fo + 7] = z
        ints[i + FLAGS] = ints[i + FLAGS] or HAS_NORMAL
        return this
    }

    override fun setLineWidth(width: Float): VertexConsumer {
        floats[f + 8] = width
        ints[i + FLAGS] = ints[i + FLAGS] or HAS_LINE_WIDTH
        return this
    }

    /** Writes every recorded vertex into [consumer]. The positions are already transformed. */
    fun replay(consumer: VertexConsumer) {
        for (vertex in 0 until vertexCount) {
            val fo = vertex * FLOATS_PER_VERTEX
            val io = vertex * INTS_PER_VERTEX
            val flags = ints[io + FLAGS]
            consumer.addVertex(floats[fo], floats[fo + 1], floats[fo + 2])
            if (flags and HAS_COLOR != 0) consumer.setColor(ints[io + COLOR])
            if (flags and HAS_UV != 0) consumer.setUv(floats[fo + 3], floats[fo + 4])
            if (flags and HAS_UV1 != 0) consumer.setUv1(ints[io + UV1_U], ints[io + UV1_V])
            if (flags and HAS_UV2 != 0) consumer.setUv2(ints[io + UV2_U], ints[io + UV2_V])
            if (flags and HAS_NORMAL != 0) consumer.setNormal(floats[fo + 5], floats[fo + 6], floats[fo + 7])
            if (flags and HAS_LINE_WIDTH != 0) consumer.setLineWidth(floats[fo + 8])
        }
    }

    companion object {
        private const val INITIAL_VERTICES = 256

        // x, y, z, u, v, nx, ny, nz, lineWidth
        private const val FLOATS_PER_VERTEX = 9
        // flags, color, uv1 u/v, uv2 u/v
        private const val INTS_PER_VERTEX = 6
        private const val FLAGS = 0
        private const val COLOR = 1
        private const val UV1_U = 2
        private const val UV1_V = 3
        private const val UV2_U = 4
        private const val UV2_V = 5

        private const val HAS_COLOR = 1
        private const val HAS_UV = 2
        private const val HAS_UV1 = 4
        private const val HAS_UV2 = 8
        private const val HAS_NORMAL = 16
        private const val HAS_LINE_WIDTH = 32
    }
}
