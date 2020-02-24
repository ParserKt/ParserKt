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

//// == Fold for number I/L/F/D ==
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
    fun itemNocase(char: Char) = elementIn(char.toUpperCase(), char).toConstant(char)

    fun <V> Trie<Char, V>.getOrCreatePathsNocase(key: CharSequence) = getOrCreatePaths(key.asIterable()) { listOf(it.toUpperCase(), it.toLowerCase()) }
    fun <V> Trie<Char, V>.setNocase(key: CharSequence, value: V) = getOrCreatePathsNocase(key).forEach { it.value = value }
    fun <V> Trie<Char, V>.mergeStringsNocase(vararg kvs: Pair<CharSequence, V>) { for ((k, v) in kvs) this.setNocase(k, v) }

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

    inline fun <reified T, T0> itemTyped() where T: T0 = object: SatisfyPattern<T0>() {
      override fun test(value: T0) = value is T
      override fun toPreetyDoc() = T::class.preety().surroundText(parens)
    }
  }

  open class ExpectClose {
    private val map: MutableMap<Any, MutableList<SourceLocation>> = mutableMapOf()
    fun add(id: Any, sourceLoc: SourceLocation) { map.getOrPut(id, ::mutableListOf).add(sourceLoc) }
    fun remove(id: Any): SourceLocation = map.getValue(id).removeLast()
  }
}

//// == Old-style Parsing (Regex TextPattern, LexerFeed) ==
val newlineChar = elementIn('\r', '\n') named "newline"
val singleLine = LexicalBasics.suffix1(newlineChar, anyChar)

/** Pattern for reading with [Regex], input string is taken by [item] from [Feed] stream */
open class TextPattern<T>(item: Pattern<Char, String>, val regex: Regex, val transform: (List<String>) -> T): ConvertPatternWrapper<Char, String, T>(item) {
  constructor(regex: Regex, transform: (List<String>) -> T): this(singleLine, regex, transform)
  override fun read(s: Feed<Char>): T? = item.read(s)?.let { regex.find(it)?.groupValues?.let(transform) }
  override open fun show(s: Output<Char>, value: T?) {}
  override fun wrap(item: Pattern<Char, String>) = TextPattern(item, regex, transform)
  override fun toPreetyDoc() = item.toPreetyDoc() + regex.preety().surroundText("/" to "/")
}

/** Old-style lexer-parser token stream split by [tokenizer] */
abstract class LexerFeed<TOKEN>(feed: Feed<Char>): StreamFeed<TOKEN, TOKEN?, Feed<Char>>(feed) {
  abstract fun tokenizer(): Pattern<Char, TOKEN>
  protected abstract val eof: TOKEN

  override fun bufferIterator(stream: Feed<Char>) = object: Iterator<TOKEN?> {
    private val token = tokenizer()
    override fun next() = token.read(stream)
    override fun hasNext() = nextOne != notParsed
  }
  override fun convert(buffer: TOKEN?) = buffer ?: eof
  override fun consume(): TOKEN {
    if (nextOne == null) throw Feed.End()
    else return super.consume()
  }
}
