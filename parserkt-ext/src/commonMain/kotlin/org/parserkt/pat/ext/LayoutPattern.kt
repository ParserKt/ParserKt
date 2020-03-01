package org.parserkt.pat.ext

import org.parserkt.*
import org.parserkt.util.*
import org.parserkt.pat.*

// File: pat/ext/LayoutPattern

/** item<fun sample()> tail<where> children<...{layout item}> */
sealed class Deep<T, L> {
  interface HasItem<T> { val item: T }

  data class Root<T, L>(val nodes: List<Deep<T, L>>): Deep<T, L>() {
    override fun toString() = "Root $nodes"
  }
  data class Nest<T, L>(override val item: T, val tail: L, val children: List<Deep<T, L>>): Deep<T, L>(), HasItem<T> {
    override fun toString() = "$item$tail { ${children.joinToString()} }"
  }
  data class Term<T, L>(override val item: T): Deep<T, L>(), HasItem<T> {
    override fun toString() = item.toString()
  }

  fun <R> visitBy(visitor: Visitor<T, L, R>): R = when (this) {
    is Root -> visitor.see(this)
    is Nest -> visitor.see(this)
    is Term -> visitor.see(this)
  }
  interface Visitor<T, L, out R> {
    fun see(t: Root<T, L>): R
    fun see(t: Nest<T, L>): R
    fun see(t: Term<T, L>): R
  }
}

typealias LayoutRec<T, L> = Pair<Int, List<Deep<T, L>>>?
open class LayoutPattern<IN, T, L>(val item: Pattern<IN, T>, val tail: Pattern<IN, L>, val layout: Pattern<IN, Int>): PreetyPattern<IN, Deep<T, L>>() {
  protected open val layoutZero = 0
  protected open fun rescueLayout(s: Feed<IN>, parsed: T): Int? = notParsed
  protected open fun rescueLayout(s: Feed<IN>, parsed: T, parsedTail: L): Int? = notParsed
  override fun toPreetyDoc() = listOf("Layout", item, tail, layout).preety().colonParens()

  /** [Pattern.show] for resulting pattern should be general, since [show] does not use this function */
  protected open fun decideLayerItem(parsed: T, parsedTail: L): Pattern<IN, T> = item

  override fun read(s: Feed<IN>): Deep<T, L>? {
    val (closed, layout) = readRec(s) ?: return notParsed
    onRootIndent(s, closed)
    return Deep.Root(layout)
  }
  override fun show(s: Output<IN>, value: Deep<T, L>?) {
    if (value == null) return
    value.visitBy(ShowVisitor(s))
  }

  fun readRec(s: Feed<IN>, layerItem: Pattern<IN, T>, n0: Int): LayoutRec<T, L> {
    val layerItems: MutableList<Deep<T, L>> = mutableListOf()
    var parsed: T? = layerItem.read(s)
    var parsedTail: L? = tail.read(s)
    while (parsed != notParsed) {
      if (parsedTail != notParsed) {
        val n1 = layout.read(s) ?: rescueLayout(s, parsed, parsedTail) ?: return notParsed
        val (closed, items1) = readRec(s, decideLayerItem(parsed, parsedTail), n1) ?: return notParsed
        val layer1 = Deep.Nest(parsed, parsedTail, items1)
        layerItems.add(layer1)
        onNestIndent(s, n0, n1, closed, parsedTail, layerItems)
        if (closed < n0) return Pair(closed, layerItems)
      } else {
        val term = Deep.Term<T, L>(parsed)
        layerItems.add(term)
        val n = layout.read(s) ?: rescueLayout(s, parsed) ?: return notParsed
        onTermIndent(s, n0, n, parsed)
        if (n < n0) return Pair(n, layerItems)
      }
      parsed = layerItem.read(s)
      parsedTail = tail.read(s)
    }
    return Pair(n0, layerItems)
  }
  fun readRec(s: Feed<IN>, n0: Int = layoutZero) = readRec(s, item, n0)

  protected inner class ShowVisitor(private val s: Output<IN>, private val indent: Int = 1): Deep.Visitor<T, L, Unit> {
    private var level = layoutZero
    override fun see(t: Deep.Root<T, L>) { t.nodes.forEach { it.show() } }
    override fun see(t: Deep.Nest<T, L>) {
      layout.show(s, level)
      item.show(s, t.item); tail.show(s, t.tail)
      level += indent; t.children.forEach { it.show() }; level -= indent
    }
    override fun see(t: Deep.Term<T, L>) { layout.show(s, level); item.show(s, t.item) }
    private fun Deep<T, L>.show() = visitBy(this@ShowVisitor)
  }

  protected open fun onRootIndent(s: Feed<IN>, closed: Int) {
    if (closed != 0) s.error("terminate indent not zero: $closed")
  }
  protected open fun onNestIndent(s: Feed<IN>, n0: Int, n1: Int, closed: Int, parsedTail: L, layerItems: MutableList<Deep<T, L>>) {
    if (n1 <= n0) s.error("bad layout-open decrement ($n0 => $n1)")
  }
  protected open fun onTermIndent(s: Feed<IN>, n0: Int, n: Int, parsed: T) = when {
    n <= n0 -> Unit
    n0 < n -> s.error("illegal layout increment ($n0 => $n) near $parsed")
    else -> impossible()
  }
}
