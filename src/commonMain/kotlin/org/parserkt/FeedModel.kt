package org.parserkt

import org.parserkt.util.*

// File: FeedModel
interface Feed<out T>: Preety {
  val peek: T; fun consume(): T
  class End: NoSuchElementException("no more")
}
typealias AllFeed = Feed<*>

fun <IN> Feed<IN>.consumeOrNull() = try { consume() } catch (_: Feed.End) {null}
fun <IN> Feed<IN>.consumeIf(predicate: Predicate<IN>): IN?
  = peek.takeIf(predicate)?.let { consumeOrNull() }

fun <IN> Feed<IN>.takeWhile(predicate: Predicate<IN>): Sequence<IN>
  = sequence { while (predicate(peek)) yield(consume()) }
fun <IN> Feed<IN>.takeWhileNotEnd(predicate: Predicate<IN>): Sequence<IN>
  = sequence { while (true) yield(consumeIf(predicate) ?: break) }

fun <IN> Feed<IN>.asSequence(): Sequence<IN>
  = sequence { while (true) yield(consumeOrNull() ?: break) }
fun <IN> Feed<IN>.asIterable() = asSequence().asIterable()
fun <IN> Feed<IN>.toList() = asIterable().toList()

abstract class PreetyFeed<T>: PreetyAny(), Feed<T>

// NOTES ABOUT Feed:
// - Feed cannot be constructed using empty input
fun Feed<Char>.readText() = asIterable().joinToString("")
// - Feed.peek will yield last item *again* when EOS reached
fun AllFeed.isStickyEnd() = consumeOrNull() == null
// - Patterns like `Until(elementIn(' '), asString(), anyChar)` will fail when EOS entercounted
//      easiest workaround: append EOF or terminate char to end of *actual input*
fun <R> AllFeed.catchError(op: Producer<R>): R? = try { op() } catch (e: Exception) { this.error(e.message ?: e.toString()); null }

//// == SliceFeed & StreamFeed ==
// SliceFeed { position, viewport }
// StreamFeed { bufferIterator, convert, nextOne }
//   - IteratorFeed
//   - ReaderFeed

open class SliceFeed<T>(private val slice: Slice<T>): PreetyFeed<T>() {
  init { require(slice.isNotEmpty) {"empty input"} }
  protected var position = 0
  override val peek get() = try { slice[position] }
    catch (_: IndexOutOfBoundsException) { slice[slice.lastIndex] }
  override fun consume() = try { slice[position++] }
    catch (_: IndexOutOfBoundsException) { --position; throw Feed.End() }
  override fun toPreetyDoc(): PP = "Slice".preety() + listOf(peek.rawPreety(), viewport(slice)).joinText("...").surroundText(parens)
  protected open fun viewport(slice: Slice<T>): PP
    = (position.inbound()..(position+10).inbound()).map(slice::get)
      .let { items -> items.preety().joinText(if (items.all { it is Char }) "" else ", ") }
  private fun Idx.inbound() = coerceIn(slice.indices)
}

abstract class StreamFeed<T, BUF, STREAM>(private val stream: STREAM): PreetyFeed<T>() {
  protected abstract fun bufferIterator(stream: STREAM): Iterator<BUF>
  protected abstract fun convert(buffer: BUF): T
  private val iterator = bufferIterator(stream)
  protected var nextOne: BUF = try { iterator.next() }
    catch (_: NoSuchElementException) { require(false) {"empty input"}; impossible() }
  private var tailConsumed = false
  override val peek get() = convert(nextOne)
  override fun consume() = peek.also {
    if (iterator.hasNext()) nextOne = iterator.next()
    else if (!tailConsumed) tailConsumed = true
    else throw Feed.End()
  }
  override fun toPreetyDoc(): PP = "Stream".preety() + listOf(peek.rawPreety(), stream.preety()).joinText("...").surroundText(parens)
}

//// == Stream Feeds ==
class IteratorFeed<T>(iterator: Iterator<T>): StreamFeed<T, T, Iterator<T>>(iterator) {
  override fun bufferIterator(stream: Iterator<T>) = stream
  override fun convert(buffer: T) = buffer
}
