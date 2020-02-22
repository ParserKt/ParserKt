package org.parserkt.util

//// == Trie Tree ==
open class Trie<K, V>(var value: V?) { constructor(): this(null)
  val routes: MutableMap<K, Trie<K, V>> by lazy(::mutableMapOf)

  operator fun get(key: Iterable<K>): V? = getPath(key).value
  open operator fun set(key: Iterable<K>, value: V) { getOrCreatePath(key).value = value }
  operator fun contains(key: Iterable<K>) = try { this[key] != null } catch (_: NoSuchElementException) { false }
  fun toMap() = collectKeys().toMap { k -> k to this[k]!! }

  fun getPath(key: Iterable<K>): Trie<K, V> {
    return key.fold(initial = this) { point, k -> point.routes[k] ?: errorNoPath(key, k) }
  }
  fun getOrCreatePath(key: Iterable<K>): Trie<K, V> {
    return key.fold(initial = this) { point, k -> point.routes.getOrPut(k, ::Trie) }
  }
  fun collectKeys(): Set<List<K>> {
    if (routes.isEmpty() && value == null) return emptySet()
    val routeSet = routes.flatMapTo(mutableSetOf()) { kr ->
      val (pathKey, nextRoute) = kr
      return@flatMapTo nextRoute.collectKeys().map { listOf(pathKey)+it }
    }
    if (value != null) routeSet.add(emptyList())
    return routeSet
  }
  private fun errorNoPath(key: Iterable<K>, k: K): Nothing {
    val msg = "${key.joinToString("/")} @$k"
    throw NoSuchElementException(msg)
  }
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    return if (other !is Trie<*, *>) false
    else (routes == other.routes) && value == other.value
  }
  override fun hashCode() = routes.hashCode() xor value.hashCode()
  override fun toString(): String = when {
    value == null -> "Path".preety() + routes
    value != null && routes.isNotEmpty() -> "Bin".preety() + value.preety().surroundText(squares) + routes
    value != null && routes.isEmpty() -> "Term".preety() + value.preety().surroundText(parens)
    else -> impossible()
  }.toString()
}

//// == Abstract ==
operator fun <V> Trie<Char, V>.get(index: CharSequence) = this[index.asIterable()]
operator fun <V> Trie<Char, V>.set(index: CharSequence, value: V) { this[index.asIterable()] = value }
operator fun <V> Trie<Char, V>.contains(index: CharSequence) = index.asIterable() in this

fun <K, V> Trie<K, V>.merge(vararg kvs: Pair<Iterable<K>, V>) {
  for ((k, v) in kvs) this[k] = v
}
fun <V> Trie<Char, V>.mergeStrings(vararg kvs: Pair<CharSequence, V>) {
  for ((k, v) in kvs) this[k] = v
}

fun <K, V> Trie<K, V>.getOrCreatePaths(key: Iterable<K>, layer: (K) -> List<K>): List<Trie<K, V>> = key.fold(listOf(this)) { points, k ->
  points.flatMap { point ->
    layer(k).map { point.routes.getOrPut(it, ::Trie) }
  }
}
