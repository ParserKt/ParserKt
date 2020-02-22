package org.parserkt.util

import kotlin.reflect.KProperty

// File: util/ArrangeModel
interface Sized { val size: Cnt }
val Sized.lastIndex: Idx get() = size.dec()
val Sized.indices: IdxRange get() = 0..lastIndex
val Sized.isEmpty get() = size == 0
val Sized.isNotEmpty get() = !isEmpty

//// == Abstract ==
// Sized { size } (lastIndex, indices, isEmpty)
// Slice: Sized { get }
//   Instance: Array<E>, List<E>, CharSequence
// Tuple2, Tuple3
// Tuple (tupleOf, emptyTuple)
//   TypedTuple (IntTuple..., tupleOf(::IntTuple, 1, 2) )
//   DynamicTuple (AnyTuple, anyTupleOf, getAs<T>, indexAs<T>)

interface Slice<out E>: Sized {
  operator fun get(index: Idx): E
  companion object Instance {
    operator fun <E> invoke(array: Array<E>): Slice<E> = object: AnyBy(array), Slice<E> {
      override val size get() = array.size
      override fun get(index: Idx) = array[index]
    }
    operator fun <E> invoke(list: List<E>): Slice<E> = object: AnyBy(list), Slice<E> {
      override val size get() = list.size
      override fun get(index: Idx) = list[index]
    }
    operator fun invoke(str: CharSequence): Slice<Char> = object: AnyBy(str), Slice<Char> {
      override val size get() = str.length
      override fun get(index: Idx) = str[index]
    }
  }
}

data class Tuple2<A, B>(var first: A, var second: B)
data class Tuple3<A, B, C>(var first: A, var second: B, var third: C)

/** Data storage base on array [items], [get]/[set] and destruct, delegate by [index] */
abstract class Tuple<E>(override val size: Cnt): Slice<E> {
  protected abstract val items: Array<E>
  fun toArray() = items

  override fun get(index: Idx) = items[index]
  operator fun set(index: Idx, value: E) { items[index] = value }

  protected fun <E> Tuple<E>.index(idx: Idx): Index<E> = Index(idx)
  protected class Index<T>(private val idx: Idx) {
    operator fun getValue(self: Tuple<out T>, _p: KProperty<*>): T = self[idx]
    operator fun setValue(self: Tuple<in T>, _p: KProperty<*>, value: T) { self[idx] = value }
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    return if (other !is Tuple<*>) false
    else items.contentEquals(other.items)
  }
  override fun hashCode() = items.contentHashCode()
  override fun toString() = items.asIterable().preety().joinText(", ").surroundText(parens).toString()
}

operator fun <E> Tuple<E>.component1() = this[0]
operator fun <E> Tuple<E>.component2() = this[1]
operator fun <E> Tuple<E>.component3() = this[2]
operator fun <E> Tuple<E>.component4() = this[3]

fun <E> Tuple<E>.toList() = toArray().toList()
fun <E> Tuple<E>.asIterable() = toArray().asIterable()

//// == Abstract ==
inline fun <reified E> tupleOf(vararg items: E) = object: Tuple<E>(items.size) {
  override val items: Array<E> = arrayOf(*items)
}
inline fun <reified E> emptyTuple() = object: Tuple<E>(0) {
  override val items: Array<E> = emptyArray()
}

//// == Typed Tuples ==
open class IntTuple(size: Cnt): Tuple<Int>(size) { override val items = Array(size){0} }
open class LongTuple(size: Cnt): Tuple<Long>(size) { override val items = Array(size){0L} }
open class FloatTuple(size: Cnt): Tuple<Float>(size) { override val items = Array(size){0.0F} }
open class DoubleTuple(size: Cnt): Tuple<Double>(size) { override val items = Array(size){0.0} }

open class CharTuple(size: Cnt): Tuple<Char>(size) { override val items = Array(size){'\u0000'} }
open class StringTuple(size: Cnt): Tuple<String>(size) { override val items = Array(size){""} }

fun <T, TUPLE: Tuple<T>> tupleOf(type: (Cnt) -> TUPLE, vararg items: T): TUPLE {
  val tuple = type(items.size)
  for ((i, x) in items.withIndex()) tuple[i] = x
  return tuple
}
fun <T, TUPLE: Tuple<T>> tupleOf(type: Producer<TUPLE>, vararg items: T) = tupleOf({ _ -> type() }, *items)

/// == Dynamic Tuples ==
open class AnyTuple(size: Cnt): Tuple<Any>(size) {
  @Suppress("UNCHECKED_CAST")
  override val items = arrayOfNulls<Any>(size) as Array<Any>
}

fun anyTupleOf(vararg items: Any) = object: AnyTuple(items.size) {
  override val items = arrayOf(*items)
}

inline fun <reified T> Tuple<*>.getAs(idx: Idx) = this[idx] as T

fun <T> Tuple<*>.indexAs(idx: Idx): IndexAs<T> = IndexAs(idx)
class IndexAs<T>(private val idx: Idx) {
  @Suppress("UNCHECKED_CAST")
  operator fun getValue(self: Tuple<*>, _p: KProperty<*>): T = self[idx] as T
  operator fun setValue(self: Tuple<in T>, _p: KProperty<*>, value: T) { self[idx] = value }
}
