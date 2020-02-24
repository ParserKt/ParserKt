# ParserKt

<div id="header" align="center">
  <img alt="ParserKt" src="//parserkt.github.io/resources/ParserKt.svg" widht="160" height="160"></img><br>
  <a href="https://jitpack.io/#parserkt/parserkt"><img alt="version" src="https://jitpack.io/v/parserkt/parserkt.svg"></img></a>・<a href="build.gradle"><img alt="kotlin" src="https://img.shields.io/badge/Kotlin-jvm&js--1.3-orange?logo=kotlin"></img></a>・<a href="//github.com/ParserKt/examples"><img alt="examples" src="https://img.shields.io/badge/-examples-yellow"></img></a>・<a href="//github.com/ParserKt/ParserKt/wiki/Comparison"><img alt="comparison" src="https://img.shields.io/badge/-comparison-informational"></img></a>
</div>

## Introduction

ParserKt is a naive one-pass __recursive descent, scannerless parser__ framework for Kotlin (mainly JVM, compatible for JS)

A parser is a kind of program extracting data from input sequences:

> NOTE: Using REPL from command line is not a good practice, you can <a href="#header">create Gradle project</a> or use IDEA Kotlin REPL.

```bash
gradle shadowJar
kotlinc -cp build/libs/ParserKt-*.jar
```

```kotlin
import org.parserkt.*  // Pattern.read(String), CharInput.STDIN, ...
import org.parserkt.pat.* // item(x), elementIn('a'..'z'), satisfy<Int> { it % 2 == 0 }, ...
import org.parserkt.util.* // asList(), asString(), ...
```

Import main, `pat`, `util`, then we can implement our own combined pattern! (for a more in depth sample, see link at <a href="#header">header</a>)

```kotlin
val digits = Repeat(asList(), elementIn('0'..'9')) //{[0-9]}
digits.read("123") //[1, 2, 3]
digits.read("12a3") //[1, 2]
digits.read("a3") //null //(= notParsed)
```

parser mainly recognize, and transform input into a simpler form (like AST) for post-processing (preetify, evaluate, type-check, ...). and it can be used to create (DSL) language tools / compilers / transpilers / interpreters.

ParserKt divides complex grammar into simple subpart `val` definition way, "entry rule" could be all public structure — like `item`, `elementIn`, `repeat`, and thus it's very easy to debug and to reuse implemented syntax.

> What means "one-pass"?

See [FeedModel.kt](src/commonMain/kotlin/org/parserkt/FeedModel.kt):

```kotlin
interface Feed<out T> {
  val peek: T; fun consume(): T
  class End: NoSuchElementException("no more")
}
```

That's all one subparser can see, no mark/reset, not even one character supported for lookahead.

> What means "recursive descent"?

Take a look of these expression: `a + b`, `a + (b + c)`

```kotlin
sealed class PlusAst {
  data class Plus(val left: PlusAst, val right: PlusAst): PlusAst()
  data class IntLiteral(val n: Int): PlusAst()
}
```

