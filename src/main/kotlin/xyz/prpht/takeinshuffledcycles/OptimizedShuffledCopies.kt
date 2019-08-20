package xyz.prpht.takeinshuffledcycles

import kotlin.math.min
import kotlin.random.Random

object OptimizedShuffledCopies : Taker {
    override fun <T> takeInShuffledCycles(random: Random, list: List<T>, n: Int): List<T> {
        val result = ArrayList<T>(n)
        val section = list.toMutableList()

        var left = n
        while (left > 0) {
            val sectionSize = min(left, list.size)
            left -= list.size

            for (i in 0 until sectionSize) {
                val j = random.nextInt(list.size - i) + i
                result.add(section[j])
                section[j] = section[i]
            }

            if (left > 0) {
                list.indices.forEach { i ->
                    section[i] = list[i]
                }
            }
        }
        return result
    }
}
