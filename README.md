# Forewords

MIT License

Copyright (c) 2022 Judekeyser

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.

# The Calang NPL

## Calang code structure

### Variable declaration

A Calang program starts by variable declarations. A variable declaration always takes the form
```
DECLARE $variable TYPE
```
and must start at column 0. It declares a variable named `$variable` of type `TYPE`.
Every variable must start by a dollar sign.

Built-in types are `INTEGER`, `BOOLEAN`, `BYTES` and `PROGRAM`.

Variables are not initialized. Their value at program start-up is always unspecified.
You should never rely on default values.

### Paragraph

A Calang program is organized through paragraphs. Paragraphs have a name and their
declaration always ends with a dot. The content of a paragraph must start at column 2.
For example:

```
BEGIN.
  PRINT Hello buddy :-)
```
defines a paragraph named `BEGIN` composed of a single instruction.
The instruction is a `PRINT` instruction, the simplest instruction possible. You should not
enclose strings by double quotes. You can print variables if they're alone on their word
(= surrounded by spaces).

As another example, the following Calang program prints the initial value
of its only variable:
```
DECLARE SOME INTEGER

BEGIN.
  PRINT Initial value of SOME is $SOME !
```

The first paragraph of a Calang program is *always* the main one, whatever its name.
This paragraph must be the unique paragraph that is called by no one; in particular:
- unused paragraphs are forbidden
- you cannot call the main paragraph from another one
- there must be at least one paragraph

### Jumping to paragraphs

From the *main paragraph*, you can jump
to any other paragraph:
```
BEGIN.
  PRINT Here is the beginning of our Calang journey
  PERFORM DUMMY
  PRINT Wooh! That was quite a thing already

DUMMY.
  PRINT (don't mind me)
```

### Paragraph-oriented philosophy

The idea of paragraphs is to group together instructions that have the same semantic value.
If possible, a paragraph must have pre- and post-conditions, as well as invariants.
This can greatly help debugging and program understanding.

The main paragraph has a central role, as it is aimed at describing the main flow of the program
(the great picture). This is more or less obvious to do, depending on the use case.

All the paragraphs share the same registers of memory. They are not meant to be composed as
independent a-contextual functions: programs serve that purpose.

## Calang Object-Oriented Principle

Despite first impression, Calang is meant to be an object-oriented programming language.
Contrary to standard perspective, objects are not described by classes;
or at least not in a Calang way. In Calang, variables are objects and the
way they respond to events is driven by their type.

