package org.parserkt.pat.ext

import org.parserkt.*
import org.parserkt.util.*
import org.parserkt.pat.*
import org.parserkt.pat.complex.*

// File: pat/ext/MiscHelper
typealias CharPattern = MonoPattern<Char>
typealias CharOptionalPattern = MonoOptionalPattern<Char>
typealias CharPatternWrapper = MonoPatternWrapper<Char, Char>
typealias CharConstantPattern = MonoConstantPattern<Char>

typealias CharSatisfyEqualTo = SatisfyEqualTo<Char>

//// == Pattern toLong, toPair ==
fun <IN> Pattern<IN, Int>.toLongPat() = Convert(this, Int::toLong, Long::toInt)

fun <IN, T, TUPLE: Tuple<T>> Seq<IN, T, TUPLE>.toPairPat(type: (Cnt) -> TUPLE): Pattern<IN, MonoPair<T>> {
  require(items.size == 2) {"2 items for a pair"}
  return Convert(this, { it[0] to it[1] }, { tupleOf(type, it.first, it.second) })
}

//// == Frequently used Parser Rules ==
typealias ClamlyFormat = AllFeed.(MonoPair<CharSatisfyEqualTo>) -> String
abstract class LexicalBasics {
  protected val digit = digitFor('0'..'9')
  protected val sign = Convert(elementIn('+', '-').toDefault('+'), { it == '-' }, { if(it) '-' else null })

  protected val bin = digitFor('0'..'1'); val octal = digitFor('0'..'8')
  protected val hex = Decide(digit, digitFor('A'..'F', 'A', 10), digitFor('a'..'f', 'a', 10)).mergeFirst { if (it in 0..9) 0 else 1 }

  protected val numInt = RepeatUn(asInt(), digit) { i -> i.toString().map { it - '0' } }
  protected val numLong = RepeatUn(asLong(), digit) { i -> i.toString().map { it - '0' } }

  protected open val white: SatisfyPattern<Char> = elementIn(' ', '\t', '\n', '\r') named "white"
  protected val ws by lazy(LazyThreadSafetyMode.NONE) { stringFor(white).toConstant("") }
  protected val ws1 by lazy(LazyThreadSafetyMode.NONE) { Repeat(asString(), white).toConstant(" ") }

  protected fun <T> Pattern<Char, T>.tokenize() = SurroundBy(ws to ws, this)
  protected fun <T> Pattern<Char, T>.tokenizePunction() = SurroundBy(ws to ws.toConstant(" "), this)
  protected fun <T> Pattern<Char, T>.split() = SurroundBy(ws to ws1, this)

  protected infix fun <SEP, ITEM> Pattern<Char, SEP>.seprated(item: Pattern<Char, ITEM>): Pattern<Char, List<ITEM>>
    = JoinBy(this, item).mergeConstantJoin().toDefault(emptyList()).tokenize()

  companion object Helper {
    private val clamlyFormat: ClamlyFormat = { pair ->
      val fromTag = stateAs<ExpectClose>()?.remove(pair)?.let {" (from ${it.tag})"} ?: ""
      "expecting ${pair.second}$fromTag"
    }
    fun clamly(pair: MonoPair<CharSatisfyEqualTo>, format: ClamlyFormat = clamlyFormat) = pair.first.alsoDo {
      sourceLoc?.let { stateAs<ExpectClose>()?.add(pair, it.clone()) }
    } to pair.second.clam { format(pair) }
    fun clamly(pair: MonoPair<String>) = clamly(pair.toCharPat())

    //// == Pattern Templates ==
    fun digitFor(cs: CharRange, zero: Char = '0', pad: Int = 0): Convert<Char, Char, Int>
      = Convert(elementIn(cs), { (it - zero) +pad }, { zero + (it -pad) })
    fun stringFor(char: CharPattern) = Repeat(asString(), char).Many()
    fun stringFor(char: CharPattern, surround: MonoPair<CharPattern>): Pattern<Char, StringTuple> {
      val terminate = surround.second.toStringPat()
      return Seq(::StringTuple, surround.first.toStringPat(), Until(terminate, asString(), char), terminate)
    }

    fun prefix1(head: CharPattern, item: Pattern<Char, String>) = Convert(Seq(::StringTuple, head.toStringPat(), item),
      { it[0] + it[1] }, { it.run { tupleOf(::StringTuple, take(1), drop(1)) } })
    fun suffix1(tail: CharPattern, item: CharPattern) = Convert(Seq(::StringTuple, *item until tail),
      { it[0] + it[1] }, { it.run { tupleOf(::StringTuple, take(length -1), last().toString()) } })

    val newlineChar = elementIn('\r', '\n') named "newline"
    val singleLine = suffix1(newlineChar, anyChar)
  }

  open class ExpectClose {
    private val map: MutableMap<Any, MutableList<SourceLocation>> = mutableMapOf()
    fun add(id: Any, sourceLoc: SourceLocation) { map.getOrPut(id, ::mutableListOf).add(sourceLoc) }
    fun remove(id: Any): SourceLocation = map.getValue(id).removeLast()
  }
}

fun itemNocase(char: Char) = elementIn(char.toUpperCase(), char).toConstant(char)

fun <V> Trie<Char, V>.getOrCreatePathsNocase(key: CharSequence) = getOrCreatePaths(key.asIterable()) { listOf(it.toUpperCase(), it.toLowerCase()) }
fun <V> Trie<Char, V>.setNocase(key: CharSequence, value: V) = getOrCreatePathsNocase(key).forEach { it.value = value }
fun <V> Trie<Char, V>.mergeStringsNocase(vararg kvs: Pair<CharSequence, V>) { for ((k, v) in kvs) this.setNocase(k, v) }

/** Make definitions like `inline fun <reified T> term() = itemTyped<T, TOKEN>()` */
inline fun <reified T: T0, T0> itemTyped(crossinline predicate: Predicate<T> = {true}): MonoPatternWrapper<T0, T> = object: SatisfyPattern<T0>() {
  override fun test(value: T0) = value is T && predicate(value)
  override fun toPreetyDoc() = T::class.preety().surroundText(parens)
}.let { pat -> Convert(pat, { it as T }, {it}) }
