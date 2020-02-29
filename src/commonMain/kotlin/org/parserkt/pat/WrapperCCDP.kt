package org.parserkt.pat

import org.parserkt.*
import org.parserkt.util.*

// File: pat/WrapperCCDP
// "CCDP"
// Convert(item, transform: ConvertAs<T1, T>) constructor(item, to={unsupported})
//   + ConvertAs.Box<T>(v: T)
//   + ext Pattern { typed((T)->BOX), force() }
// Contextual(head, body)
// Deferred(item: Producer<Pattern<IN, R>>)
// Piped(item, op)

// Tuple patterns: flatten()
//   + merge first/second (op)
//   + discard first/second ()

data class ConvertAs<T1, T>(val from: (T) -> T1, val to: (T1) -> T?) {
  interface Box<T> { val v: T }
}
class Convert<IN, T, T1>(item: Pattern<IN, T>, val transform: ConvertAs<T1, T>): ConvertPatternWrapper<IN, T, T1>(item) {
  constructor(item: Pattern<IN, T>, from: (T) -> T1, to: (T1) -> T?): this(item, ConvertAs(from, to))
  constructor(item: Pattern<IN, T>, from: (T) -> T1): this(item, from, { unsupported("convert back") })

  override fun read(s: Feed<IN>) = item.read(s)?.let(transform.from)
  override fun show(s: Output<IN>, value: T1?) {
    if (value == null) return
    item.show(s, value.let(transform.to))
  }
  override fun wrap(item: Pattern<IN, T>) = Convert(item, transform)
}
infix fun <IN, T, BOX: ConvertAs.Box<T>> Pattern<IN, T>.typed(type: (T) -> BOX) = Convert(this, type, ConvertAs.Box<T>::v)
fun <IN, T:T1, T1> Pattern<IN, T>.force() = @Suppress("unchecked_cast") Convert(this, { it as T1 }, { it as T })


class Contextual<IN, HEAD, BODY>(val head: Pattern<IN, HEAD>, val body: (HEAD) -> Pattern<IN, BODY>): PreetyPattern<IN, Tuple2<HEAD, BODY>>() {
  override fun read(s: Feed<IN>): Tuple2<HEAD, BODY>? {
    val context = head.read(s) ?: return notParsed
    val parsed = body(context).read(s) ?: return notParsed
    return Tuple2(context, parsed)
  }
  override fun show(s: Output<IN>, value: Tuple2<HEAD, BODY>?) {
    if (value == null) return
    val (context, parsed) = value
    head.show(s, context)
    body(context).show(s, parsed)
  }
  override fun toPreetyDoc(): PP = head.preety() + "@"
}

class Deferred<IN, T>(val lazyItem: Producer<Pattern<IN, T>>): Pattern<IN, T>, RecursionDetect() {
  override fun read(s: Feed<IN>) = lazyItem().read(s)
  override fun show(s: Output<IN>, value: T?) = lazyItem().show(s, value)
  override fun toPreetyDoc(): PP = lazyItem().toPreetyDoc()
  override fun toString() = recurse { if (isActive) "recurse" else toPreetyDoc().toString() }
}

class Piped<IN, T>(item: Pattern<IN, T>, val op: Feed<IN>.(T?) -> T? = {it}): PatternWrapper<IN, T>(item) {
  override fun read(s: Feed<IN>): T? = item.read(s).let { s.op(it) }
  override fun wrap(item: Pattern<IN, T>) = Piped(item, op)
  override fun toPreetyDoc(): PP = listOf("Piped", item).preety().colonParens()
}

// Tuple2: flatten(), mergeFirst(first: (B) -> A), mergeSecond(second: (A) -> B)
/* val i2 = Seq(::IntTuple, *Contextual(item<Int>()) { item(it) }.flatten().items()) */

fun <IN, A, B> Pattern<IN, Tuple2<A, B>>.flatten(): Pair<Pattern<IN, A>, Pattern<IN, B>> {
  val item = this; var parsed: Tuple2<A, B>? = null
  val part1: Pattern<IN, A> = object: PreetyPattern<IN, A>() {
    override fun read(s: Feed<IN>) = item.read(s).also { parsed = it }?.first
    override fun show(s: Output<IN>, value: A?) {}
    override fun toPreetyDoc() = item.toPreetyDoc()
  }
  val part2: Pattern<IN, B> = object: PreetyPattern<IN, B>() {
    override fun read(s: Feed<IN>) = parsed?.second
    override fun show(s: Output<IN>, value: B?) = item.show(s, parsed)
    override fun toPreetyDoc() = "#2".preety()
  }
  return Pair(part1, part2)
}

fun <IN, A, B> Pattern<IN, Tuple2<A, B>>.mergeFirst(first: (B) -> A) = Convert(this, { it.second }, { Tuple2(first(it), it) })
fun <IN, A, B> Pattern<IN, Tuple2<A, B>>.mergeSecond(second: (A) -> B) = Convert(this, { it.first }, { Tuple2(it, second(it)) })

fun <IN, A, B> Pattern<IN, Tuple2<A, B>>.discardFirst() = Convert(this) { it.second }
fun <IN, A, B> Pattern<IN, Tuple2<A, B>>.discardSecond() = Convert(this) { it.first }