**Types** are **atomic** and **part of the compiler** implementation. (In other words, they
behave as if they were primitives, from Calang's perspective.)

As an example, the built-in BYTES objects is capable of being fed by data, and
can answer to a size `|.|` operator to feed any INTEGER object:
```
DECLARE $SENTENCE BYTES
DECLARE $SIZE INTEGER

BEGIN.
  STORE IN $SENTENCE Hello :-)
  COMPT IN $SIZE $SENTENCE |.|
  PRINT The size of | $SENTENCE | is $SIZE
```
will print `The size of | Hello :-) | is 9`.

This example must be read as follows:
- `$SENTENCE` is a `BYTES` instance, `$SIZE` is a `INTEGER` instance.
- At begin of the program:
-- We *ask* `$SENTENCE` to store byte-data "Hello :-)"
-- We *send* `$SENTENCE` the `|.|` message with no-arguments,
-- and we *ask* `$SIZE` to store the result
- We print.

The way information is stored in an object state, and the way objects respond to
stimuli (operators), are all an implementation detail of the compiler.

### Built-in object types

Calang comes with four built-in types: `INTEGER`, `BOOLEAN`, `BYTES` and `PROGRAM`.
The operators that act on them can be enriched by the compiler.

## Calang programs

Programs in Calang may call other programs to achieve tasks or compute information.

*Note*: a Calang `PROGRAM` object is more general than a Calang program. This section is about
Calang program (and not about the `PROGRAM` type).

A Calang program can specify its input and output fields, using decorators on the `DECLARE` keyword:
```
DECLARE INPUT $X1 BYTES
DECLARE OUTPUT $A INTEGER

BEGIN.
  COMPT IN $A $X1 |.|
```
No other change is required.

In some sense, a program is as pure as it works on encapsulated data.
This means that if every object in a Calang program are detached from a state, then
a Calang program becomes a pure function from its inputs, to its outputs; and this pure function
may (or not) declare additional variables for computation purposes.
(`PRINT` doesn't count in this picture).

The following example shows how a program can call the above, named `subprog`,
by using the `CALL` statement and the `>>`, `<<` binding operators:
```
DECLARE $X BYTES
DECLARE $Y INTEGER

BEGIN.
  STORE IN $X Hello World
  CALL subprog $X >> $X1 $Y << $A
  PRINT Size of $X is $Y
```

Program naming is done by checking files in the system, with extension `.calang`.

**Note**: File resolution strategy is still a working progress and this specification *will* change.

# Tangling rules

A Calang program is a `hcal` file written in HTML.
That file will be processed to get a Calang code similar to the ones shown above.
This Calang code eventually will be transpiled (or run).

## Write as you want

The very first tangling rule is to write as you prefer. Tangler program is non sensitive
to sections, subsections, titles, styles, and so on. You can therefore organize your text as you want.

## Specific markup sequences

Some sequences of markup will be processed in a special way to output a Calang program.

### Variable declarations

Variable declaration are made inline, and always inline.

The `<dfn><code>$SOME: FOO</code></dfn>` sequence will be processed
to get `DECLARE $SOME FOO` declaration.
If `class="input"` or `class="output"` is set on the `<dfn>` markup above,
the declaration will be set as an input or an output, respectively.

### Block code

Blocks of code can be made of one or many instructions.
The sequence `<pre><code>` always stands for a block of code.

## Hyperlinking

### Hyperlinking of paragraph

Paragraphs are not associated to sections. In order to define a paragraph, you
simply open a new `<a name="THE_PAR_NAME">` sequence around a text that you think suits
best the introduction of your paragraph.

Doing so, you can refer to your paragraph during `PERFORM` statements (in a block code)
by re-opening a `<a href="#THE_PAR_NAME">` followed by a text that you think suits
best the description of your paragraph in the current context.

(The Calang editor will highlight in yellow the named text if you mouseover your paragraph, so
that you can quicjly visualize where it refers to. Aside from that, you can also obviously
follow the native web navigation.)

### Hyperlink of programs

Programs are referred to in a similar manner. You can use
the sequence `<a href="/PROG_NAME">` (without `hcal` extension) on a `CALL` instruction
(in a block code). The tangler will replace it with the correct reference.

## More on paragraphs

### Paragraph ordering

You are not forced to organize your paragraph in a specific ordering.
You can start with the main paragraph, or with any other paragraph you find more relevant.

### Main paragraph

The main paragraph need not come first anymore in a literate context.
However, the universal property that it remains the unique paragraph no one links to, must
still hold true; as otherwise we couldn't guess who is the main...

From a pedagogical perspective, the main paragraph should still drive the global fow of the
program. This suggests that either it comes first (top-down teaching: you first give the global
picture, then you zoom on details), or last (bottom-up teaching: you first describe independent
details, then you explain how their organize to achieve the goal). This is up to you! (Do it
with love and care.)

### Paragraph split

Paragraphs may not be written in one block: you can decide to split your paragraphs in
different blocks. The important part is that you do it in a sequentially coherent order:
new paragraph starts when `<a name="...">` is detected.

# Testing Calang

Calang tests are, for now at least, quite constrained to development limitations.

## Building Calang

Get the project and run the `ServerProcess#main` method from the tests folder. Make sure to declare
an environment variable containing the absolute path to the `hcal-files` folder:
```
hcal-files = /absolute/path/website/hcal-files
```
It should open a web server. You should be able to browse the Calang files (hcal files)
by visiting programs by their name without extension:
```
localhost/prog
```
There is currently no batch-build, but we have composed a transpiled version in the `project.out.js`.
You can open the file `website/index.html` (out of local host: it is a detached file) to check the result.

## Unit-testing Calang

In order to unit test Calang, you will need to register JUnit dependency
(and its dependency Hamcrest) by yourself. Checkout Maven Central repository
to fetch JUnit version 13.2, together with Hamcrest version 1.3.

This dependency should be a dependency of the sub-module `calang`.

# Future of Calang

It is likely that Calang will eventually disappear.
In the meantime, Calang growth expectations is expected to be roughly 
following the issues list on Github.