package org.parserkt.util

// File: util/TextPreety
interface Preety {
  fun toPreetyDoc(): Doc
  sealed class Doc {
    object Null: Doc()
    object None: Doc()
    data class Text(val obj: Any): Doc()
      { override fun toString() = super.toString() }
    data class SurroundBy(val lr: MonoPair<Doc>, override val sub: Doc): Doc(), DocWrapper
      { override fun toString() = super.toString() }
    data class JoinBy(val sep: Doc, override val subs: List<Doc>): Doc(), DocsWrapper
      { override fun toString() = super.toString() }
    fun <R> visitBy(visitor: Visitor<R>): R = when (this) {
      is Null -> visitor.see(this)
      is None -> visitor.see(this)
      is Text -> visitor.see(this)
      is JoinBy -> visitor.see(this)
      is SurroundBy -> visitor.see(this)
    }
    interface Visitor<out R> {
        fun see(t: Doc.Null): R
        fun see(t: Doc.None): R
        fun see(t: Doc.Text): R
        fun see(t: Doc.JoinBy): R
        fun see(t: Doc.SurroundBy): R
    }
    override fun toString() = visitBy(DocShow)
    private object DocShow: Visitor<String> {
      override fun see(t: Doc.Null) = "null"
      override fun see(t: Doc.None) = ""
      override fun see(t: Doc.Text) = t.obj.toString()
      override fun see(t: Doc.JoinBy) = t.subs.joinToString(t.sep.show(), transform = { it.show() })
      override fun see(t: Doc.SurroundBy) = "${t.lr.first.show()}${t.sub.show()}${t.lr.second.show()}"
      private fun Doc.show() = visitBy(DocShow)
    }
  }
  interface DocWrapper { val sub: Doc }
  interface DocsWrapper { val subs: List<Doc> }
}

abstract class PreetyAny: Preety {
  override fun toString() = toPreetyDoc().toString()
}

typealias PP = Preety.Doc

fun Any?.preetyOr(defaultValue: PP) = if (this == null) defaultValue else Preety.Doc.Text(this)
fun Any?.preetyOrNone() = preetyOr(Preety.Doc.None)
fun Any?.preety() = preetyOr(Preety.Doc.Null)
fun Iterable<*>.preety() = map(Any?::preety)

fun PP.surround(lr: MonoPair<PP>) = Preety.Doc.SurroundBy(lr, this)
fun List<PP>.join(sep: PP) = Preety.Doc.JoinBy(sep, this)
operator fun PP.plus(other: PP) = listOf(this, other).join(Preety.Doc.None)

fun PP.surroundText(lr: MonoPair<String>) = surround(lr.map(Any?::preety))
fun List<PP>.joinText(sep: String) = join(sep.preety())
operator fun PP.plus(other: Any?) = this + other.preety()

infix fun String.paired(other: String) = MonoPair(this, other)
fun String.monoPaired() = this paired this
val parens = "(" paired ")"
val squares = "[" paired "]"
val braces = "{" paired "}"
val quotes = "'" paired "'"
val dquotes = "\"" paired "\""

fun List<PP>.colonParens() = joinText(":").surroundText(parens)

//// == Raw Strings ==
fun CharSequence.prefixTranslate(map: Map<Char, Char>, prefix: String) = fold(StringBuilder()) { acc, char ->
  map[char]?.let { acc.append(prefix).append(it) } ?: acc.append(char)
}.toString()

/** `\"\'\t\b\n\r\$\\` */
internal val KOTLIN_ESCAPE = mapOf(
  '"' to '"', '\'' to '\'',
  '\t' to 't', '\b' to 'b',
  '\n' to 'n', '\r' to 'r',
  '$' to '$', '\\' to '\\'
)

internal val ESCAPED_CHAR = Regex("""^\\.$""")
fun String.rawString() = prefixTranslate(KOTLIN_ESCAPE, "\\").let {
  if (it.length == 1 || it.matches(ESCAPED_CHAR)) it.preety().surroundText(quotes)
  else it.preety().surroundText(dquotes)
}

fun Any?.rawPreety() = if (this == null) Preety.Doc.Null else toString().rawString()
