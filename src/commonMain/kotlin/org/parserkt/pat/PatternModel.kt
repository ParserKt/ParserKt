package org.parserkt.pat

import org.parserkt.*
import org.parserkt.util.*

// File: pat/PatternModel
inline val notParsed: Nothing? get() = null
typealias Output<T> = Consumer<T>

//// "POPCorn" (Pattern, OptionalPattern, PatternWrapper, ConstantPattern)
// Pattern { read(Feed), show(Output, value) }
// val notParsed: Nothing? = null

interface Pattern<IN, T>: Preety {
  fun read(s: Feed<IN>): T?
  fun show(s: Output<IN>, value: T?)
  override fun toString(): String
}
interface OptionalPatternKind<T> { val defaultValue: T }
interface PatternWrapperKind<IN, T> { val item: Pattern<IN, T> }
interface ConstantPattern<IN, T>: Pattern<IN, T> { val constant: T }

// == Patterns ==
// SURDIES (Seq, Until, Repeat, Decide) (item, elementIn, satisfy, StickyEnd) (always, never)
// CCDP (Convert, Contextual, Deferred, Piped)
// SJIT (SurroundBy, JoinBy) (InfixPattern, TriePattern)

// == Extensions ==
// ArrangeModel: Sized, Slice, Tuple
// FoldModel: Fold
// TextPreety: Preety
// InputLayer: Feed, Input, CharInput

// == With OnItem / Rescue ==
// CharInput.OnItem
// JoinBy.OnItem, JoinBy.Rescue
// InfixPattern.Rescue

// == Special Repeat ==
// Repeat.InBounds(bounds, greedy = true)
// Repeat.Many -- with support for OptionalPattern

// == Error Handling ==
// Input.addErrorList
// clam(messager), clamWhile(pat, defaultValue, messager)
// Pattern.toDefault(defaultValue), ConstantPattern.toDefault()

// == State ==
// AllFeed.withState, AllFeed.stateAs
// alsoDo -- Pattern, SatisfyPattern, SatisfyEqualTo

abstract class PreetyPattern<IN, T>: PreetyAny(), Pattern<IN, T>

typealias MonoPattern<IN> = Pattern<IN, IN>
typealias MonoOptionalPattern<IN> = OptionalPattern<IN, IN>
typealias MonoPatternWrapper<IN, T> = ConvertPatternWrapper<IN, IN, T>
typealias MonoConstantPattern<IN> = ConstantPattern<IN, IN>

@Suppress("UNCHECKED_CAST")
inline val <IN, T> Pattern<IN, T>.defaultValue get() = (this as? OptionalPatternKind<T>)?.defaultValue
@Suppress("UNCHECKED_CAST")
inline val <IN, T> Pattern<IN, T>.item get() = (this as? PatternWrapperKind<IN, T>)?.item
@Suppress("UNCHECKED_CAST")
inline val <IN, T> Pattern<IN, T>.constant get() = (this as? ConstantPattern<IN, T>)?.constant

//// == OptionalPattern (toDefault) ==

open class OptionalPattern<IN, T>(override val item: Pattern<IN, T>, override val defaultValue: T): PreetyPattern<IN, T>(),
    OptionalPatternKind<T>, PatternWrapperKind<IN, T> {
  override fun read(s: Feed<IN>) = item.read(s) ?: defaultValue
  override fun show(s: Output<IN>, value: T?) = item.show(s, value)
  override fun toPreetyDoc() = listOf(item.preety(), defaultValue.rawPreety()).joinText("?:")
}

fun <IN, T> Pattern<IN, T>.toDefault(defaultValue: T) = OptionalPattern(this, defaultValue)
fun <IN, T> ConstantPattern<IN, T>.toDefault() = toDefault(constant)

//// == PatternWrapper / SatisfyPatternBy ==

abstract class ConvertPatternWrapper<IN, T, T1>(override val item: Pattern<IN, T>): PreetyPattern<IN, T1>(), PatternWrapperKind<IN, T> {
  abstract fun wrap(item: Pattern<IN, T>): ConvertPatternWrapper<IN, T, T1>
  override fun toPreetyDoc() = item.toPreetyDoc()
}

abstract class SatisfyPatternBy<IN>(protected open val self: SatisfyPattern<IN>): SatisfyPattern<IN>() {
  override fun test(value: IN) = self.test(value)
  override fun read(s: Feed<IN>) = self.read(s)
  override fun show(s: Output<IN>, value: IN?) = self.show(s, value)
  override fun toPreetyDoc() = self.toPreetyDoc()
}

