package org.parserkt.util

// File: util/AnyBy&RecDetect
interface Eq {
  override fun equals(other: Any?): Boolean
  override fun hashCode(): Int
}

inline fun <reified T: Any> T.compareUsing(other: Any?, crossinline compare: T.(T) -> Boolean): Boolean {
  if (this === other) return true
  return other is T && compare(other)
}
fun hash(vararg values: Any?) = values.contentHashCode()
fun hash(value: Any?) = value.hashCode()

/** Transparent delegate for [Any] */
open class AnyBy(val obj: Any): Eq {
  override fun equals(other: Any?)
    = if (other is AnyBy) obj == other.obj
    else obj == other
  override fun hashCode() = hash(obj)
  override fun toString() = obj.toString()
}

open class RecursionDetect {
  protected var recursion = 0
  protected fun <R> recurse(op: Producer<R>): R {
    ++recursion; try { return op() } finally { --recursion }
  }
  protected val isActive get() = recursion > 1
}
