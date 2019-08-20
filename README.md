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
Also when reimplementing `shuffle` in own code one can reuse intermediate collection.
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
            // we don't need to actually set `section[i]` elements since it will not be used  
        }
    }
}.take(n)
```     
This implementations is called `ViaSequence`.

### Get rid of sequence overhead

Unfortunately `sequence` generation involves some overhead which is (as shown below) much bigger than the economy from optimizations, at least for single-threaded use.
So our next step is to get rid of `sequence` mechanisms rewriting the algorithm in an old-fashioned style, like this:
```kotlin
var left = n
while (left > 0) {
    section.reset()
    val sectionSize = min(left, list.size)
    left -= list.size

    for (i in 0 until sectionSize) {
        val j = random.nextInt(list.size - i) + i
        result.add(section[j])
        section[j] = section[i]
    }
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
Don't actually use the results, but it seems optimizer doesn't punish us for that.

CPU used in actual benchmarking is i7-3770 @ 3.40 GHz, OS is Win 10.

## Findings

1. In single-threaded mode `ViaSequence` is always an outsider with typical factor of `1.5` against competitors (sometimes more than `3`).
Even for its best case with `take cycles < 1` (where optimization actually works) it is slightly worse than `StraightforwardShuffledCopies`.
And unsurprisingly the wort case is for integer `take cycles`.
Blame `sequence` generation overhead.

2. In the same conditions `OptimizedShuffledCopies` is noticeably better than `StraightforwardShuffledCopies` also with typical factor aroun `1.5`.
In one case numbers are worse for optimized version (with `take cycles = 1`) but it may be just a deviation since all results are small there and the difference is not big.
Totally the optimization seems to be a worth investment (if one is actually needed in your profile).

## Raw benchmark log  

```
Benchmark[mode: seq, runs: 4096, list size: 256, take cycles: 3.5]
35430 nanos -- StraightforwardShuffledCopies
45446 nanos -- ViaSequence
15465 nanos -- OptimizedShuffledCopies
================================================================

Benchmark[mode: seq, runs: 4096, list size: 64, take cycles: 3.5]
5937 nanos -- StraightforwardShuffledCopies
7817 nanos -- ViaSequence
3676 nanos -- OptimizedShuffledCopies
================================================================

Benchmark[mode: seq, runs: 4096, list size: 1024, take cycles: 3.5]
81496 nanos -- StraightforwardShuffledCopies
122786 nanos -- ViaSequence
56086 nanos -- OptimizedShuffledCopies
================================================================

Benchmark[mode: seq, runs: 4096, list size: 256, take cycles: 0.5]
4285 nanos -- StraightforwardShuffledCopies
4457 nanos -- ViaSequence
2501 nanos -- OptimizedShuffledCopies
================================================================

Benchmark[mode: seq, runs: 4096, list size: 256, take cycles: 9.5]
51006 nanos -- StraightforwardShuffledCopies
106821 nanos -- ViaSequence
34367 nanos -- OptimizedShuffledCopies
================================================================

Benchmark[mode: seq, runs: 4096, list size: 256, take cycles: 1.0]
3573 nanos -- StraightforwardShuffledCopies
8893 nanos -- ViaSequence
4004 nanos -- OptimizedShuffledCopies
================================================================

Benchmark[mode: seq, runs: 4096, list size: 256, take cycles: 5.0]
20480 nanos -- StraightforwardShuffledCopies
64042 nanos -- ViaSequence
17930 nanos -- OptimizedShuffledCopies
================================================================

Benchmark[mode: par, runs: 4096, list size: 256, take cycles: 3.5]
29946 nanos -- StraightforwardShuffledCopies
33850 nanos -- ViaSequence
13766 nanos -- OptimizedShuffledCopies
================================================================

Benchmark[mode: par, runs: 4096, list size: 64, take cycles: 3.5]
5540 nanos -- StraightforwardShuffledCopies
8477 nanos -- ViaSequence
3748 nanos -- OptimizedShuffledCopies
================================================================

Benchmark[mode: par, runs: 4096, list size: 1024, take cycles: 3.5]
83086 nanos -- StraightforwardShuffledCopies
144739 nanos -- ViaSequence
56382 nanos -- OptimizedShuffledCopies
================================================================

Benchmark[mode: par, runs: 4096, list size: 256, take cycles: 0.5]
7567 nanos -- StraightforwardShuffledCopies
6241 nanos -- ViaSequence
2372 nanos -- OptimizedShuffledCopies
================================================================

Benchmark[mode: par, runs: 4096, list size: 256, take cycles: 9.5]
56033 nanos -- StraightforwardShuffledCopies
86907 nanos -- ViaSequence
34225 nanos -- OptimizedShuffledCopies
================================================================

Benchmark[mode: par, runs: 4096, list size: 256, take cycles: 1.0]
4519 nanos -- StraightforwardShuffledCopies
8801 nanos -- ViaSequence
3659 nanos -- OptimizedShuffledCopies
================================================================

Benchmark[mode: par, runs: 4096, list size: 256, take cycles: 5.0]
19743 nanos -- StraightforwardShuffledCopies
49570 nanos -- ViaSequence
17962 nanos -- OptimizedShuffledCopies
================================================================

```
