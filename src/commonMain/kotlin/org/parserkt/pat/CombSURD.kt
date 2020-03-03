package org.parserkt.pat

import org.parserkt.*
import org.parserkt.util.*

// File: pat/CombSURD
// SingleFeed (for Until) and FoldPattern (Until, Repeat)

/** Peek feed used in [Until], [StickyEnd], etc. */
class SingleFeed<T>(val value: T): Feed<T> {
  private var valueConsumed = false
  override val peek = value

  override fun consume()
    = if (!valueConsumed)
      { valueConsumed = true; value }
    else throw Feed.End()
  override fun toPreetyDoc() = "SingleFeed".preety() + value.preety().surroundText(parens) +
    (if (valueConsumed) ".".preety() else Preety.Doc.None)
}


/** Pattern of "folded" items, like [Until], [Repeat]. exceptions terminates read with [notParsed] */
abstract class FoldPattern<IN, T, R>(val fold: Fold<T, R>, override val item: Pattern<IN, T>): PreetyPattern<IN, R>(), PatternWrapperKind<IN, T> {
  protected open fun unfold(value: R): Iterable<T> = defaultUnfold(value)
  override fun show(s: Output<IN>, value: R?) {
    if (value == null) return
    unfold(value).forEach { item.show(s, it) }
  }
}
internal fun <R, T> defaultUnfold(value: R): Iterable<T> = @Suppress("unchecked_cast") when (value) {
  is Iterable<*> -> value as Iterable<T>
  is String -> value.asIterable() as Iterable<T>
  else -> unsupported("unfold")
}

// "SURD"
// Seq(type: TUPLE, vararg items)
// Until(terminate, fold, item),
// Repeat(fold, item) { greedy, bound; InBounds, Many }
// Decide(vararg cases)

class Seq<IN, T, TUPLE: Tuple<T>>(val type: (Cnt) -> TUPLE, vararg val items: Pattern<IN, out T>): PreetyPattern<IN, TUPLE>() {
  constructor(type: Producer<TUPLE>, vararg items: Pattern<IN, out T>): this({ _ -> type() }, *items)
  init { checkedTuple() }
  override fun read(s: Feed<IN>): TUPLE? {
    val tuple = checkedTuple()
    for ((i, x) in items.withIndex()) tuple[i] = x.read(s) ?: return notParsed
    return tuple
  }
  override fun show(s: Output<IN>, value: TUPLE?) {
    if (value == null) return
    require(value.size == items.size) {"bad tuple-show size (${value.size}, not ${items.size})"}
    for ((i, v) in value.toArray().withIndex())
      @Suppress("unchecked_cast") (items[i] as Pattern<IN, in @UnsafeVariance T>).show(s, v)
  }
  private fun checkedTuple(): TUPLE {
    val tuple = type(items.size)
    require(tuple.size >= items.size) {"tuple size too small (${tuple.size} for ${items.size})"} // lesser strict
    return tuple
  }
  override fun toPreetyDoc() = items.asIterable().preety().joinText(" ").surroundText(parens)
}

open class Until<IN, T, R>(val terminate: Pattern<IN, *>, fold: Fold<T, R>, item: Pattern<IN, T>): FoldPattern<IN, T, R>(fold, item) {
  override fun read(s: Feed<IN>): R? {
    val reducer = fold.reducer()
    while (!terminate.testPeek(s)) {
      val parsed = item.read(s) ?: return notParsed
      s.catchError { reducer.accept(parsed) } ?: return notParsed
    }
    return reducer.finish()
  }
  override fun toPreetyDoc() = listOf(item, terminate).preety().joinText("~")
}
internal fun <IN> Feed<IN>.singleFeed() = FilterInput(this, SingleFeed(peek))
internal fun <IN, T> Pattern<IN, T>.testPeek(s: Feed<IN>) = read(s.singleFeed()) != notParsed


open class Repeat<IN, T, R>(fold: Fold<T, R>, item: Pattern<IN, T>): FoldPattern<IN, T, R>(fold, item) {
  override fun read(s: Feed<IN>): R? {
    val reducer = fold.reducer()
    var count = 0
    while (true) {
      val parsed = item.read(s) ?: break
      s.catchError { reducer.accept(parsed) } ?: return notParsed
      ++count; if (!greedy && count >= bounds.last) break
    }
    return if (count in bounds) reducer.finish() else notParsed
  }
  override fun show(s: Output<IN>, value: R?) {
    if (value == null) return
    var count = 0
    unfold(value).forEach { item.show(s, it); ++count }
    check(count in bounds) {"bad wrote count $count !in $bounds"}
  }
  override fun toPreetyDoc() = item.preety().surroundText(braces)

  // "repeat many" (0..MAX) - Repeat(pat).Many(); Repeat(pat).InBounds(0..n, greedy = true)
  protected open val greedy = true
  protected open val bounds = 1..Cnt.MAX_VALUE

  open inner class InBounds(override val bounds: IntRange, override val greedy: Boolean = true): Repeat<IN, T, R>(fold, item) {
    override fun toPreetyDoc() = listOf( super.toPreetyDoc(),
      (bounds as Any).preety().surroundText(parens) ).join(if (greedy) "g".preety() else Preety.Doc.None)
  }
  inner class Many: InBounds(0..Cnt.MAX_VALUE), OptionalPatternKind<R> {
    override val defaultValue = fold.reducer().finish()
    override fun toPreetyDoc() = item.preety().surroundText(braces) + "?"
  }
}

class Decide<IN, T>(vararg val cases: Pattern<IN, out T>): PreetyPattern<IN, Tuple2<Idx, T>>() {
  override fun read(s: Feed<IN>): Tuple2<Idx, T>? {
    for ((i, case) in cases.withIndex()) case.read(s)?.let { return Tuple2(i, it) }
    return notParsed
  }
  override fun show(s: Output<IN>, value: Tuple2<Idx, T>?) {
    if (value == null) return
    val (i, state) = value
    @Suppress("unchecked_cast") (cases[i] as Pattern<IN, in @UnsafeVariance T>).show(s, state)
  }
  override fun toPreetyDoc() = cases.asIterable().preety().joinText("|").surroundText(parens)
}

// "rebuild" — UntilUn(+unfold), RepeatUn(+unfold)

class UntilUn<IN, T, R>(terminate: ConstantPattern<IN, T>, fold: Fold<T, R>, item: Pattern<IN, T>, private val unfold: (R) -> Iterable<T>): Until<IN, T, R>(terminate, fold, item) {
  override fun unfold(value: R) = unfold.invoke(value)
}
class RepeatUn<IN, T, R>(fold: Fold<T, R>, item: Pattern<IN, T>, private val unfold: (R) -> Iterable<T>): Repeat<IN, T, R>(fold, item) {
  override fun unfold(value: R) = unfold.invoke(value)
}
