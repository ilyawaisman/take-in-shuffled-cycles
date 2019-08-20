# take-in-shuffled-cycles Kotlin implementations benchmark

Benchmark for implementations of behavior "take-in-shuffled-cycles".

## The task
 
 Given a `list: List<T>` and a number `n: Int`. Algorithm needs to uniformly randomly produce one of sequences bound with following conditions:
* every element of the sequence is some element from the `list`,
* if you break the sequence into sections of length `list.size` (last section can be smaller) than every section will have no repetitions (under the condition that initial `list` has no repetitions).

## Algorithms

### Shuffled copies

First idea is to concatenate enough shuffled copies of list and take enough elements.
Straightforward implementation
```kotlin
(0 until sectionsNumber).flatMap {
    list.shuffled()
}.take(n)
```

gets the name `StraightforwardShuffledCopies`.

### Shuffle only while you need (into sequence)

Idea of optimization is that for the last section one can stop shuffle algorithm when it produced enough elements. This optimization make turn out to be substantial if `n << list.size`.
Also when reimplementing `shuffle` in own code one can reuse intermediate collection (which turned out to be a bad idea).
The flow of this approach reminds `sequence` generation.

Omitting some details we got something like this:
```kotlin
sequence {
    while (true) {
        section.reset() // whatever it means  
        list.indices.forEach { i ->
            val j = random.nextInt(list.size - i) + i
            yield(section[j])
            section[j] = section[i]
            // we don't need to actually set `section[i]` element since it will not be used  
        }
    }
}.take(n)
```     
This implementations is called `ViaSequence`.

### Get rid of sequence overhead

Unfortunately `sequence` generation involves some overhead which is (as shown below) much bigger than the economy from optimizations, at least for single-threaded use.
So our next step is to get rid of `sequence` mechanisms rewriting the algorithm in an old-fashioned style, like this:
```kotlin
val fullSectionsNum = n / list.size
repeat(fullSectionsNum) {
    result.addAll(list.shuffled(random))
}

val incompleteSectionSize = n - fullSectionsNum * list.size
val section = list.toMutableList()
for (i in 0 until incompleteSectionSize) {
    val j = random.nextInt(list.size - i) + i
    result.add(section[j])
    section[j] = section[i]
}
```  
This implementation is called `OptimizedShuffledCopies`.

## Benchmarking

Benchmark aspects.
1. Runs numbers. Empirically found number of `4096` seems to be good enough for all purposes.
2. `list` size. We use `256` as a regular value but also try `64` and `1024`.
3. Elements to take. We use this parameter in form of factor upon `list size` - `take cycles`. 
Start with `3.5` but also try `0.5` (to cover the case where not all elements are used), `9.5` (as a bigger number), `1` and `5` (to cover case when result consists of complete cycles).
4. Single/multi threaded.
Below used terms `seq` for _sequential_ i.e. single-threaded and `par` for _parallel_ i.e. using all cores available (via coroutines).

Technical details.
Start each benchmark with enough (`0x10000`) JIT-warmup runs.
The results of the actual runs are not used, but optimizer doesn't punish us for that.

CPU used in actual benchmarking is i7-3770 @ 3.40 GHz (4 actual cores), OS is Win 10.

## Findings

1. In single-threaded mode `ViaSequence` is always an outsider with typical factor of `1.5` against competitors (sometimes more than `3`).
Even for its best case with `take cycles < 1` (where optimization actually works) it is slightly worse than `StraightforwardShuffledCopies`.
And unsurprisingly the wort case is for integer `take cycles`.
Blame `sequence` generation overhead.

2. In the same conditions `OptimizedShuffledCopies` is noticeably better than `StraightforwardShuffledCopies` also with typical factor around `1.5`.
Totally the optimization seems to be a worth investment (if one is actually needed in your profile).

3. For `OptimizedShuffledCopies` it turned out that for complete segments usage of `addAll(list.shuffled())` is a little cheaper than reuse of intermediate collection and resetting it.
I think that is because in the former case `System.arraycopy` is used.

