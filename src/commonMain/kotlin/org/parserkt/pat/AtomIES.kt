package org.parserkt.pat

import org.parserkt.*
import org.parserkt.util.*

// File: pat/AtomIES
abstract class SatisfyPattern<IN>: PreetyPattern<IN, IN>(), MonoPattern<IN> {
  abstract fun test(value: IN): Boolean
  override fun read(s: Feed<IN>) = s.consumeIf(::test)
  override fun show(s: Output<IN>, value: IN?) { value?.let(s) }
  /** Apply logical relation like (&&) (||) with 2 satisfy patterns */
  class LogicalConcat<IN>(val self: SatisfyPattern<IN>, val other: SatisfyPattern<IN>,
      val name: String, private val join: InfixJoin<Boolean>): SatisfyPattern<IN>() {
    override fun test(value: IN) = join(self.test(value), other.test(value))
    override fun toPreetyDoc() = listOf(self, other).preety().joinText(name)
  }
  class Negate<IN>(val self: SatisfyPattern<IN>): SatisfyPattern<IN>() {
    override fun test(value: IN) = !self.test(value)
    override fun toPreetyDoc(): PP = self.preety().let { "!".preety() + if (self is LogicalConcat<*>) it.surroundText(parens) else it }
  }
  infix fun and(next: SatisfyPattern<IN>) = LogicalConcat(this, next, "&", Boolean::and)
  infix fun or(next: SatisfyPattern<IN>) = LogicalConcat(this, next, "|", Boolean::or)
  operator fun not(): SatisfyPattern<IN> = Negate(this)
}
class SatisfyEqualTo<IN>(override val constant: IN): SatisfyPattern<IN>(), MonoConstantPattern<IN> {
  override fun test(value: IN) = value == constant
  override fun toPreetyDoc() = constant.rawPreety()
}

/* item(); item(value) */
fun <IN> item() = object: SatisfyPattern<IN>() {
  override fun test(value: IN) = true
  override fun toPreetyDoc() = "anyItem".preety()
}
fun <IN> item(value: IN) = SatisfyEqualTo(value)

/* elementIn(a, b, c); elementIn(1..100); elementIn('a'..'z', 'A'..'Z') */
fun <IN> elementIn(vararg values: IN) = object: SatisfyPattern<IN>() {
  override fun test(value: IN) = value in values
  override fun toPreetyDoc() = values.map { it.rawPreety() }.joinText("|").surroundText(parens)
}
fun <IN: Comparable<IN>> elementIn(range: ClosedRange<IN>) = object: SatisfyPattern<IN>() {
  override fun test(value: IN) = value in range
  override fun toPreetyDoc() = range.preety().surroundText(parens)
}
fun elementIn(vararg ranges: CharRange) = object: SatisfyPattern<Char>() {
  override fun test(value: Char) = ranges.any { range -> value in range }
  override fun toPreetyDoc() = ranges.map(::toDashPreety).join(Preety.Doc.None).surroundText(squares)
  private fun toDashPreety(r: CharRange) = listOf(r.first, r.last).preety().joinText("-")
}

/* satisfy<Int>("even") { it % 2 == 0 } */
fun <IN> satisfy(name: String = "?", predicate: Predicate<IN>) = object: SatisfyPattern<IN>() {
  override fun test(value: IN) = predicate(value)
  override fun toPreetyDoc() = name.preety().surroundText(parens)
}

class StickyEnd<IN, T>(override val item: MonoPattern<IN>, val value: T?, val onFail: ProducerOn<Feed<IN>, T?> = {notParsed}): MonoPatternWrapper<IN, T>(item) {
  override fun read(s: Feed<IN>) = if (item.testPeek(s) && s.isStickyEnd()) value else s.onFail()
  override fun show(s: Output<IN>, value: T?) {}
  override fun wrap(item: Pattern<IN, IN>) = StickyEnd(item, value, onFail)
  override fun toPreetyDoc() = listOf("stickyEnd", item, value).preety().colonParens()
}

//// == anyChar & named ==
val anyChar = item<Char>() named "anyChar"
val EOF = item('\uFFFF') named "EOF"

infix fun <IN> SatisfyPattern<IN>.named(name: PP) = object: SatisfyPatternBy<IN>(this) { override fun toPreetyDoc() = name }
infix fun <IN> SatisfyPattern<IN>.named(name: String) = named(name.preety())

infix fun <IN, T> Pattern<IN, T>.named(name: PP) = object: Pattern<IN, T> by this {
  override fun toPreetyDoc() = name
  override fun toString() = toPreetyDoc().toString()
}
infix fun <IN, T> Pattern<IN, T>.named(name: String) = named(name.preety())
