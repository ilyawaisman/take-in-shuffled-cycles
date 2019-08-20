package xyz.prpht.takeinshuffledcycles

import kotlin.random.Random

object ViaSequence : Taker {
    override fun <T> takeInShuffledCycles(random: Random, list: List<T>, n: Int): List<T> = sequence {
        val section = list.toMutableList()
        while (true) {
            list.indices.forEach { i ->
                val j = random.nextInt(list.size - i) + i
                yield(section[j])
                section[j] = section[i]
            }
            list.indices.forEach { i ->
                section[i] = list[i]
            }
        }
    }.take(n).toList()
}
