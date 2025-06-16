![](https://github.com/logicsquad/ibis/workflows/build/badge.svg)
[![License](https://img.shields.io/badge/License-BSD_2--Clause-orange.svg)](https://opensource.org/licenses/BSD-2-Clause)

Ibis
====

What is this?
-------------
Ibis is a minimal implementation of a [spell
checker](https://en.wikipedia.org/wiki/Spell_checker) for use in Java
projects.

Getting started
---------------

Ibis requires Java 21. You can use it in your projects by including it
as a Maven dependency:

    <dependency>
      <groupId>net.logicsquad</groupId>
      <artifactId>ibis</artifactId>
      <version>0.1</version>
    </dependency>

Ibis ships with some built-in word lists, and you can start checking
text against a `Dictionary` containing these words:

    Dictionary dict = Dictionary.builder().addWords().build();
    Checker checker = new Checker(dict);
    String text = "Now is the time forr all good men to come to the aod of the party.";
    var incorrect = checker.checkSpelling(new Tokenizer(text));
    for (Word w : incorrect) {
      System.out.println("w = " + w);
    }

A `Word` object contains the start position of the word in the
original text, the incorrect word itself, and a list of suggestions
(based on phonetic matches with words in the dictionary).

    w = Word [start=16, text=forr, suggestions=(fair, fairy, far...
    w = Word [start=49, text=aod, suggestions=(ad, add, ado, aid...

You can add your own word lists to a `Dictionary` using the `Builder`
pattern:

    Dictionary.Builder builder = Dictionary.builder().addWords().addWords("/words-1.txt").addWords("/words-2.txt.gz");
    Dictionary dict = builder.build();

In this example, `words-1.txt` and `words-2.txt.gz` are resources
available on the classpath at runtime. Word lists can be compressed
with `gzip`.

Contributing
------------
By all means, open issue tickets and pull requests if you have
something to contribute.

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
License](https://opensource.org/license/bsd-2-clause).  This extends
to the word lists shipped with this project, all of which are used
here either under the [FreeBSD
License](https://www.freebsd.org/copyright/freebsd-license/) or
because they have been explicitly declared as in the [public
domain](https://en.wikipedia.org/wiki/Public_domain).
