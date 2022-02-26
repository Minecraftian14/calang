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

Built-in types are `INTEGER`, `BOOLEAN` and `BYTES`.

Variables are not initialized. Their value at program start-up is always unspecified.
You should not rely on default values.

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

### Jumping to paragraphs

From the *main paragraph*, and *only* from the main paragraph, you can jump
to any other paragraph:
```
BEGIN.
  PRINT Here is the beginning of our Calang journey
  PERFORM DUMMY
  PRINT Wooh! That was quite a thing already

DUMMY.
  PRINT (don't mind me)
```
(Note: currently the implementation allows jumping from everywhere. This is a bad idea.)

## Calang Object-Oriented Principle

Despite first impression, Calang is meant to be an object-oriented programming language.
Contrary to standard perspective, objects are not described by classes;
or at least not in a Calang way. In Calang, variables are objects and the
way they respond to events is driven by their type.

Types are atomic and part of the compiler implementation.

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
will print *The size of | Hello :-) | is 9*.

This example must be read as follows:
- `$SENTENCE` is a `BYTES` instance, `$SIZE` is a `INTEGER` instance.
- At begin of the program:
-- We *ask* `$SENTENCE` to store byte-data "Hello :-)"
-- We *send* `$SENTENCE` the `|.|` message with no-arguments,
-- and we *ask* `$SIZE` to store the result
- We print.

The way information is stored in an object state, and the way objects respond to
stimuli (operators), are all an implementation detail of the compiler.
The Calang NPL is a semantic phrasing of algorithms.

## Calang subprograms

Programs in Calang may call other programs to achieve tasks or compute information.

A Calang program can specify its input and output fields, using decorators on the `DECLARE` keyword:
```
DECLARE INPUT $X1 BYTES
DECLARE OUTPUT $A INTEGER

BEGIN.
  COMPT IN $A $X1 |.|
```
No other change is required.

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

# Future of Calang

It is likely that Calang will eventually disappear.
In the meantime, Calang growth expectations is expected to be roughly as follows:

1. version 2: improve type-safety, hoare logic (?) and assertions (?)
2. version 3: literate programming
3. version 4: use the abstract types and the built-in service loader to make types and operators system modular

