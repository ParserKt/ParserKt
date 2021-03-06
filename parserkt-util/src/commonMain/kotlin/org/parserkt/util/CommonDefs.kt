package org.parserkt.util

// File: util/CommonDefs
typealias Cnt = Int
typealias Idx = Int
typealias IdxRange = IntRange

typealias Producer<T> = () -> T
typealias Consumer<T> = (T) -> Unit
typealias Predicate<T> = (T) -> Boolean

typealias Pipe<T> = (T) -> T
typealias ActionOn<T> = T.() -> Unit
typealias ProducerOn<T, R> = T.() -> R
typealias ConsumerOn<T, A1> = T.(A1) -> Unit

fun unsupported(message: String): Nothing = throw UnsupportedOperationException(message)
fun impossible(): Nothing = throw IllegalStateException("impossible")

fun <E> MutableList<E>.removeLast() = removeAt(lastIndex)

typealias MonoPair<T> = Pair<T, T>
fun <T, R> MonoPair<T>.map(transform: (T) -> R): MonoPair<R> = Pair(transform(first), transform(second))

inline fun <T, reified R> Collection<T>.mapToArray(transform: (T) -> R): Array<R> {
  val mapFirst = transform(firstOrNull() ?: return emptyArray())
  val array: Array<R> = Array(size) {mapFirst}
  for ((i, x) in withIndex()) { array[i] = transform(x) }
  return array
}

inline fun <A:T, B:T, reified T> Pair<A, B>.items(): Array<T> = arrayOf(first, second)
inline fun <reified T> Collection<T>.toArray() = mapToArray {it}

// reversed map (bi-directional unchecked)
fun <K, V> Map<K, V>.reversedMap(): Map<V, K> = entries.associate { it.value to it.key }

/* val printLen = String::length then ::println */
inline infix fun <A, B, C> ((A) -> B).then(crossinline transform: (B) -> C): (A) -> C = { transform(invoke(it)) }
inline fun <A1, A2, R> flip(crossinline op: (A1, A2) -> R): (A2, A1) -> R = { b, a -> op(a, b) }
