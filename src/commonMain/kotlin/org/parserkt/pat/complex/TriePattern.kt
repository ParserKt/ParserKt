package org.parserkt.pat.complex

import org.parserkt.*
import org.parserkt.util.*
import org.parserkt.pat.*

// File: pat/complex/TriePattern

class MapPattern<K, V>(val map: Map<K, V>, private val noKey: Feed<K>.(K) -> V? = {notParsed}): PreetyPattern<K, V>() {
  override fun read(s: Feed<K>): V? {
    val key = s.peek; val value = map[key]
    return if (value == null) s.noKey(key)
    else if (s.isStickyEnd()) notParsed else value
  }
  override fun show(s: Output<K>, value: V?) { if (value != null) reverseMap[value]?.let(s) }
  private val reverseMap = map.reversedMap()
  override fun toPreetyDoc(): PP = listOf("map", map).preety().colonParens()
}

/*
val dict = KeywordPattern<String>().apply { mergeStrings("hello" to "你好", "world" to "世界") }
val noun = Repeat(asList(), dict)
*/
typealias KeywordPattern<V> = TriePattern<Char, V>
typealias PairedKeywordPattern<V> = PairedTriePattern<Char, V>

open class TriePattern<K, V>: Trie<K, V>(), Pattern<K, V> {
  override fun read(s: Feed<K>): V? {
    var point: Trie<K, V> = this
    while (true)
      try { point = point.routes[s.peek]?.also { onItem(s.consume()) } ?: break }
      catch (_: Feed.End) { onEOS(); break }
    return point.value?.also(::onSuccess) ?: onFail()
  }
  override fun show(s: Output<K>, value: V?) {
    if (value == null) return
    reverseMap[value]?.let { it.forEach(s) }
  }
  private val reverseMap by lazy { toMap().reversedMap() }
  override fun toPreetyDoc(): PP = super.toString().preety()

  protected open fun onItem(value: K) {}
  protected open fun onSuccess(value: V) {}
  protected open fun onFail(): V? = notParsed
  protected open fun onEOS() {}
  override fun toString() = toPreetyDoc().toString()
}

open class PairedTriePattern<K, V>: TriePattern<K, V>() {
  val map: Map<Iterable<K>, V> get() = pairedMap
  val reverseMap: Map<V, Iterable<K>> get() = pairedReverseMap
  private val pairedMap: MutableMap<Iterable<K>, V> = mutableMapOf()
  private val pairedReverseMap: MutableMap<V, Iterable<K>> = mutableMapOf()

  override fun set(key: Iterable<K>, value: V) {
    pairedMap[key] = value; pairedReverseMap[value] = key
    return super.set(key, value)
  }
  override fun show(s: Output<K>, value: V?) {
    if (value == null) return super.show(s, value)
    reverseMap[value]?.let { it.forEach(s) }
  }
}
