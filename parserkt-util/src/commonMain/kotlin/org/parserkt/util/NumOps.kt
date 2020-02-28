package org.parserkt.util

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