//// == Pattern Wrappers (not, clam) ==
abstract class PatternWrapper<IN, T>(item: Pattern<IN, T>): ConvertPatternWrapper<IN, T, T>(item) {
  override fun read(s: Feed<IN>) = item.read(s)
  override fun show(s: Output<IN>, value: T?) = item.show(s, value)
}

inline operator fun <reified IN, T> MonoPatternWrapper<IN, T>.not() = wrap(!(item as SatisfyPattern<IN>))
inline fun <reified IN, T> MonoPatternWrapper<IN, T>.clam(noinline messager: ErrorMessager) = wrap((item as SatisfyPattern<IN>).clam(messager))

@Suppress("UNCHECKED_CAST") // Losing type information for T in ConvertPatternWrapper, required for fun show
inline operator fun <reified IN, T> PatternWrapper<IN, T>.not() = wrap(!(item as ConvertPatternWrapper<IN, IN, T>))
@Suppress("UNCHECKED_CAST")
inline fun <reified IN, T> PatternWrapper<IN, T>.clam(noinline messager: ErrorMessager) = wrap((item as ConvertPatternWrapper<IN, IN, T>).clam(messager))

//// == Constant Pattern ==
class PatternToConstant<IN, T>(self: Pattern<IN, T>, override val constant: T): Pattern<IN, T> by self, ConstantPattern<IN, T>
class SatisfyToConstant<IN>(self: SatisfyPattern<IN>, override val constant: IN): SatisfyPatternBy<IN>(self), MonoConstantPattern<IN>

fun <IN, T> Pattern<IN, T>.toConstant(constant: T) = PatternToConstant(this, constant)
fun <IN> SatisfyPattern<IN>.toConstant(constant: IN) = SatisfyToConstant(this, constant)

//// == Pseudo Pattern always & never ==
fun <IN, T> always(value: T): ConstantPattern<IN, T> = object: PreetyPattern<IN, T>(), ConstantPattern<IN, T> {
  override val constant = value
  override fun read(s: Feed<IN>) = constant
  override fun show(s: Output<IN>, value: T?) {}
  override fun toPreetyDoc() = listOf("always", value).preety().colonParens()
}
fun <IN> never(): Pattern<IN, *> = object: PreetyPattern<IN, Nothing>() {
  override fun read(s: Feed<IN>) = notParsed
  override fun show(s: Output<IN>, value: Nothing?) {}
  override fun toPreetyDoc() = "never".preety().surroundText(parens)
}

//// == Abstract ==
fun <T> Pattern<Char, T>.read(text: String) = read(inputOf(text))
fun <T> Pattern<Char, T>.readPartial(text: String): Pair<List<LocatedError>, T?> {
  val (e, input) = inputOf(text).addErrorList()
  return Pair(e, read(input))
}
fun <T> Pattern<Char, T>.show(value: T?): String? {
  if (value == null) return null
  val sb = StringBuilder()
  show({ sb.append(it) }, value)
  return sb.toString()
}
fun <T> Pattern<Char, T>.rebuild(text: String) = show(read(text))
fun <T> Pattern<Char, T>.rebuild(text: String, operation: ActionOn<T>) = show(read(text)?.apply(operation))

fun <IN, T> Pattern<IN, T>.read(vararg items: IN) = read(inputOf(*items))
fun <IN, T> Pattern<IN, T>.readPartial(vararg items: IN): Pair<List<BoundError<IN>>, T?> {
  val (e, input) = inputOf(*items).addErrorList()
  return Pair(e, read(input))
}
fun <IN, T> Pattern<IN, T>.show(value: T?): List<IN>? {
  if (value == null) return null
  val list: MutableList<IN> = mutableListOf()
  show({ list.add(it) }, value)
  return list
}
fun <IN, T> Pattern<IN, T>.rebuild(vararg items: IN) = show(read(*items))
fun <IN, T> Pattern<IN, T>.rebuild(vararg items: IN, operation: ActionOn<T>) = show(read(*items)?.apply(operation))

fun Pattern<Char, *>.test(value: Char) = read(SingleFeed(value)) != notParsed
fun <IN> Pattern<IN, *>.test(value: IN) = read(SingleFeed(value)) != notParsed

fun <IN, T> Pattern<IN, T>.showBy(show: (Output<IN>, T) -> Unit) = object: Pattern<IN, T> by this {
  override fun show(s: Output<IN>, value: T?) { if (value != null) show.invoke(s, value) }
}
fun <T> Pattern<Char, T>.showByString(string: (T) -> String) = showBy { s, value -> string(value).forEach(s) }
