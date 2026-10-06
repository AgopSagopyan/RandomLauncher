package com.agopsagopyan.randomlauncher.domain

import kotlin.random.Random

/**
 * Produces a new grid order where no app that existed in [previous] lands on the
 * same index it had before. That's the whole point of the launcher: the cell your
 * thumb remembers is always wrong.
 */
object Shuffler {
    private const val MAX_ATTEMPTS = 200

    fun shuffle(ids: List<String>, previous: List<String>, random: Random = Random.Default): List<String> {
        if (ids.size < 2) return ids
        val oldIndex = previous.withIndex().associate { (i, id) -> id to i }

        // A random permutation is a valid derangement with probability ~1/e,
        // so rejection sampling almost always finishes in a handful of tries.
        repeat(MAX_ATTEMPTS) {
            val candidate = ids.shuffled(random)
            if (candidate.indices.none { oldIndex[candidate[it]] == it }) return candidate
        }
        return repair(ids.shuffled(random), oldIndex, random)
    }

    /** Fallback for pathological inputs: swap each offender with a cell that fixes both sides. */
    private fun repair(order: List<String>, oldIndex: Map<String, Int>, random: Random): List<String> {
        val result = order.toMutableList()
        for (i in result.indices) {
            if (oldIndex[result[i]] != i) continue
            val j = result.indices.shuffled(random).firstOrNull { j ->
                j != i && oldIndex[result[j]] != i && oldIndex[result[i]] != j
            } ?: continue
            result[i] = result[j].also { result[j] = result[i] }
        }
        return result
    }
}
