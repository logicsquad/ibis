![](https://github.com/logicsquad/ibis/workflows/build/badge.svg)
[![License](https://img.shields.io/badge/License-BSD_2--Clause-orange.svg)](https://opensource.org/licenses/BSD-2-Clause)

Ibis
====

What is this?
-------------
Ibis is a minimal implementation of a [spell
checker](https://en.wikipedia.org/wiki/Spell_checker) for use in Java
projects.

There are two modules:

* `ibis`: the library.
* `ibis-cli`: a command line that checks the spelling in text and XML
  files.

Getting started
---------------

Ibis requires Java 21. You can use it in your projects by including it
as a Maven dependency:

    <dependency>
      <groupId>net.logicsquad</groupId>
      <artifactId>ibis</artifactId>
      <version>0.7</version>
    </dependency>

Ibis ships with some built-in word lists, and you can start checking
text against a `Dictionary` containing these words:

    Checker checker = new Checker();
    String text = "Now is the time forr all good men to come to the aod of the party.";
    for (Misspelling m : checker.checkSpelling(text)) {
      System.out.println(m);
    }

A `Misspelling` contains the incorrect `Word`, which holds its text and
start position in the original text, and up to 10 suggestions, which
may be empty. Suggestions are words in the dictionary that are one
edit away from the misspelling, or that sound like it, closest and
most common first:

    Misspelling[word=Word[text=forr, start=16], suggestions=[for, four, fore...
    Misspelling[word=Word[text=aod, start=49], suggestions=[and, aid, add...

A `Checker` is immutable and thread-safe, so you can create one and use
it to check any number of texts.

You can add your own word lists to a `Dictionary` using the `Builder`
pattern:

    Dictionary dict = Dictionary.builder(Locale.ENGLISH)
        .addBuiltInWords()
        .addWords(MyApp.class, "/words-1.txt")
        .addWords(Path.of("words-2.txt.gz"))
        .build();
    Checker checker = new Checker(dict);

In this example, `addBuiltInWords()` adds the built-in word lists,
`words-1.txt` is a resource found on the classpath through `MyApp`, and
`words-2.txt.gz` is a file. You can also add words from an
`InputStream`, a `Reader` or a `Collection<String>`, or one at a time
with `addWord()`. Word lists must be encoded in UTF-8, with one word on
each line, and are decompressed if they are compressed with `gzip`.
Ibis leaves any stream or reader you pass it open, for you to close.

The built-in dictionary for each language is built once, on first use,
and shared: `new Checker()` and `addBuiltInWords()` both use it rather
than copying it, as does `Dictionary.builtIn(Locale)`. To add a few
words to an existing `Dictionary` without copying it, use `with()`:

    Dictionary dict = Dictionary.builtIn(Locale.ENGLISH).with(List.of("Ibis", "tokenizer"));

Using the command line
----------------------
`ibis-cli`'s jar contains everything it needs. You can fetch it from
Maven Central with:

    mvn dependency:copy -Dartifact=net.logicsquad:ibis-cli:0.7 -DoutputDirectory=.

and run it on text and XML files:

    java -jar ibis-cli-0.7.jar [options] <file>...

    --words <file>    Accept the words in <file> too, one per line,
                      gzipped or not. Give it once for each word list.
    --skip <element>  Leave out the text of XML elements with this local
                      name, such as DocBook's info. Give it once for
                      each element.
    --help            Show the help.
    --version         Show Ibis's version.

It checks a file whose name ends in `.xml` as XML, and any other as
UTF-8 text. It reports each misspelling on a line of its own, in GNU's
format for messages, so that an editor such as Emacs can go straight
to it:

    party.xml:3:25: forr (for, four, fore, form, fora, fort, ford, fork, far, fair)
    party.xml:4:18: aod (and, aid, add, god, ad, ado, at, rod, nod, cod)

It exits with status 1 if it found a misspelling, 2 if it couldn't
check a file, and 0 otherwise.

In an XML file, it checks the text of the elements, but not their
attributes, comments or processing instructions. It checks the text of
an element such as a paragraph as a whole, with the text of the
elements within it, so that a word split by markup, such as
`mis<emphasis>spel</emphasis>led`, isn't reported, but a space missing
before an element's text is. It checks separately the text of elements
that have no text of their own around them, such as table cells, and
of two elements side by side, which are often alternatives.

Internationalisation
--------------------
Ibis is currently fairly Anglocentric: it offers only English word
lists, and the only language it supports is English. You choose a
language once, when you build a `Dictionary`, and a `Checker` uses the
tokenizer for that dictionary's `locale()`. Ibis throws
`IllegalArgumentException` for a `Locale` whose language it doesn't
support. Support for other languages should be straightforward to add
if someone was keen.

Contributing
------------
By all means, open issue tickets and pull requests if you have
something to contribute.

[docs/design.md](docs/design.md) describes how Ibis works: how it
finds words, stores and checks them, and suggests corrections.

Influences
----------
The landscape of open source spell checkers for Java is a little
sparse. There are a number of options out there, though few of them
appear to be under active development. (It's certainly possible that
some are in a state where no more development is required.)  Some of
them appear to be solutions for rather specific use cases (such as
those designed to co-operate with Swing components). All of them
appear to be constrained by non-permissive licenses (usually the
[LGPL](https://en.wikipedia.org/wiki/GNU_Lesser_General_Public_License)).
While Ibis was, to some extent, _inspired by_ the
[Jazzy](https://sourceforge.net/projects/jazzy/) project (and [a fork
of it](https://github.com/magsilva/jazzy)), it is a [clean-room
design](https://en.wikipedia.org/wiki/Clean-room_design) and licensed
here under the more permissive [2-Clause BSD
License](https://opensource.org/license/bsd-2-clause).

Licensing
---------
Ibis is licensed under the [2-Clause BSD
License](https://opensource.org/license/bsd-2-clause) (see
[LICENSE](LICENSE)). The built-in word lists have their own terms,
which are all permissive: the BSD 3-Clause License, a public domain
release, or a public domain dedication under
[CC0 1.0](https://creativecommons.org/publicdomain/zero/1.0/).
[ibis/src/main/resources/en/README.md](ibis/src/main/resources/en/README.md)
gives each list's source and terms.

The project follows the [REUSE](https://reuse.software/)
specification: [REUSE.toml](REUSE.toml) records the copyright and
licence of every file, and [LICENSES](LICENSES) holds each licence's
text. The jar carries the same texts under `META-INF/LICENSES`.