4. Everyone benefits from parallelism, but `StraightforwardShuffledCopies` does worse than others, so in a couple of tests is loses to `ViaSequence`.
I don't have an explanation for that.
What is even more interesting, if `Iterable<T>.shuffled(Random)` is replaced with `Iterable<T>.shuffled()` this implementation becomes _slower_ in multi-threaded mode than in single-thread (which remains the same).
The reason for it is that in latter case `java.util.Random` is used ultimately.
And although it is thread-safe it may become quite inefficient if intensively used from several threads simultaneously.
Shuffle scenario is one of the best examples of such situation.

**Long story short** optimized version is always faster than straightforward one typically around `1.5` times.
But one need to write it in old-fashioned style since `sequence` generation overhead eats all of the advantage (and even more).

## Raw benchmark log  

```
Benchmark[mode: seq, runs: 4096, list size: 256, take cycles: 3.5]
21188 nanos -- StraightforwardShuffledCopies
29714 nanos -- ViaSequence
8809 nanos -- OptimizedShuffledCopies
================================================================

Benchmark[mode: seq, runs: 4096, list size: 64, take cycles: 3.5]
4346 nanos -- StraightforwardShuffledCopies
7230 nanos -- ViaSequence
2111 nanos -- OptimizedShuffledCopies
================================================================

Benchmark[mode: seq, runs: 4096, list size: 1024, take cycles: 3.5]
62011 nanos -- StraightforwardShuffledCopies
124952 nanos -- ViaSequence
35701 nanos -- OptimizedShuffledCopies
================================================================

Benchmark[mode: seq, runs: 4096, list size: 256, take cycles: 0.5]
3180 nanos -- StraightforwardShuffledCopies
4164 nanos -- ViaSequence
1837 nanos -- OptimizedShuffledCopies
================================================================

Benchmark[mode: seq, runs: 4096, list size: 256, take cycles: 9.5]
38166 nanos -- StraightforwardShuffledCopies
82904 nanos -- ViaSequence
23844 nanos -- OptimizedShuffledCopies
================================================================

Benchmark[mode: seq, runs: 4096, list size: 256, take cycles: 1.0]
2414 nanos -- StraightforwardShuffledCopies
8202 nanos -- ViaSequence
2372 nanos -- OptimizedShuffledCopies
================================================================

Benchmark[mode: seq, runs: 4096, list size: 256, take cycles: 5.0]
13789 nanos -- StraightforwardShuffledCopies
43085 nanos -- ViaSequence
12100 nanos -- OptimizedShuffledCopies
================================================================

Benchmark[mode: par, runs: 4096, list size: 256, take cycles: 3.5]
7601 nanos -- StraightforwardShuffledCopies
8763 nanos -- ViaSequence
3397 nanos -- OptimizedShuffledCopies
================================================================

Benchmark[mode: par, runs: 4096, list size: 64, take cycles: 3.5]
1892 nanos -- StraightforwardShuffledCopies
2686 nanos -- ViaSequence
803 nanos -- OptimizedShuffledCopies
================================================================

Benchmark[mode: par, runs: 4096, list size: 1024, take cycles: 3.5]
19724 nanos -- StraightforwardShuffledCopies
30796 nanos -- ViaSequence
10372 nanos -- OptimizedShuffledCopies
================================================================

Benchmark[mode: par, runs: 4096, list size: 256, take cycles: 0.5]
1186 nanos -- StraightforwardShuffledCopies
1772 nanos -- ViaSequence
657 nanos -- OptimizedShuffledCopies
================================================================

Benchmark[mode: par, runs: 4096, list size: 256, take cycles: 9.5]
10932 nanos -- StraightforwardShuffledCopies
23662 nanos -- ViaSequence
5656 nanos -- OptimizedShuffledCopies
================================================================

Benchmark[mode: par, runs: 4096, list size: 256, take cycles: 1.0]
796 nanos -- StraightforwardShuffledCopies
2437 nanos -- ViaSequence
1042 nanos -- OptimizedShuffledCopies
================================================================

Benchmark[mode: par, runs: 4096, list size: 256, take cycles: 5.0]
4218 nanos -- StraightforwardShuffledCopies
10963 nanos -- ViaSequence
3865 nanos -- OptimizedShuffledCopies
================================================================

```
