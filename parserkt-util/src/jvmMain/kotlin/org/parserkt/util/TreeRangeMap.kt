package org.parserkt.util

import java.util.*

/** A [RangeMap] implemented by sorted map
 * + NOTE: RangeMap is __not ready__ to handle overlapped ranges */
open class TreeRangeMap<N, T>: MutableRangeMap<N, T> where N: Comparable<N> {
  protected val tree: TreeSet<Ray<N, T>> = TreeSet()

  override fun get(index: N): T? {
    val node = searchMaxLE(index) ?: return null
    return if (node is Ray.Shadow<N, T>) node.value else null
  }
  override fun set(indices: ExclusiveRange<N>, value: T) {
    tree.remove(edgeAt(indices.start)) // remove overlapping end edge
    val newStart = Ray.Shadow(indices.start, value)
    val stop = Ray.Blank(indices.stop)
    tree.addAll(listOf(newStart, stop))
  }

  /** The greatest element in this set ≤ [target] */
  protected fun searchMaxLE(target: N): Ray<N, T>? = tree.floor(edgeAt(target))
  protected fun edgeAt(start: N) = Ray.Blank(start)

  /** A kind of storage at specialized position in lines, covering all points until next [Ray] */
  protected sealed class Ray<N, out T>(val start: N): Comparable<Ray<N, *>> where N: Comparable<N> {
    class Shadow<N, T>(start: N, val value: T): Ray<N, T>(start) where N: Comparable<N>
    class Blank<N>(start: N): Ray<N, Nothing>(start) where N: Comparable<N>

    override fun compareTo(other: Ray<N, *>) = start.compareTo(other.start)
    override fun equals(other: Any?): Boolean {
      if (this === other) return true
      return if (other !is Ray<*, *>) false
      else start == other.start
    }
    override fun hashCode(): Int = Ray::class.hashCode()
  }
}

fun <N, T> mutableRangeMapOf(vararg pairs: Pair<ExclusiveRange<N>, T>): MutableRangeMap<N, T> where N: Comparable<N>
  = TreeRangeMap<N, T>().also { for ((k, v) in pairs) it[k] = v }

fun <N, T> rangeMapOf(vararg pairs: Pair<ExclusiveRange<N>, T>): RangeMap<N, T> where N: Comparable<N>
  = mutableRangeMapOf(*pairs)
