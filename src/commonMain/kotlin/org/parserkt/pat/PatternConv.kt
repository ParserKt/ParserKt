package org.parserkt.pat

import org.parserkt.*
import org.parserkt.util.*

// File: pat/PatternConv
fun <T> MonoPair<T>.toPat(): MonoPair<SatisfyEqualTo<T>> = map(::item)
fun MonoPair<String>.toCharPat(): MonoPair<SatisfyEqualTo<Char>> = map(String::single).toPat()

fun MonoPattern<Char>.toStringPat() = Convert(this, Char::toString, String::first)
fun <IN> Seq<IN, Char, CharTuple>.toStringPat() = Convert(this, { it.toArray().joinToString("") }, { tupleOf(::CharTuple, *it.toList().toArray()) })

/* val str = Seq(::StringTuple, item('"').toStringPat(), *anyChar until item('"')) */
infix fun MonoPattern<Char>.until(terminate: MonoPattern<Char>)
  = arrayOf<Pattern<Char, String>>(Until(terminate, asString(), this), terminate.toStringPat())

// Tuple patterns: flatten()
//   + merge first/second (op)
//   + discard first/second ()

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
