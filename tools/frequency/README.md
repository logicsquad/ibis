Word Frequencies
================

These tools make `ibis/src/main/resources/en/frequency.txt.gz`, which
lists the built-in dictionary's words in lower case, most common first.
Ibis uses it to rank suggestions.
See [#27](https://github.com/logicsquad/ibis/issues/27).

None of this is part of the build. Run it again only to change the
corpus or the method, from the root of the repository.

The corpus
----------
The corpus is public domain, so the counts carry no rights from it,
and the frequency list is dedicated to the public domain under CC0
1.0. It has two sources, which count equally:

- **Project Gutenberg:** the 310 books in `gutenberg.txt`, chosen by
  `Select.java` from the Gutenberg catalog with a seeded random sample:
  150 works of literature, and up to 8 books from each other Library
  of Congress class. They are downloaded from the
  `gutenberg.pglaf.org` mirror, as Project Gutenberg asks of automated
  downloads, and their Project Gutenberg headers and footers are
  removed.
- **The Compilation of Presidential Documents** for 2019 and 2024,
  from govinfo.gov: statements, remarks, interviews and press
  conferences, as modern English to offset Gutenberg's older books.
  These are works of the US government, so in the public domain.

Steps
-----
1. Choose the books, if the corpus should change. The catalog is
   https://www.gutenberg.org/cache/epub/feeds/pg_catalog.csv.

       java tools/frequency/Select.java pg_catalog.csv > tools/frequency/gutenberg.txt

2. Download the corpus. This takes about an hour, since it waits a
   second between requests, and can be stopped and run again.

       java tools/frequency/Fetch.java <corpus directory>

3. Count the words.

       mvn -pl ibis compile dependency:build-classpath -Dmdep.outputFile=target/classpath.txt
       java -cp ibis/target/classes:$(cat ibis/target/classpath.txt) tools/frequency/Count.java <corpus directory> ibis/src/main/resources/en/frequency.txt.gz

`Count.java` finds words with Ibis's own tokenizer, so they are counted
as Ibis checks them, and ignores case. A word's frequency is its share
of each source's words, averaged over the sources. The list keeps the
30,000 most common words in the built-in dictionary that appear at
least three times.

Then run the tests: `SuggestionQualityTest` reports how well
suggestions rank the intended word for the misspellings in
`ibis/src/test/resources/misspellings.txt`. How much frequency counts
against edit distance is set by `RANK_WEIGHT` and `SOUNDS_LIKE_BONUS`
in `Dictionary`.
