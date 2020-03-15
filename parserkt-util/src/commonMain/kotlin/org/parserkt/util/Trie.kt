package org.parserkt.util

//// == Trie Tree ==
open class Trie<K, V>(var value: V?) { constructor(): this(null)
  val routes: MutableMap<K, Trie<K, V>> by lazy(::mutableMapOf)

  operator fun get(key: Iterable<K>): V? = getPath(key).value
  open operator fun set(key: Iterable<K>, value: V) { getOrCreatePath(key).value = value }
  operator fun contains(key: Iterable<K>) = try { this[key] != null } catch (_: NoSuchElementException) { false }

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

  override fun equals(other: Any?) = compareUsing(other) { routes == it.routes && value == it.value }
  override fun hashCode() = hash(routes, value)
  override fun toString(): String = when {
    value == null -> "Path$routes"
    value != null && routes.isNotEmpty() -> "Bin[$value]$routes"
    value != null && routes.isEmpty() -> "Term($value)"
    else -> impossible()
  }
}

//// == Abstract ==
fun <K, V> Trie<K, V>.toMap() = collectKeys().associate { it to this[it]!! }

operator fun <V> Trie<Char, V>.get(key: CharSequence) = this[key.asIterable()]
operator fun <V> Trie<Char, V>.set(key: CharSequence, value: V) { this[key.asIterable()] = value }
operator fun <V> Trie<Char, V>.contains(key: CharSequence) = key.asIterable() in this

fun <K, V> Trie<K, V>.merge(vararg kvs: Pair<Iterable<K>, V>) {
  for ((k, v) in kvs) this[k] = v
}
fun <V> Trie<Char, V>.mergeStrings(vararg kvs: Pair<CharSequence, V>) {
  for ((k, v) in kvs) this[k] = v
}

/** Create multiply route in path to [key], helper for functions like `setNocase` */
fun <K, V> Trie<K, V>.getOrCreatePaths(key: Iterable<K>, layer: (K) -> Iterable<K>): List<Trie<K, V>>
  = key.fold(listOf(this)) { points, k ->
    points.flatMap { point ->
      layer(k).map { point.routes.getOrPut(it, ::Trie) }
    }
  }
