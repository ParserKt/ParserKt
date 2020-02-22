package org.parserkt.pat.complex

import org.parserkt.*
import org.parserkt.util.*
import org.parserkt.pat.*

// File: pat/complex/AuxiliarySJ
// "SJIT"
// SurroundBy(surround: Pair<ConstantPattern?, ConstantPattern?>, item)

typealias SurroundPair<IN, T> = Pair<ConstantPattern<IN, T>?, ConstantPattern<IN, T>?>
class SurroundBy<IN, T, SURR>(val surround: SurroundPair<IN, SURR>, item: Pattern<IN, T>): PatternWrapper<IN, T>(item) {
  override fun read(s: Feed<IN>): T? {
    val (left, right) = surround
    if (left != null) left.read(s) ?: return notParsed
    val parsed = item.read(s)
    if (right != null) right.read(s) ?: return notParsed
    return parsed
  }
  override fun show(s: Output<IN>, value: T?) {
    if (value == null) return
    val (leftV, rightV) = surround.map { it?.constant }
    val (left, right) = surround
    leftV?.let { left!!.show(s, it) }
    item.show(s, value)
    rightV?.let { right!!.show(s, it) }
  }
  override fun wrap(item: Pattern<IN, T>) = SurroundBy(surround, item)
  override fun toPreetyDoc() = item.preety().surround(surround.first.preetyOrNone() to surround.second.preetyOrNone())
}

//// == Surround Shorthands ==
infix fun <IN, T, SURR> Pattern<IN, T>.prefix(item: ConstantPattern<IN, SURR>) = SurroundBy(item to null, this)
infix fun <IN, T, SURR> Pattern<IN, T>.suffix(item: ConstantPattern<IN, SURR>) = SurroundBy(null to item, this)

// JoinBy(sep, item) { onItem, onSep; Rescue(rescue), AddListeners(onItem, onSep), OnItem(onItem) }

typealias DoubleList<A, B> = Tuple2<List<A>, List<B>>
open class JoinBy<IN, SEP, ITEM>(val sep: Pattern<IN, SEP>, val item: Pattern<IN, ITEM>): PreetyPattern<IN, DoubleList<ITEM, SEP>>() {
  protected open fun rescue(s: Feed<IN>, doubleList: DoubleList<ITEM, SEP>): ITEM? = notParsed.also { s.error("expecting item for last seprator $sep") }
  override fun read(s: Feed<IN>): DoubleList<ITEM, SEP>? {
    val items: MutableList<ITEM> = mutableListOf()
    val seprators: MutableList<SEP> = mutableListOf()
    fun readItem() = item.read(s)?.also { items.add(it); onItem(it) }
    fun readSep() = sep.read(s)?.also { seprators.add(it); onSep(it) }
    val doubleList: DoubleList<ITEM, SEP> = Tuple2(items, seprators)

    readItem() ?: return notParsed
    var seprator = readSep()
    while (seprator != notParsed) {
      readItem() ?: if (sep.defaultValue?.let { seprator == it } ?: false) return doubleList
        else rescue(s, doubleList) ?: return notParsed
      seprator = readSep()
    }
    return doubleList
  }
  override fun show(s: Output<IN>, value: DoubleList<ITEM, SEP>?) {
    if (value == null) return
    val (values, sepratorList) = value
    val seprators = sepratorList.iterator()
    item.show(s, values.firstOrNull() ?: return)
    try { values.drop(1).forEach { sep.show(s, seprators.next()); item.show(s, it) } }
    catch (_: NoSuchElementException) { error("missing seprator: ${sepratorList.size} vs. ${values.size}") }
  }
  override fun toPreetyDoc() = listOf(item, sep).preety().joinText("...").surroundText(braces)

  protected open fun onItem(value: ITEM) {}
  protected open fun onSep(value: SEP) {}

  inner class Rescue(private val rescue: (Feed<IN>, DoubleList<ITEM, SEP>) -> ITEM?): JoinBy<IN, SEP, ITEM>(sep, item) {
    override fun rescue(s: Feed<IN>, doubleList: DoubleList<ITEM, SEP>) = rescue.invoke(s, doubleList)
  }

  inner open class AddListeners(private val onItem: Consumer<ITEM>, private val onSep: Consumer<SEP>): JoinBy<IN, SEP, ITEM>(sep, item) {
    override fun onItem(value: ITEM) = onItem.invoke(value)
    override fun onSep(value: SEP) = onSep.invoke(value)
  }
  inner class OnItem(onItem: Consumer<ITEM>): AddListeners(onItem, {})
}

//// == Merge JoinBy Join ==
fun <IN, SEP, ITEM> JoinBy<IN, SEP, ITEM>.mergeConstantJoin(constant: SEP) = mergeSecond { (1..it.lastIndex).map{constant} }
fun <IN, SEP, ITEM> JoinBy<IN, SEP, ITEM>.mergeConstantJoin() = mergeConstantJoin(sep.constant ?: sep.item?.constant ?: error("$sep not constant"))

fun <IN, ITEM> JoinBy<IN, Char, ITEM>.concatCharJoin() = Convert(this,
  { Tuple2(it.first, it.second.joinToString("")) },
  { Tuple2(it.first, it.second.toList()) })
