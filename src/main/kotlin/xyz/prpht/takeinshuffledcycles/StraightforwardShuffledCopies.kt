package xyz.prpht.takeinshuffledcycles

import kotlin.random.Random

object StraightforwardShuffledCopies : Taker {
    override fun <T> takeInShuffledCycles(random: Random, list: List<T>, n: Int): List<T> {
        val sectionsNumber = (n - 1) / list.size + 1
        return (0 until sectionsNumber).flatMap {
            list.shuffled(random)
        }.take(n)
    }
}
