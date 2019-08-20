package xyz.prpht.takeinshuffledcycles

import kotlin.random.Random

object OptimizedShuffledCopies : Taker {
    override fun <T> takeInShuffledCycles(random: Random, list: List<T>, n: Int): List<T> {
        val result = ArrayList<T>(n)

        val fullSectionsNum = n / list.size
        repeat(fullSectionsNum) {
            result.addAll(list.shuffled(random))
        }

        val incompleteSectionSize = n - fullSectionsNum * list.size
        if (incompleteSectionSize > 0) {
            val section = list.toMutableList()
            for (i in 0 until incompleteSectionSize) {
                val j = random.nextInt(list.size - i) + i
                result.add(section[j])
                section[j] = section[i]
            }
        }
        return result
    }
}
