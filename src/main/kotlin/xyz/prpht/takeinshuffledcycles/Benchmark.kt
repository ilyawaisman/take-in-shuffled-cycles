package xyz.prpht.takeinshuffledcycles

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlin.math.round
import kotlin.random.Random
import kotlin.system.measureNanoTime

private const val warmUpRuns = 0x10000
private val hline = "=".repeat(0x40)

fun main() {
    val takers = listOf(
            StraightforwardShuffledCopies,
            ViaSequence,
            OptimizedShuffledCopies
    )

    val seqBenchmark = Benchmark(0x1000, 0x100, 3.5f, false)
    val parBenchmark = seqBenchmark.withParallel(true)

    val benchmarks = listOf(
            seqBenchmark,
            seqBenchmark.withListSize(0x40),
            seqBenchmark.withListSize(0x400),
            seqBenchmark.withTakeCycles(0.5f),
            seqBenchmark.withTakeCycles(9.5f),
            seqBenchmark.withTakeCycles(1f),
            seqBenchmark.withTakeCycles(5f),

            parBenchmark,
            parBenchmark.withListSize(0x40),
            parBenchmark.withListSize(0x400),
            parBenchmark.withTakeCycles(0.5f),
            parBenchmark.withTakeCycles(9.5f),
            parBenchmark.withTakeCycles(1f),
            parBenchmark.withTakeCycles(5f)
    )

    benchmarks.forEach { benchmark ->
        println(benchmark)
        takers.forEach { taker ->
            println("${benchmark.measure(taker)} nanos -- ${taker::class.simpleName}")
        }
        println(hline)
        println()
    }
}

class Benchmark(
        private val testRuns: Int,
        private val listSize: Int,
        private val takeCycles: Float,
        private val isPar: Boolean
) {
    private val random = Random
    private val list = (0 until listSize).toList()
    private val elementsToTake: Int = round(listSize * takeCycles).toInt()
    private val performRuns = if (isPar) ::performRunsPar else ::performRunsSeq

    fun measure(taker: Taker): Long {
        performRuns(taker, warmUpRuns)

        return measureNanoTime {
            performRuns(taker, testRuns)
        } / testRuns
    }

    private fun performRunsSeq(taker: Taker, numCycles: Int) {
        repeat(numCycles) { performRun(taker) }
    }

    private fun performRunsPar(taker: Taker, numCycles: Int) {
        runBlocking {
            withContext(Dispatchers.Default) {
                (0 until numCycles).map { async { performRun(taker) } }.forEach { it.await() }
            }
        }
    }

    private fun performRun(taker: Taker) {
        taker.takeInShuffledCycles(random, list, elementsToTake)
    }

    override fun toString() = "Benchmark[mode: ${if (isPar) "par" else "seq"}, runs: $testRuns, list size: $listSize, take cycles: $takeCycles]"

    fun withTestRuns(testCycles: Int) = Benchmark(testCycles, listSize, takeCycles, isPar)
    fun withListSize(listSize: Int) = Benchmark(testRuns, listSize, takeCycles, isPar)
    fun withTakeCycles(takeCycles: Float) = Benchmark(testRuns, listSize, takeCycles, isPar)
    fun withParallel(parallel: Boolean) = Benchmark(testRuns, listSize, takeCycles, parallel)
}