(`sealed class` is just class with determinable subclasses, in it's inner name scope)

This data structure implies, every symbol of `a + b` could be an integer (like `0`, `9`, `16`, ...`IntLiteral(n)`), or other `a + b` (`1 + 2`, `9 + 0`, ...`Plus(left, right)`)

`PlusAst` is recursive, so it's best to __implement it's parser in recurse function form__, that's "recursive".

```bnf
Plus := Int | (Plus '+' Plus)  ; "left associative"
Int := {[0-9]}
```

(`{a}` denotes repeat, `a|b` denotes alternative)

When reading input "1+2+3" by rule `Plus`, results `Plus(Plus(Int(1), Int(2)), Int(3))`.

Parser keep going to the state of reading substructure `Int` from reading `Plus`.

From rule `Plus` to its subrule `Int`, that's "descent".

——

ParserKt cannot make any lookaheads, so it's looks like impossible to parse syntax like `//`, `/*`, `0x` `0b` and error when `0(octal)`.

In fact, "lookahead" can be stored in _call stack_ of `Pattern.read` by pattern `Contextual`, so write parser for such syntax is possible (but much more complicated, so it's better to create new Pattern subclass, or fall back to tokenizer-parser `LexerFeed`, or use `TriePattern`)

> What is the difference between "parser combinator" and "parser compiler"?

Let me clarify first: I _really_ don't know what "combinator" is :(

I think combinators must be related to ["functional programming"](https://en.wikipedia.org/wiki/Functional_programming), or at least "declarative programming" — Define what it is, not how computers actually solve it.

__Parser combinator is mainly about code reuse, parser compiler is mainly about pattern matching algorithms.__

Parser compilers and parser combinators __solve the same problem in different ways__.
A parser compiler have better portability, and better performance (I'm [not sure](https://sap.github.io/chevrotain/performance/)).
A parser combinator integrates better with the host language, and it's really easy to write/debug, since it's just hand-wrote program.

For example, keywords about parser compilers:
[LL(k)](https://en.wikipedia.org/wiki/LL_parser), [LR(k)](https://en.wikipedia.org/wiki/LR_parser), [LALR](https://en.wikipedia.org/wiki/LALR_parser), [NFA](https://en.wikipedia.org/wiki/Nondeterministic_finite_automaton), [DFA](https://en.wikipedia.org/wiki/Deterministic_finite_automaton), [KMP](https://en.wikipedia.org/wiki/Knuth%E2%80%93Morris%E2%80%93Pratt_algorithm), [Aho–Corasick](https://en.wikipedia.org/wiki/Aho%E2%80%93Corasick_algorithm)

😨 Scared? Recursive descent method are the most popular practice for hand-wrote parser used by many well-known projects like [Lua](https://lua.org), and it's the __best selection for code quality__ — source files generated by parser compilers offen a mess!

## Features

+ Minimal boilerplate code & high-reusability
+ DSL style & fully object-oriented library design
+ __Rebuild parse result back to input__ (REPL friendly)
+ __Generic parsing input__ (Char, String, XXXToken, ...)
+ __One-pass input stream design__ without `hasNext`
+ `ReaderFeed` for __implementating interactive shells__ (JVM only)
+ The framework manages parsed data in a flexible also extensible way (`Tuple2`, Sized-`Tuple`, `Fold`)
+ __Supports complex contextual syntax structure that cannot be supported directly by parser compilers__ (`LayoutPattern`, `NumUnits`, ...)
+ Extensive error messages using `clam {"message"}`, `clamWhile(pat, defaultValue) {"message"}`
+ Parser input stream `Feed` can have state storage: `withState(T)`, `stateAs<T>()`
+ <500K compiled JVM library, no extra runtime (except kotlin-stdlib, Kotlin sequences are optional) needed
+ __No magics__, all code are __rewrote at least 9 times__ by author, ugly/ambiguous/orderless codes are removed

Great thanks to [Kotlin](https://kotlinlang.org/), for its strong expressibility and amazing type inference.

## Provided combinators

### Modules

+ [:parserkt-util](parserkt-util) Fundamental data types (`Slice`, `Tuple`, `Trie`, ...) and helper class (`Preety`, `AnyBy`, ...) used/prepared for ParserKt and friends
+ [:parserkt](src) for Feed/Input model and basic (`org.parserkt.pat`) / complex (`org.parserkt.pat.complex`) patterns
+ [:parserkt-ext](parserkt-ext) Extension library for real-world parsers, eliminating boilerplate code (`LayoutPattern`, `LexicalBasics`, ...)

### abbreviations

+ POPCorn (Pattern, OptionalPattern, PatternWrapper, ConstantPattern)
+ SURDIES(Seq, Until, Repeat, Decide, item, elementIn, satisfy)
+ CCDP(Convert, Contextual, Deferred, Piped)
+ SJIT(SurroundBy, JoinBy, InfixPattern, TriePattern)

## References

+ GitHub: [Simple calculator in Haskell](https://github.com/duangsuse-valid-projects/BinOps)
+ Blog post: [看完这段 Kotlin 代码后我哭了](https://duangsuse-valid-projects.github.io/Share/Others/essay-kotlin-parser)
+ Article: [Hello, declarative world](https://codon.com/hello-declarative-world)
+ Article: [Parsing in JavaScript: Tools and Libraries](https://tomassetti.me/parsing-in-javascript/)

## References on Historic

+ Origin for [Feed stream (1)](https://github.com/duangsuse-valid-projects/jison/tree/master/src/commonMain/kotlin/org/parserkt)
+ Origin for [Feed stream (2)](https://github.com/duangsuse-valid-projects/Share/tree/master/Others/kt_misc/)
+ Origin for ["pattern" model](https://github.com/duangsuse-valid-projects/SomeAXML/tree/master/src/commonMain/kotlin/org/duangsuse/bin/pat)
+ Origin for [NumOps](https://github.com/duangsuse-valid-projects/three-kt-files/blob/master/src/commonMain/kotlin/NumEqualize.kt), [RangeMap](https://github.com/duangsuse-valid-projects/jison/blob/master/src/jvmMain/kotlin/org/parserkt/util/RangeMap.kt)
