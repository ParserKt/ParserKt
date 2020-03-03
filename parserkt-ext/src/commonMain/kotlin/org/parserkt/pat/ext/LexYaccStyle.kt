package org.parserkt.pat.ext

import org.parserkt.*
import org.parserkt.util.*
import org.parserkt.pat.*

// File: pat/ext/LexYaccStyle
//// == Old-style Parsing (Regex TextPattern, LexerFeed) ==

/** Pattern for reading with [Regex], input string is taken by [item] from [Feed] stream */
open class TextPattern<T>(item: Pattern<Char, String>, val regex: Regex, val transform: (List<String>) -> T): ConvertPatternWrapper<Char, String, T>(item) {
  constructor(regex: Regex, transform: (List<String>) -> T): this(LexicalBasics.singleLine, regex, transform)
  override fun read(s: Feed<Char>): T? = item.read(s)?.let { regex.find(it)?.groupValues?.let(transform) }
  override open fun show(s: Output<Char>, value: T?) {}
  override fun wrap(item: Pattern<Char, String>) = TextPattern(item, regex, transform)
  override fun toPreetyDoc() = item.toPreetyDoc() + regex.preety().surroundText("/" to "/")
}

/** Old-style lexer-parser token stream split by [tokenizer], end by [eof] */
abstract class LexerFeed<TOKEN>(private val feed: Feed<Char>): StreamFeed<TOKEN, TOKEN?, Feed<Char>>(feed) {
  abstract fun tokenizer(): Pattern<Char, TOKEN>
  protected abstract val eof: TOKEN

  override fun bufferIterator(stream: Feed<Char>) = object: Iterator<TOKEN?> {
    private val token = tokenizer()
    override fun next() = token.read(stream)
    override fun hasNext() = nextOne != notParsed
  }
  override fun convert(buffer: TOKEN?) = buffer ?: eof.also { onEOF() }

  protected open fun onEOF() {
    if (!feed.isStickyEnd()) feed.error("lexer failed at here, ${feed.peek}")
  }
}
