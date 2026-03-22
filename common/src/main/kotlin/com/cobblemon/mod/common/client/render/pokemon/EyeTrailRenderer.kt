/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.render.pokemon

import com.cobblemon.mod.common.client.render.MatrixWrapper
import net.minecraft.client.renderer.RenderType
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.world.phys.Vec3
import org.joml.Vector3f

/**
 *
 * Renders an alpha eye trail based on a list of previous eye locator positions
 *
 * @author Jackson
 * @since March 21st, 2026
 */
private const val TRAIL_MAX_WIDTH = 0.015f
private const val TRAIL_MAX_ALPHA = 0.8f
private val TRAIL_COLOR = Vector3f(0.7f, 0.0f, 0.0f)
private const val TRAIL_DURATION_MS = 250 // lookback distance in ms for previous eye positions for trail rendering

private val EYE_LOCATOR_NAMES = setOf(
    "eye_left", "eye_right", "eye",
    "locator_eye_left", "locator_eye_right",
    "eye1", "eye2"
)

/**
 * Updates the eye trail positions based on the current entity position and locator states
 */
fun updateEyeTrail(
    aspects: Set<String>,
    locatorStates: Map<String, MatrixWrapper>,
    entityPos: Vec3,
    trailPositions: MutableMap<String, ArrayDeque<Pair<Vec3, Long>>>
) {
    if ("alpha_eyes" !in aspects) {
        trailPositions.clear()
        return
    }
    for (locatorName in EYE_LOCATOR_NAMES) {
        val wrapper = locatorStates[locatorName] ?: continue
        val pos = wrapper.matrix.getTranslation(Vector3f())
        val worldPos = Vec3(
            entityPos.x + pos.x,
            entityPos.y + pos.y,
            entityPos.z + pos.z
        )
        val now = System.currentTimeMillis()
        val deque = trailPositions.getOrPut(locatorName) { ArrayDeque() }
        deque.addLast(worldPos to now)
        while (deque.isNotEmpty() && now - deque.first().second > TRAIL_DURATION_MS) {
            deque.removeFirst()
        }
    }
}

fun renderEyeTrail(
    positions: ArrayDeque<Pair<Vec3, Long>>,
    entityPos: Vec3,
    camPos: Vec3,
    poseStack: PoseStack,
    bufferSource: MultiBufferSource
) {
    if (positions.size < 2) return

    val consumer = bufferSource.getBuffer(RenderType.lightning())
    val matrix = poseStack.last().pose()
    val n = positions.size

    for (i in 0 until n - 1) {
        val curr = positions[i].first
        val next = positions[i + 1].first

        val tCurr = i.toFloat() / (n - 1)
        val tNext = (i + 1).toFloat() / (n - 1)

        val wCurr = TRAIL_MAX_WIDTH * tCurr
        val wNext = TRAIL_MAX_WIDTH * tNext
        val aCurr = TRAIL_MAX_ALPHA * tCurr
        val aNext = TRAIL_MAX_ALPHA * tNext

        val localCurr = curr.subtract(entityPos)
        val localNext = next.subtract(entityPos)

        val dir = next.subtract(curr).normalize()
        val toCamera = camPos.subtract(curr).normalize()
        val perp = dir.cross(toCamera).normalize()

        val px = perp.x.toFloat()
        val py = perp.y.toFloat()
        val pz = perp.z.toFloat()

        val r = TRAIL_COLOR.x
        val g = TRAIL_COLOR.y
        val b = TRAIL_COLOR.z

        consumer.addVertex(matrix,
            (localCurr.x - px * wCurr).toFloat(),
            (localCurr.y - py * wCurr).toFloat(),
            (localCurr.z - pz * wCurr).toFloat()
        ).setColor(r, g, b, aCurr)

        consumer.addVertex(matrix,
            (localCurr.x + px * wCurr).toFloat(),
            (localCurr.y + py * wCurr).toFloat(),
            (localCurr.z + pz * wCurr).toFloat()
        ).setColor(r, g, b, aCurr)

        consumer.addVertex(matrix,
            (localNext.x + px * wNext).toFloat(),
            (localNext.y + py * wNext).toFloat(),
            (localNext.z + pz * wNext).toFloat()
        ).setColor(r, g, b, aNext)

        consumer.addVertex(matrix,
            (localNext.x - px * wNext).toFloat(),
            (localNext.y - py * wNext).toFloat(),
            (localNext.z - pz * wNext).toFloat()
        ).setColor(r, g, b, aNext)
    }
}
