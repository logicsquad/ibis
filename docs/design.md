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

Finding words
-------------
`Tokenizer` is an interface with one method, `Stream<Word>
tokenize(String text)`, so a caller can supply their own.
`Tokenizer.forLocale(Locale)` returns the built-in one, a
`DefaultTokenizer`. A `Word` records the word's text and its start
position, as a `char` index into the original text.

`DefaultTokenizer` works in four stages.

1. **Normalising apostrophes:** it replaces typographic apostrophes
   and look-alikes (such as `’` and `ʼ`) with `'`. Each replacement
   is one `char` for one `char`, so positions in the normalised text
   are positions in the original.
2. **Finding candidates:** a `java.text.BreakIterator` for the
   tokenizer's locale splits the text into segments. Segments are
   words, but also runs of spaces and punctuation.
3. **Rejecting:** `Rejector` drops any candidate that:
   - is shorter than two characters;
   - doesn't start with a letter, which drops spaces and punctuation;
   - contains a digit;
   - is `http`, `https` or `ftp`;
   - looks like a domain name.
4. **Handling special cases:** a `Handler` rewrites or splits what's
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

The English word lists are in `src/main/resources/en`, and
[their README](../src/main/resources/en/README.md) gives each list's
source and terms.

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

Suggestions are ordered by:

1. edit distance;
2. words that sound like the misspelling before those that don't;
3. alphabetically.

Words that differ only in case, such as "Ward" and "ward", are
suggested once, in lower case if the dictionary has that form, since
that form is also correct with an initial capital.

Across 60 common misspellings, the intended word comes first for 49,
and in the top ten for 58.

Languages
---------
Everything language-specific is chosen in one place, the
package-private `Language` enum: the phonetic encoder, the built-in
word lists, and the `Case`s for the tokenizer. `Language.of(Locale)`
matches on `Locale.getLanguage()` alone, and throws
`IllegalArgumentException` for a language Ibis doesn't support.

A `Dictionary` records its `locale()`, and `Checker(Dictionary)`
chooses its tokenizer from it, so a caller gives a locale once, when
building the dictionary. Only English is supported. See
[#26](https://github.com/logicsquad/ibis/issues/26) for what another
language would need.

Limitations
-----------
- **No context:** Ibis checks each word on its own, so it can't find a
  real word used in the wrong place, such as "their" for "there".
- **Unchecked words:** single letters, words containing digits, and
  words that don't start with a letter are never checked.
- **Lenient case:** a word stored in lower case or with an initial
  capital is correct in any mix of cases, so "tHe" and "australian"
  are correct.
- **No word frequency:** the built-in lists don't record how common
  each word is, so ties are broken alphabetically. For a short word,
  where many words are one edit away, the intended word can rank
  lower: "the" is 8th for "teh".
- **Limited suggestions:** words more than one edit away are found
  only if they sound alike, so "reccomend" doesn't get "recommend".
- **No regional variants:** `en_GB` and `en_US` share one dictionary,
  which accepts both "colour" and "color".
