package org.parserkt.util

// File: util/FoldModel
interface Reducer<in T, out R> {
  fun accept(value: T)
  fun finish(): R
}
interface Fold<in T, out R> {
  fun reducer(): Reducer<T, R>
}

/** Fold of [makeBase] and [onAccept] */
abstract class EffectFold<T, R: R0, R0>: Fold<T, R0> {
  protected abstract fun makeBase(): R
  protected abstract fun onAccept(base: R, value: T)
  override fun reducer() = object: Reducer<T, R0> {
    val base = makeBase()
    override fun accept(value: T) { onAccept(base, value) }
    override fun finish() = base
  }
}
/** Fold of [initial], [join], [convert] */
abstract class ConvertFold<T, A, R>: Fold<T, R> {
  protected abstract val initial: A
  protected abstract fun join(base: A, value: T): A
  protected abstract fun convert(base: A): R
  override fun reducer() = object: Reducer<T, R> {
    var base = initial
    override fun accept(value: T) { base = join(base, value) }
    override fun finish() = convert(base)
  }
}

/** Shorthand for [ConvertFold], use like `JoinFold(initial = 0,  append = Int::plus)` */
open class ConvertJoinFold<T, R>(override val initial: R, private val append: R.(T) -> R): ConvertFold<T, R, R>() {
  override fun join(base: R, value: T) = base.append(value)
  override fun convert(base: R) = base
}
class JoinFold<T>(initial: T, append: T.(T) -> T): ConvertJoinFold<T, T>(initial, append)

typealias InfixJoin<T> = (T, T) -> T

//// == Abstract ==
// EffectFold { makeBase, onAccept }
// ConvertFold { initial, join, convert }
// JoinFold(initial, append)

fun <T> asList() = object: EffectFold<T, MutableList<T>, List<T>>() {
  override fun makeBase(): MutableList<T> = mutableListOf()
  override fun onAccept(base: MutableList<T>, value: T) { base.add(value) }
}

abstract class  AsStringBuild<T>: ConvertFold<T, StringBuilder, String>() {
  override val initial get() = StringBuilder()
  override fun convert(base: StringBuilder) = base.toString()
}
fun asString() = object: AsStringBuild<Char>() {
  override fun join(base: StringBuilder, value: Char) = base.append(value)
}
fun joinAsString() = object: AsStringBuild<String>() {
  override fun join(base: StringBuilder, value: String) = base.append(value)
}

fun <T, R> Iterable<T>.fold(fold: Fold<T, R>): R {
  val reducer = fold.reducer()
  forEach(reducer::accept)
  return reducer.finish()
}
