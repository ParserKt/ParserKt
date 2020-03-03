package org.parserkt.pat

import org.parserkt.*
import org.parserkt.util.*

// File: pat/PatternMisc
// Error-handling (clam) & Stated-Feed (alsoDo)

//// == Error Handling (addErrorList, clamWhile, clam) ==
// Input.addErrorList(): Pair<List<BoundError>, Input>
// CharInput.addErrorList(): Pair<List<LocatedError>, CharInput>

typealias BoundError<IN> = Pair<IN, ErrorMessage>
fun <IN> Input<IN>.addErrorList(): Pair<List<BoundError<IN>>, Input<IN>> {
  val errorList: MutableList<BoundError<IN>> = mutableListOf()
  onError = @Suppress("unchecked_cast") { message -> errorList.add((peek as IN) to message) }
  return Pair(errorList, this)
}
typealias LocatedError = Pair<SourceLocation, ErrorMessage>
fun CharInput.addErrorList(): Pair<List<LocatedError>, CharInput> {
  val errorList: MutableList<LocatedError> = mutableListOf()
  onError = { message -> errorList.add(this@addErrorList.sourceLoc.clone() to message) }
  return Pair(errorList, this)
}

/** Add error and __skip unacceptable__ [pat], yield [defaultValue] */
fun <IN, T> Feed<IN>.clamWhile(pat: Pattern<IN, *>, defaultValue: T, message: ErrorMessage): T {
  this.error(message)
  while (pat.read(this) != notParsed) {}
  return defaultValue
}
fun <IN, T> Pattern<IN, T>.clamWhile(pat: Pattern<IN, *>, defaultValue: T, messager: ErrorMessager) = object: OptionalPattern<IN, T>(this, defaultValue) {
  override fun read(s: Feed<IN>) = this@clamWhile.read(s) ?: s.clamWhile(pat, defaultValue, s.messager())
}

/** Add error, consume item until __pattern parses__ or feed end */
open class SatisfyClam<IN>(self: SatisfyPattern<IN>, val messager: ErrorMessager): SatisfyPatternBy<IN>(self) {
  override fun read(s: Feed<IN>): IN? = self.read(s) ?: run { s.error(s.messager())
    var parsed: IN? = null
    while (parsed == notParsed) {
      s.consumeOrNull() ?: break
      parsed = self.read(s)
    }; return@run parsed }
}
class SatisfyEqualToClam<IN>(override val self: SatisfyEqualTo<IN>, messager: ErrorMessager): SatisfyClam<IN>(self, messager), MonoConstantPattern<IN> {
  override val constant get() = self.constant
}

fun unexpectedMessager(expecting: Any): ErrorMessager = {"unexpected $peek, expecting $expecting"}
fun <IN> SatisfyPattern<IN>.clam(messager: ErrorMessager = unexpectedMessager(this@clam)) = SatisfyClam(this, messager)
fun <IN> SatisfyEqualTo<IN>.clam(messager: ErrorMessager = unexpectedMessager(this@clam)) = SatisfyEqualToClam(this, messager)

//// == State & Modify State ==

// StatedInput, StatedCharInput, stateAs<ST>
interface State<out ST> { val state: ST }

open class StatedInput<T, ST>(feed: Feed<T>, override val state: ST): Input<T>(feed), State<ST>
open class StatedCharInput<ST>(feed: Feed<Char>, file: String, override val state: ST): CharInput(feed, file), State<ST>

fun <T, ST> Feed<T>.withState(value: ST) = StatedInput(this, value)
fun <ST> CharInput.withState(value: ST) = StatedCharInput(this, sourceLoc.file, value)

@Suppress("UNCHECKED_CAST")
inline fun <reified ST> AllFeed.stateAs(): ST? = (this as? State<ST>)?.state

// PatternAlsoDo
// SatisfyAlsoDo, SatisfyEqualToAlsoDo
// Pattern.alsoDo(op), ...
typealias AlsoDo<IN> = ConsumerOn<AllFeed, IN>

class PatternAlsoDo<IN, T>(self: Pattern<IN, T>, val op: AlsoDo<T>): PatternWrapper<IN, T>(self) {
  override fun read(s: Feed<IN>) = super.read(s)?.also { s.op(it) }
  override fun wrap(item: Pattern<IN, T>) = PatternAlsoDo(this, op)
}
open class SatisfyAlsoDo<IN>(self: SatisfyPattern<IN>, val op: AlsoDo<IN>): SatisfyPatternBy<IN>(self) {
  override fun read(s: Feed<IN>): IN? = self.read(s)?.also { s.op(it) }
}
class SatisfyEqualToAlsoDo<IN>(override val self: SatisfyEqualTo<IN>, op: AlsoDo<IN>): SatisfyAlsoDo<IN>(self, op), MonoConstantPattern<IN> {
  override val constant get() = self.constant
}

fun <IN, T> Pattern<IN, T>.alsoDo(op: AlsoDo<T>) = PatternAlsoDo(this, op)
fun <IN> SatisfyPattern<IN>.alsoDo(op: AlsoDo<IN>) = SatisfyAlsoDo(this, op)
fun <IN> SatisfyEqualTo<IN>.alsoDo(op: AlsoDo<IN>) = SatisfyEqualToAlsoDo(this, op)
