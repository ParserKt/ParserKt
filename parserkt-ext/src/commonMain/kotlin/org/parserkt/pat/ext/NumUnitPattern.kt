package org.parserkt.pat.ext

import org.parserkt.*
import org.parserkt.util.*
import org.parserkt.pat.*
import org.parserkt.pat.complex.PairedTriePattern

// File: pat/ext/NumUnitPattern
interface NumOps<NUM: Comparable<NUM>> {
  val zero: NUM
  fun from(n: Number): NUM
  fun plus(b: NUM, a: NUM): NUM
  fun minus(b: NUM, a: NUM): NUM
  fun times(b: NUM, a: NUM): NUM
  fun div(b: NUM, a: NUM): NUM
  fun rem(b: NUM, a: NUM): NUM
  open class Instance<NUM: Comparable<NUM>>(
    override val zero: NUM, private val from: (Number) -> NUM,
    private val plus: InfixJoin<NUM>, private val minus: InfixJoin<NUM>,
    private val times: InfixJoin<NUM>, private val div: InfixJoin<NUM>, private val rem: InfixJoin<NUM>
  ): NumOps<NUM> {
    override fun from(n: Number) = from.invoke(n)
    override fun plus(b: NUM, a: NUM) = plus.invoke(a, b)
    override fun minus(b: NUM, a: NUM) = minus.invoke(a, b)
    override fun times(b: NUM, a: NUM) = times.invoke(a, b)
    override fun div(b: NUM, a: NUM) = div.invoke(a, b)
    override fun rem(b: NUM, a: NUM) = rem.invoke(a, b)
  }
}
object IntOps: NumOps.Instance<Int>(0, Number::toInt, Int::plus, Int::minus, Int::times, Int::div, Int::rem)
object LongOps: NumOps.Instance<Long>(0L, Number::toLong, Long::plus, Long::minus, Long::times, Long::div, Long::rem)
object FloatOps: NumOps.Instance<Float>(0.0F, Number::toFloat, Float::plus, Float::minus, Float::times, Float::div, Float::rem)
object DoubleOps: NumOps.Instance<Double>(0.0, Number::toDouble, Double::plus, Double::minus, Double::times, Double::div, Double::rem)

/*
val n=RepeatUn(asInt(), digitFor('0'..'9')) { it.toString().map { it-'0' } }
val u=KeywordPattern<Int>().apply { mergeStrings("s" to 1, "min" to 60, "hr" to 60*60) }
val k=NumUnitTrie(n, u, IntOps)
*/

typealias NumUnit<NUM, IN> = Pair<NUM, Iterable<IN>>

/** Pattern for `"2hr1min14s"`, note that reverse map won't be updated every [show] */
abstract class NumUnitPattern<IN, NUM: Comparable<NUM>>(val number: Pattern<IN, NUM>, open val unit: Pattern<IN, NUM>,
    protected val op: NumOps<NUM>): PreetyPattern<IN, NUM>() {
  protected open fun rescue(s: Feed<IN>, acc: NUM, i: NUM): NUM? = notParsed.also { s.error("expecting unit for $i (accumulated $acc)") }
  override fun read(s: Feed<IN>): NUM? {
    var accumulator: NUM = op.zero
    var lastUnit: NumUnit<NUM, IN>? = null
    var i: NUM? = number.read(s) ?: return notParsed
    while (i != notParsed) { // i=num, k=unit
      val k = unit.read(s) ?: rescue(s, accumulator, i) ?: return notParsed
      val unit = reversedPairsDsc.first { it.first == k }
      accumulator = (if (lastUnit == null) joinUnitsInitial(s, k, i)
        else joinUnits(s, lastUnit, unit, accumulator, i)) ?: return accumulator
      lastUnit = unit //->
      i = number.read(s)
    }
    return accumulator
  }
  override fun show(s: Output<IN>, value: NUM?) {
    if (value == null) return
    var rest: NUM = value
    var lastUnit: NumUnit<NUM, IN>? = null
    while (rest != op.zero) {
      val unit = maxCmpLE(rest)
      if (lastUnit != null) joinUnitsShow(s, lastUnit, unit)
      lastUnit = unit //->
      val (ratio, name) = unit
      val i = op.div(ratio, rest); rest = op.rem(ratio, rest)
      number.show(s, i); name.forEach(s)
    }
  }
  protected abstract val map: Map<Iterable<IN>, NUM>
  protected val reversedPairsDsc by lazy { map.reversedMap().toList().sortedByDescending { it.first } }
  protected fun maxCmpLE(value: NUM) = reversedPairsDsc.first { it.first <= value }
  override fun toPreetyDoc() = listOf("NumUnit", number, unit).preety().colonParens()

  protected open fun joinUnitsInitial(s: Feed<IN>, k: NUM, i: NUM): NUM? = op.times(k, i)
  protected open fun joinUnits(s: Feed<IN>, u0: NumUnit<NUM, IN>, u1: NumUnit<NUM, IN>, acc: NUM, i: NUM): NUM? = op.run { plus(times(u1.first, i), acc) }
  protected open fun joinUnitsShow(s: Output<IN>, u0: NumUnit<NUM, IN>, u1: NumUnit<NUM, IN>) {}
}

open class NumUnitTrie<IN, NUM: Comparable<NUM>>(number: Pattern<IN, NUM>, override val unit: PairedTriePattern<IN, NUM>,
    op: NumOps<NUM>): NumUnitPattern<IN, NUM>(number, unit, op) {
  override val map get() = unit.map
}
