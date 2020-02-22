package org.parserkt.pat.complex

import org.parserkt.*
import org.parserkt.util.*
import org.parserkt.pat.*

// File: pat/complex/InfixPattern
// InfixChain(atom, infix)

data class Precedence(val ordinal: Int, val isRAssoc: Boolean)
infix fun String.infixl(prec: Int) = this to Precedence(prec, false)
infix fun String.infixr(prec: Int) = this to Precedence(prec, true)

class InfixOp<T>(val name: String, val assoc: Precedence, val join: InfixJoin<T>): Comparable<InfixOp<T>> {
  override fun compareTo(other: InfixOp<T>) = assoc.ordinal.compareTo(other.assoc.ordinal)
  override fun toString() = name
}

infix fun <T> Pair<String, Precedence>.join(op: InfixJoin<T>) = InfixOp(first, second, op)
fun <T> KeywordPattern<InfixOp<T>>.register(op: InfixOp<T>) { this[op.name] = op }

open class InfixPattern<IN, ATOM>(val atom: Pattern<IN, ATOM>, val op: Pattern<IN, InfixOp<ATOM>>): PreetyPattern<IN, ATOM>() {
  protected open fun rescue(s: Feed<IN>, base: ATOM, op1: InfixOp<ATOM>): ATOM? = notParsed.also { s.error("infix $base parse failed at $op1") }
  override fun read(s: Feed<IN>): ATOM? {
    val base = atom.read(s) ?: return notParsed
    return infixChain(s, base)
  }
  override open fun show(s: Output<IN>, value: ATOM?) { unsupported("infix show") }
  fun infixChain(s: Feed<IN>, base: ATOM, op_left: InfixOp<ATOM>? = null): ATOM? {
    val op1 = op_left ?: op.read(s) ?: return base  //'+' in 1+(2*3)... || return atom "1"
    val rhs1 = atom.read(s) ?: rescue(s, base, op1) ?: return notParsed //"2"
    val op2 = op.read(s) ?: return op1.join(base, rhs1) //'*' //(a⦁b) END: terminated

    fun associateLeft() = infixChain(s, op1.join(base, rhs1), op2) //(a ⦁ b) ⦁ ...
    fun associateRight() = infixChain(s, rhs1, op2)?.let { op1.join(base, it) } //a ⦁ (b ⦁ ...)
    return when { // lessThan b => first
      op1 < op2 -> associateLeft()
      op1  > op2 -> associateRight()
      else -> if (op1.assoc.isRAssoc) associateRight() else associateLeft()
    }
  }
  override fun toPreetyDoc() = listOf("InfixChain", op).preety().colonParens()

  inner class Rescue(private val rescue: (Feed<IN>, ATOM, InfixOp<ATOM>) -> ATOM?): InfixPattern<IN, ATOM>(atom, op) {
    override fun rescue(s: Feed<IN>, base: ATOM, op1: InfixOp<ATOM>) = rescue.invoke(s, base, op1)
  }
}
