package org.parserkt.util

//// == Fold as for number I/L/F/D ==
fun asInt(radix: Int = 10, initial: Int = 0) = JoinFold(initial) { this*radix + it }
fun asLong(radix: Int = 10, initial: Long = 0L): Fold<Int, Long> = ConvertJoinFold(initial) { this*radix + it }

abstract class AsFloating<NUM: Comparable<NUM>>(val integral: Long): ConvertFold<Int, Long, NUM>() {
  override val initial = 0L
  override fun join(base: Long, value: Int) = base*radix + value
  override fun convert(base: Long) = op.run { plus(integral.let(::from), fraction(base.let(::from)) ) }

  protected abstract val op: NumOps<NUM>
  /** Make 0.2333 from ([NUM]) 2333.0 */
  protected abstract fun fraction(n: NUM): NUM
  protected open val radix = 10
}

fun asFloat(integral: Long) = object: AsFloating<Float>(integral) {
  override val op = FloatOps
  override tailrec fun fraction(n: Float): Float = if (n < 1.0F) n else fraction(n / radix)
}
fun asDouble(integral: Long) = object: AsFloating<Double>(integral) {
  override val op = DoubleOps
  override tailrec fun fraction(n: Double): Double = if (n < 1.0) n else fraction(n / radix)
}

//// == asCount, asMap == (T is Any? so Fold<in *, R> will accept them)
fun asCount(): Fold<Any?, Cnt> = ConvertJoinFold(0) { _ -> inc() }

fun <K, V> asMap(): Fold<Pair<K, V>, Map<K, V>> = object: EffectFold<Pair<K, V>, MutableMap<K, V>, Map<K, V>>() {
  override fun makeBase(): MutableMap<K, V> = mutableMapOf()
  override fun onAccept(base: MutableMap<K, V>, value: Pair<K, V>) { base[value.first] = value.second }
}
