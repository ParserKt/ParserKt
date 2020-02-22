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
  override fun toPreetyDoc() = listOf("map", map).preety().colonParens()
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
  override fun toPreetyDoc() = super.toString().preety()

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
  abstract class BackTrie<K, V>: PairedTriePattern<K, V>() {
    abstract fun split(value: V): Iterable<K>
    abstract fun join(parts: Iterable<K>): V
    val back = TriePattern<K, V>()
    override fun set(key: Iterable<K>, value: V) {
      back[split(value)] = join(key)
      return super.set(key, value)
    }
  }
}

//// == DictTrie, LazyTrie, GreedyTrie ==
open class PairedDictTrie: PairedTriePattern.BackTrie<Char, String>() {
  override fun split(value: String) = value.asIterable()
  override fun join(parts: Iterable<Char>) = parts.joinToString("")
}
open class PairedLazyTrie<V>(private val op: (String) -> V?): PairedTriePattern<Char, V>() {
  private val currentPath = StringBuilder()
  private fun clear() { currentPath.clear() }
  override fun onItem(value: Char) { currentPath.append(value) }
  override fun onSuccess(value: V) { clear() }
  override fun onFail(): V? {
    val path = currentPath.toString()
    clear(); return op(path)?.also { this[path] = it  }
  }
}
open class PairedGreedyTrie(private val predicate: Predicate<Char>): PairedLazyTrie<String>({ it.takeIf(String::isNotEmpty) }) {
  override fun read(s: Feed<Char>) = super.read(s) ?: s.takeWhileNotEnd { it !in routes && predicate(it) }
    .joinToString("").takeIf { it.isNotEmpty() || s.peek in routes && !isEOS }
  override fun onEOS() { _isEOS = true }
  protected val isEOS get() = _isEOS.also { _isEOS = false }
  private var _isEOS = false
}
