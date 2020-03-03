package org.parserkt.util

interface BasicNumOps<NUM> {
  val zero: NUM
  fun from(n: Number): NUM
  fun to(n1: NUM): Number

  fun unaryMinus(a: NUM): NUM
  fun plus(b: NUM, a: NUM): NUM
  fun minus(b: NUM, a: NUM): NUM
  fun times(b: NUM, a: NUM): NUM
  fun div(b: NUM, a: NUM): NUM
  fun rem(b: NUM, a: NUM): NUM
}
interface NumOps<NUM: Comparable<NUM>>: BasicNumOps<NUM> {
  open class Instance<NUM: Comparable<NUM>>(
    override val zero: NUM, private val from: (Number) -> NUM,
    private val unaryMinus: Pipe<NUM>,
    private val plus: InfixJoin<NUM>, private val minus: InfixJoin<NUM>,
    private val times: InfixJoin<NUM>, private val div: InfixJoin<NUM>, private val rem: InfixJoin<NUM>,
    private val to: (NUM) -> Number = { it as Number }
  ): NumOps<NUM> {
    override fun from(n: Number) = from.invoke(n)
    override fun to(n1: NUM) = to.invoke(n1)
    override fun unaryMinus(a: NUM) = unaryMinus.invoke(a)
    override fun plus(b: NUM, a: NUM) = plus.invoke(a, b)
    override fun minus(b: NUM, a: NUM) = minus.invoke(a, b)
    override fun times(b: NUM, a: NUM) = times.invoke(a, b)
    override fun div(b: NUM, a: NUM) = div.invoke(a, b)
    override fun rem(b: NUM, a: NUM) = rem.invoke(a, b)
  }
}

private inline fun low(crossinline join: Byte.(Byte) -> Int): InfixJoin<Byte> = { b, a -> join(a, b).toByte() }
private inline fun lowS(crossinline join: Short.(Short) -> Int): InfixJoin<Short> = { b, a -> join(a, b).toShort() }

object ByteOps: NumOps.Instance<Byte>(0, Number::toByte, { (-it).toByte() }, low(Byte::plus), low(Byte::minus), low(Byte::times), low(Byte::div), low(Byte::rem))
object ShortOps: NumOps.Instance<Short>(0, Number::toShort, { (-it).toShort() }, lowS(Short::plus), lowS(Short::minus), lowS(Short::times), lowS(Short::div), lowS(Short::rem))

object IntOps: NumOps.Instance<Int>(0, Number::toInt, Int::unaryMinus, Int::plus, Int::minus, Int::times, Int::div, Int::rem)
object LongOps: NumOps.Instance<Long>(0L, Number::toLong, Long::unaryMinus, Long::plus, Long::minus, Long::times, Long::div, Long::rem)
object FloatOps: NumOps.Instance<Float>(0.0F, Number::toFloat, Float::unaryMinus, Float::plus, Float::minus, Float::times, Float::div, Float::rem)
object DoubleOps: NumOps.Instance<Double>(0.0, Number::toDouble, Double::unaryMinus, Double::plus, Double::minus, Double::times, Double::div, Double::rem)
