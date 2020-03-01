package org.parserkt

import org.parserkt.util.*

// File: InputModel
interface ErrorListener {
  var onError: ConsumerOn<AllFeed, ErrorMessage>
}
typealias ErrorMessage = String
typealias ErrorMessager = ProducerOn<AllFeed, ErrorMessage>

//// == Abstract ==
// SourceLocated { sourceLoc: SourceLocation }
// ParseError(feed, message)
// ext Feed { sourceLoc?, tag?; error(message) }

interface SourceLocated { val sourceLoc: SourceLocation }
data class SourceLocation(val file: String, var line: Cnt, var column: Cnt, var position: Cnt): Preety {
  constructor(file: String): this(file,1,0, 0) // since only consumed items adds errors, column are counted from 0
  val tag get() = listOf(file, line, column).preety().joinText(":")
  fun clone() = copy(file = file, line = line, column = column, position = position)

  override fun toPreetyDoc() = tag + ("#".preety() + position)
  override fun toString() = toPreetyDoc().toString()
}

open class ParseError(val feed: AllFeed, message: ErrorMessage): Error(message.toString())

val AllFeed.sourceLoc: SourceLocation? get() = (this as? SourceLocated)?.sourceLoc
val AllFeed.tag: PP? get() = sourceLoc?.tag
fun AllFeed.error(message: ErrorMessage) = (this as? ErrorListener)?.onError?.invoke(this, message) ?: kotlin.error(message)

//// == Input & CharInput ==
// Input { onItem, onError }
// CharInput (STDIN) { isCRLF, eol; OnItem }

open class Input<T>(protected val feed: Feed<T>): PreetyFeed<T>(), ErrorListener {
  protected open fun onItem(item: T) {}
  override var onError: ConsumerOn<AllFeed, ErrorMessage> = { message ->
    val inputDesc = this.tag ?: (this as? FilterInput<*>)?.parent?.tag ?: "parse fail near `$peek'"
    throw ParseError(this, "$inputDesc: $message")
  }

  override val peek get() = feed.peek
  override fun consume() = feed.consume().also(::onItem)
  override fun toPreetyDoc() = "Input".preety() + ":" + feed.toPreetyDoc()
}

open class CharInput(feed: Feed<Char>, file: String): Input<Char>(feed), SourceLocated {
  protected open val isCRLF = false
  protected open val eol: Char = '\n'

  override fun onItem(item: Char) {
    if (isCRLF && item == '\r' && peek == '\n') when (eol) {
      '\r' -> { consume(); newLine() }
      '\n' -> newLine() // LF will be first char of line
    } else when (item) {
      eol -> newLine()
      else -> { ++sourceLoc.column }
    }
    ++sourceLoc.position
  }
  private fun newLine() { ++sourceLoc.line; sourceLoc.column = 0 }

  override val sourceLoc = SourceLocation(file)
  override fun toPreetyDoc() = super.toPreetyDoc() + ":" + sourceLoc.preety()

  companion object Companion
  inner class OnItem(private val onItem: Consumer<Char>): CharInput(feed, sourceLoc.file) {
    override fun onItem(item: Char) { super.onItem(item); onItem.invoke(item) }
  }
}

/** Input with [ErrorListener] inherited from [parent] */
class FilterInput<T>(val parent: AllFeed, feed: Feed<T>): Input<T>(feed) {
  init { onError = (parent as? ErrorListener)?.onError ?: onError }
  override fun toPreetyDoc() = parent.toPreetyDoc()
}

//// == Abstract ==
// Slice, Iterator, Reader (Char)
fun inputOf(text: String, file: String = "<string>") = CharInput(SliceFeed(Slice(text)), file)
fun <IN> inputOf(vararg items: IN) = Input(SliceFeed(Slice(items)))

fun <IN> inputOf(iterator: Iterator<IN>) = Input(IteratorFeed(iterator))
fun <IN> inputOf(iterable: Iterable<IN>) = inputOf(iterable.iterator())
