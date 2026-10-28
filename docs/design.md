How Ibis Works
==============

This document describes Ibis's design: how it finds the words in a
text, decides whether each is spelled correctly, and suggests
corrections. The [README](../README.md) covers how to use it.

Overview
--------
A `Checker` holds a `Dictionary` and a `Tokenizer`. Checking a text
takes three steps:

    text
      │  Tokenizer.tokenize(text)
      ▼
    Words (text and start position)
      │  Dictionary.isCorrect(word.text())
      ▼
    incorrect Words
      │  Dictionary.suggestionsFor(word.text())
      ▼
    Misspellings (Word and up to 10 suggestions)

`Checker.checkSpelling(String)` returns the `Misspelling`s in the
order their words appear in the text.

The public API is small: `Checker`, `Dictionary` and its `Builder`,
`Tokenizer`, `Word` and `Misspelling`. Everything else is
package-private, so that it can change without breaking callers.

The `ibis-cli` module puts a command line over the library: see
[The command line](#the-command-line).

Finding words
-------------
`Tokenizer` is an interface with one method, `Stream<Word>
tokenize(String text)`, so a caller can supply their own.
`Tokenizer.forLocale(Locale)` returns the built-in one, a
`DefaultTokenizer`. A `Word` records the word's text and its start
position, as a `char` index into the original text.

`DefaultTokenizer` works in five stages.

1. **Normalising apostrophes:** it replaces typographic apostrophes
   and look-alikes (such as `’` and `ʼ`) with `'`. Each replacement
   is one `char` for one `char`, so positions in the normalised text
   are positions in the original.
2. **Finding candidates:** a `java.text.BreakIterator` for the
   tokenizer's locale splits the text into segments. Segments are
   words, but also runs of spaces and punctuation.
3. **Skipping URLs and email addresses:** it finds the URLs and email
   addresses in the text, and drops every candidate that overlaps one,
   so that "asmade" in
   `https://www.legislation.gov.au/F2024L00996/asmade/text` isn't
   checked. A URL has a scheme, such as `https://` or `mailto:`, or
   starts with `www.`, and runs to the next whitespace.
4. **Rejecting:** `Rejector` drops any candidate that:
   - is shorter than two characters;
   - doesn't start with a letter, which drops spaces and punctuation;
   - contains a digit;
   - looks like a domain name.
5. **Handling special cases:** a `Handler` rewrites or splits what's
   left, and `Rejector` checks the results again. The handler tries
   each `Case` in turn, and the first whose predicate matches handles
   the word:
   - `DashesCase` splits a word at hyphens, en dashes and em dashes,
     which `BreakIterator` keeps together ("tit-for-tat",
     "alpha—beta"), and handles each part in turn, so "Here—e.g. one"
     becomes "Here", "e.g." and "one";
   - `AbbreviationsCase` adds back the final full stop of "e.g" or
     "i.e" (in any case) when the text has one, since `BreakIterator`
     treats it as punctuation;
   - `PossessiveCase` removes a final `'s`, so "client's" is checked
     as "client".

`DefaultTokenizer` is stateless, so one instance can tokenize any
number of texts, on any number of threads. Its `Stream` is lazy, but
it finds all the candidates before returning it.

Storing words
-------------
A `Dictionary` holds its words in a map from phonetic code to the
words with that code. The code comes from the language's phonetic
encoder: for English, Commons Codec's `Metaphone`, which reduces a
word to a code of at most four characters that roughly describes how
it sounds. For example, "government" and "governments" are both
`KFRN`. The 67,046 built-in English words have 11,413 codes, so each
code holds about six words on average. The largest, `INTR`, holds 333.

`Dictionary.Builder` adds words from resources, files, streams,
readers and collections, one word per line. It strips whitespace from
each word, skips blank lines and duplicates, and recognises gzipped
input by its first two bytes. `build()` copies the map into a new,
unmodifiable `Dictionary`, so a `Dictionary` is immutable and
thread-safe, and the `Builder` can carry on.

A `Dictionary` can also share another's words instead of copying
them: it holds an optional `base`, and looking up a code merges the
words for that code from each layer. `dictionary.with(words)` returns
a new layer over `dictionary`, and `Builder.addBuiltInWords()` layers
the builder's words over the built-in dictionary.

The built-in dictionary for each language is built once, on first
use, and kept: `Dictionary.builtIn(Locale)` returns it, and `new
Checker()` uses it. Building the English one takes about 200 ms in a
cold JVM. After that, a new `Checker` costs almost nothing.

### Built-in word lists

The English word lists are in `ibis/src/main/resources/en`, and
[their README](../ibis/src/main/resources/en/README.md) gives each
list's source and terms.

| List | Words | Contents |
|---|---|---|
| `words-1.txt.gz` | 64,662 | 12dicts' 3of6game list of common words |
| `words-2.txt` | 124 | common words missing from `words-1.txt` |
| `names-1.txt.gz` | 1,308 | proper names from 4.4BSD-Lite |
| `names-2.txt` | 929 | proper names missing from `names-1.txt` |
| `acronyms.txt` | 20 | common acronyms |

The lists hold every form of each word ("run", "runs", "running")
rather than a stem and rules for its inflections, as Hunspell does.
That keeps lookup simple, and works for English.

Checking words
--------------
`Dictionary.isCorrect(String)` looks up the words with the same
phonetic code as the word, and the word is correct if one of them
is:

- the word verbatim;
- the word in lower case; or
- the word with an initial capital and the rest in lower case.

So "AUSTRALIAN" and "australian" are correct because the dictionary
has "Australian", and "The" and "THE" are correct because it has
"the". "NATO", which has other capitals, is correct only as it is. Case conversion
uses the dictionary's locale rather than the JVM's default.

Suggesting corrections
----------------------
`Dictionary.suggestionsFor(String)` returns up to 10 suggestions,
best first, and `suggestionsFor(String, int)` takes a different limit.
Suggestions come from two sources.

1. **Words that sound alike:** words with the same phonetic code as
   the misspelling, within an edit distance of 3. These catch errors
   in how a word is spelled, such as "definately".
2. **Words one edit away:** Ibis generates every string one edit from
   the misspelling, and keeps those in the dictionary. An edit is
   deleting a character, transposing two adjacent characters,
   substituting a character, or inserting one. Substitutions and
   insertions use the characters that appear in the dictionary's own
   words. A word of n characters has about 60n such strings, and each
   is looked up through the phonetic map, so this needs no index
   beyond the one the dictionary already has. These catch typing
   errors that change the phonetic code, such as "teh" (`T`) for
   "the" (`0`), and "goverment" (`KFRM`) for "government" (`KFRN`).

The edit distance is the optimal string alignment distance, in
`Distance`. It's the Levenshtein distance plus transpositions of
adjacent characters, so "wierd" is one edit from "weird". Ibis
compares both words in lower case.

Suggestions are ordered by a score, lowest first:

    score = distance + 0.15 × ln(rank) − (0.4 if it sounds alike)

where `rank` is the word's place in the language's frequency list,
from 1 for the most common, and one past the end for words not in it.
A word's probability is roughly proportional to the inverse of its
rank (Zipf's law), so this weighs how close a suggestion is against how
unlikely it is, as a noisy channel model would. Distance still matters
most: one more edit costs as much as being about 800 times less
common, and sounding alike as much as being about 14 times less
common. So "the" comes first for "teh", ahead of "tea", which sounds
alike but is rarer. Words with the same score are ordered
alphabetically.

Words that differ only in case, such as "Ward" and "ward", are
suggested once, in lower case if the dictionary has that form, since
that form is also correct with an initial capital.

### Word frequencies

The frequency list, `frequency.txt.gz`, holds the 30,000 most common
built-in words, in lower case, most common first. It was counted from
public domain texts, by the tools in
[`tools/frequency`](../tools/frequency/README.md):

- 310 Project Gutenberg books, 19 million words: 150 works of
  literature, and the rest from across the other Library of Congress
  classes; and
- the US Compilation of Presidential Documents for 2019 and 2024,
  2.7 million words of modern English: statements, remarks,
  interviews and press conferences.

Each source counts equally, whatever its size, so the older English of
the books doesn't swamp the modern. `Dictionary` loads a language's
list once, on first use, and shares it between all its dictionaries,
including those made with a `Builder`. Words not in it, such as rare
words and most words that callers add, rank after every word that is.

`SuggestionQualityTest` measures the suggestions for 251 misspellings
in `ibis/src/test/resources/misspellings.txt`, spelling errors such as
"definately" and typing errors such as "teh", and fails if they get
worse. The intended word comes first for 237 (94%), up from 196 (78%)
without frequencies, and in the top ten for 245. Most of the gain is
in typing errors, where the intended word comes first for 95 of 99, up
from 63: "the" was 8th for "teh", and "and" 8th for "adn".

Languages
---------
Everything language-specific is chosen in one place, the
package-private `Language` enum: the phonetic encoder, the built-in
word lists, the frequency list, and the `Case`s for the tokenizer.
`Language.of(Locale)` matches on `Locale.getLanguage()` alone, and
throws `IllegalArgumentException` for a language Ibis doesn't support.

A `Dictionary` records its `locale()`, and `Checker(Dictionary)`
chooses its tokenizer from it, so a caller gives a locale once, when
building the dictionary. Only English is supported. See
[#26](https://github.com/logicsquad/ibis/issues/26) for what another
language would need.

The command line
----------------
`ibis-cli` is a command line over the library, which the shade plugin
packs with its dependencies into one jar. `Main` reads the options,
builds a dictionary from the built-in one and any `--words` lists, and
checks each file with a `Checker`. It reports each misspelling as
`file:line:column: word (suggestions)`, GNU's format for messages, in
the order they appear, and exits with status 1 if there were any, or 2
if a file couldn't be checked.

A text file is one passage of text to check. For an XML file, `XmlText`
finds the passages:

- The text of an element that has text of its own, such as a
  paragraph, is one passage, with the text of the elements within it.
  So a word split by markup is checked whole, and a space missing
  before an element's text is caught.
- Each element within an element that has no text of its own, such as
  a cell in a table row, starts a passage of its own.
- Two elements side by side, with no text between them, are
  separated, since they're more often alternatives, such as text shown
  under different conditions, than parts of one word. So is the text
  on either side of an empty element.
- Attributes, comments, processing instructions and the elements given
  to `--skip` are left out.

Each character of a passage records where it came from, so that a
misspelling's line and column are right. The JDK's StAX parser parses
the document, but the locations it reports for text run a character or
two ahead, so a `Cursor` follows the parser through the document's text
instead. It moves past each piece of markup that the parser reports,
and matches each character of text with the characters it came from,
which differ for entity references, CDATA sections and carriage
returns. If the two ever disagree, the file is reported as unchecked,
rather than with wrong positions. `Lines` turns an offset into a line
and a column, counting from 1, with tab stops every 8 columns, as GNU's
format and Emacs expect.

Limitations
-----------
- **No context:** Ibis checks each word on its own, so it can't find a
  real word used in the wrong place, such as "their" for "there".
- **Unchecked words:** single letters, words containing digits, and
  words that don't start with a letter are never checked.
- **Lenient case:** a word stored in lower case or with an initial
  capital is correct in any mix of cases, so "tHe" and "australian"
  are correct.
- **Ambiguous misspellings:** frequency can't tell which of two
  equally close words was meant, so "peace" comes before "piece" for
  "peice", and "nice" before "niece" for "neice".
- **Limited suggestions:** words more than one edit away are found
  only if they sound alike, so "reccomend" doesn't get "recommend".
- **No DTDs:** `ibis-cli` doesn't read DTDs, so it can't check an XML
  file that uses an entity that a DTD defines, such as `&nbsp;`.
- **No regional variants:** `en_GB` and `en_US` share one dictionary,
  which accepts both "colour" and "color".
