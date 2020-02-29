package org.parserkt

import java.io.Reader
import java.io.InputStream
import java.io.InputStreamReader

import java.nio.charset.Charset
import java.nio.charset.StandardCharsets.UTF_8

class ReaderFeed(reader: Reader): StreamFeed<Char, Int, Reader>(reader) {
  constructor(s: InputStream, charset: Charset = UTF_8): this(InputStreamReader(s, charset))

  override fun bufferIterator(stream: Reader) = object: Iterator<Int> {
    override fun hasNext() = nextOne != (-1) //always true
    override fun next() = stream.read()
  }
  override fun convert(buffer: Int) = buffer.toChar()

  override fun consume(): Char {
    if (nextOne == (-1)) throw Feed.End()
    else return super.consume()
  }
}

fun inputOf(reader: Reader, file: String = "<read>") = CharInput(ReaderFeed(reader), file)

val CharInput.Companion.STDIN by lazy { CharInput(ReaderFeed(System.`in`), "<stdin>") }
