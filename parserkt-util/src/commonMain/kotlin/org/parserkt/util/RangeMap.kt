package org.parserkt.util

interface ExclusiveRange<T> where T: Comparable<T> {
  val start: T; val stop: T
  operator fun contains(value: T) = value >= start && value < stop
  val isEmpty get() = start >= stop
}

abstract class BaseExclusiveRange<T>(override val start: T, override val stop: T): ExclusiveRange<T> where T: Comparable<T> {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    return other is ExclusiveRange<*> && start == other.start && stop == other.stop
    // See also https://youtrack.jetbrains.com/issue/KT-37128
  }
  override fun hashCode() = hash(start, stop)
  override fun toString() = "($start, $stop]"
}

class IntExclusiveRange(start: Int, stop: Int): BaseExclusiveRange<Int>(start, stop)
class LongExclusiveRange(start: Long, stop: Long): BaseExclusiveRange<Long>(start, stop)

class FloatExclusiveRange(start: Float, stop: Float): BaseExclusiveRange<Float>(start, stop)
class DoubleExclusiveRange(start: Double, stop: Double): BaseExclusiveRange<Double>(start, stop)

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
