package com.agopsagopyan.randomlauncher.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import kotlin.random.Random

class ShufflerTest {
    @Test
    fun noAppKeepsItsCell() {
        val random = Random(42)
        var order = (1..30).map { "app$it" }
        repeat(500) {
            val next = Shuffler.shuffle(order, order, random)
            assertEquals(order.toSet(), next.toSet())
            next.forEachIndexed { i, id -> assertNotEquals(order[i], id) }
            order = next
        }
    }

    @Test
    fun twoAppsAlwaysSwap() {
        assertEquals(listOf("b", "a"), Shuffler.shuffle(listOf("a", "b"), listOf("a", "b"), Random(1)))
    }

    @Test
    fun handlesInstallsAndRemovals() {
        val random = Random(7)
        val previous = listOf("a", "b", "c", "d", "e")
        val current = listOf("a", "c", "e", "f", "g", "h")
        repeat(200) {
            val next = Shuffler.shuffle(current, previous, random)
            assertEquals(current.toSet(), next.toSet())
            next.forEachIndexed { i, id -> if (id in previous) assertNotEquals(previous.indexOf(id), i) }
        }
    }

    @Test
    fun singleAppIsUntouched() {
        assertEquals(listOf("a"), Shuffler.shuffle(listOf("a"), listOf("a")))
    }
}
