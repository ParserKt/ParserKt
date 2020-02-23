package org.parserkt.util

interface ExclusiveRange<T> where T: Comparable<T> {
  val start: T; val stop: T
  operator fun contains(value: T) = value >= start && value < stop
  val isEmpty get() = start >= stop
}

class IntExclusiveRange(override val start: Int, override val stop: Int): ExclusiveRange<Int>
class LongExclusiveRange(override val start: Long, override val stop: Long): ExclusiveRange<Long>

class FloatExclusiveRange(override val start: Float, override val stop: Float): ExclusiveRange<Float>
class DoubleExclusiveRange(override val start: Double, override val stop: Double): ExclusiveRange<Double>

infix fun Int.stop(other: Int) = IntExclusiveRange(this, other)
infix fun Long.stop(other: Long) = LongExclusiveRange(this, other)
infix fun Float.stop(other: Float) = FloatExclusiveRange(this, other)
infix fun Double.stop(other: Double) = DoubleExclusiveRange(this, other)

//// == Range Mapping ==
interface RangeMap<N, out T> where N: Comparable<N> {
  operator fun get(index: N): T?
}
interface MutableRangeMap<N, T>: RangeMap<N, T> where N: Comparable<N> {
  operator fun set(indices: ExclusiveRange<N>, value: T)
}
