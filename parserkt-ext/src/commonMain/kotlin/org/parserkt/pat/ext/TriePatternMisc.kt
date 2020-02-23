package org.parserkt.pat.ext

import org.parserkt.*
import org.parserkt.util.*
import org.parserkt.pat.complex.TriePattern
import org.parserkt.pat.complex.PairedTriePattern

//// == BackTrie, DictTrie, LazyTrie, GreedyTrie ==
abstract class BackTrie<K, V>: PairedTriePattern<K, V>() {
  abstract fun split(value: V): Iterable<K>
  abstract fun join(parts: Iterable<K>): V
  val back = TriePattern<K, V>()
  override fun set(key: Iterable<K>, value: V) {
    back[split(value)] = join(key)
    return super.set(key, value)
  }
}
open class PairedDictTrie: BackTrie<Char, String>() {
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
