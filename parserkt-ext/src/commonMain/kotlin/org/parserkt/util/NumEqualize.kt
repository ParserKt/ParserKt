package org.parserkt.util

// File: util/NumEqualize
// NumEqualize can make +-*/ ops avaliable for all Number instances
typealias EqualizedOps = Lift<*>
typealias EqualizedOp = EqualizedOps.(Number, Number) -> Number

/** Lift [Number] subclass operations to [Number]'s operations, using [NumOps.from] and [NumOps.to] function */
open class Lift<NUM: Comparable<NUM>>(private val ops: NumOps<NUM>): BasicNumOps<Number> {
  private operator fun Number.unaryPlus() = ops.from(this)
  private fun NUM.coerce() = ops.to(this)
  override val zero = ops.zero.coerce()
  override fun from(n: Number) = n
  override fun to(n1: Number) = n1
  override fun unaryMinus(a: Number) = ops.unaryMinus(+a).coerce()
  override fun plus(b: Number, a: Number) = ops.plus(+b, +a).coerce()
  override fun minus(b: Number, a: Number) = ops.minus(+b, +a).coerce()
  override fun times(b: Number, a: Number) = ops.times(+b, +a).coerce()
  override fun div(b: Number, a: Number) = ops.div(+b, +a).coerce()
  override fun rem(b: Number, a: Number) = ops.rem(+b, +a).coerce()
  fun lessThan(b: Number, a: Number) = +a < +b
  fun greaterThan(b: Number, a: Number) = +a > +b
}

open class NumEqualize {
  /** Gets priority ordinal for [n]. If is floating, return negative */
  protected open fun numberLevel(n: Number) = when {
    n is Byte -> 20
    n is Short -> 15
    n is Int -> 10
    n is Long -> 5
    n is Float -> -5
    n is Double -> -10
    else -> unsupported("get level of ${n}: ${n::class}")
  }
  protected open fun opsForLevel(level: Int): EqualizedOps = opsMap.getValue(level)

  protected fun <N: Comparable<N>> makeOpsMap(ops: Iterable<NumOps<N>>)
  = ops.associate { numberLevel(it.zeroNum) to Lift(it) }

  protected val <NUM> BasicNumOps<NUM>.zeroNum: Number get() = this.to(zero)

  @Suppress("UNCHECKED_CAST")
  private fun <N: Comparable<N>> opsList(): List<NumOps<N>>
    = listOf(ByteOps, ShortOps, IntOps, LongOps, FloatOps, DoubleOps) as List<NumOps<N>>
    //^ N: Comparable<N>, dirty workaround for inconsistent N (Int, Long, Float, ...)
  private val opsMap = makeOpsMap(opsList<Int>())

  /** Fold two levels, merge them into one */
  protected open fun balance(level_b: Int, level_a: Int): Int {
    fun family(level: Int) = if (level < 0) "real" else "int"
    require(level_b * level_a >= 0) {"incompatible number families (${family(level_b)} $level_b vs. ${family(level_a)} $level_a)"}
    return kotlin.math.min(level_b, level_a)
  }

  //// Merge all operations using numberLevel & balance & opsForLevel!
  protected inline fun balanced(join: EqualizedOp, b: Number, a: Number): Number {
    val unifiedLevel = balance(numberLevel(b), numberLevel(a))
    val unifiedOps = opsForLevel(unifiedLevel)
    return unifiedOps.join(b, a)
  }
  protected inline fun balancedCmp(join: EqualizedOps.(Number, Number) -> Boolean, b: Number, a: Number): Boolean {
    val unifiedLevel = balance(numberLevel(b), numberLevel(a))
    val unifiedOps = opsForLevel(unifiedLevel)
    return unifiedOps.join(b, a)
  }
  protected fun lookupOps(n: Number) = opsForLevel(numberLevel(n))

  //// "Boilerplate" defs
  fun unaryMinus(a: Number) = lookupOps(a).unaryMinus(a)
  fun plus(b: Number, a: Number) = balanced(EqualizedOps::plus, b, a)
  fun minus(b: Number, a: Number) = balanced(EqualizedOps::minus, b, a)
  fun times(b: Number, a: Number) = balanced(EqualizedOps::times, b, a)
  fun div(b: Number, a: Number) = balanced(EqualizedOps::div, b, a)
  fun rem(b: Number, a: Number) = balanced(EqualizedOps::rem, b, a)

  fun lessThan(b: Number, a: Number) = balancedCmp(EqualizedOps::lessThan, b, a)
  fun greaterThan(b: Number, a: Number) = balancedCmp(EqualizedOps::greaterThan, b, a)

  companion object Default: NumEqualize()
}

object NumEqualizeLossy: NumEqualize() {
  override fun balance(level_b: Int, level_a: Int) = kotlin.math.min(level_b, level_a)
}
