package xyz.prpht.takeinshuffledcycles

import kotlin.random.Random

interface Taker {
    fun <T> takeInShuffledCycles(random: Random, list: List<T>, n: Int): List<T>
}
