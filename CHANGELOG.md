# Changelog

This project adheres to [Semantic
Versioning](https://semver.org/spec/v2.0.0.html).

## [0.1] - 2025-06-18

Initial public release.

## [0.2] - 2025-12-30
### Added
- Made simplest use case
  simpler. [#1](https://github.com/logicsquad/ibis/issues/1)
- Added rudimentary support for
  internationalization. [#3](https://github.com/logicsquad/ibis/issues/3)

### Changed
- Replaced usage of `Deque` with
  `Queue`. [#6](https://github.com/logicsquad/ibis/issues/6)

### Fixed
- Fixed a tokenization edge
  case. [#5](https://github.com/logicsquad/ibis/issues/5)

## [0.3] - 2026-10-25
### Changed
- The `addWords()` overloads now fail consistently: each throws
  `IllegalArgumentException` naming the word list when it can't be
  found or read, malformed UTF-8 is an error in resources and files
  alike, and `addWords(Path)` decompresses `.gz` files.
  [#13](https://github.com/logicsquad/ibis/issues/13)

### Fixed
- Fixed the tokenizer silently dropping the rest of the text after
  words such as "A's" or "A-B".
  [#7](https://github.com/logicsquad/ibis/issues/7)
- Fixed `StringIndexOutOfBoundsException` in three edge cases.
  [#8](https://github.com/logicsquad/ibis/issues/8)
- Fixed built-in word lists not loading for `en_US`, `en_GB`, `en_AU`
  or the no-argument `builder()`.
  [#9](https://github.com/logicsquad/ibis/issues/9)
- Fixed case conversion depending on the JVM's default locale.
  [#10](https://github.com/logicsquad/ibis/issues/10)
- Fixed `Dictionary.Builder.build()` sharing its words with the new
  `Dictionary`, which is now immutable and thread-safe.
  [#11](https://github.com/logicsquad/ibis/issues/11)
- Fixed `Word` keeping a reference to the caller's suggestions list.
  [#12](https://github.com/logicsquad/ibis/issues/12)
- Fixed errors in the documentation and POM metadata.
  [#14](https://github.com/logicsquad/ibis/issues/14)

## [0.4] - 2026-10-26
### Added
- Added `Dictionary.builtIn(Locale)`, which builds each language's
  built-in dictionary once and shares it, and `Dictionary.with()`,
  which adds words to a dictionary without copying it. A second
  `new Checker()` now takes under a millisecond rather than 25 ms.
  [#21](https://github.com/logicsquad/ibis/issues/21)
- Added `Dictionary.locale()`, so a caller chooses the locale once,
  when building the dictionary.
  [#15](https://github.com/logicsquad/ibis/issues/15)
- Added a `Checker(Dictionary, Tokenizer)` constructor for a caller's
  own tokenizer. [#17](https://github.com/logicsquad/ibis/issues/17)
- Added `addWords(Class, String)`, `addWords(InputStream)` and
  `addWords(Collection)` to `Dictionary.Builder`.
  [#20](https://github.com/logicsquad/ibis/issues/20)

### Changed
- `Checker.checkSpelling(String)` now returns a list of `Misspelling`s,
  whose suggestions are never null, and `Word` is a record of a word's
  text and position. [#16](https://github.com/logicsquad/ibis/issues/16)
- `Dictionary.isCorrect()` and `suggestionsFor()` now take a `String`,
  and `suggestionsFor()` returns an empty list for a correctly spelled
  word rather than throwing.
  [#19](https://github.com/logicsquad/ibis/issues/19)
- `Tokenizer` is now a reusable, thread-safe strategy whose
  `tokenize(String)` returns a `Stream<Word>`, created by
  `Tokenizer.forLocale()`.
  [#17](https://github.com/logicsquad/ibis/issues/17)
- `Checker(Dictionary)` now chooses its tokenizer from the
  dictionary's locale, which case conversion also uses.
  [#15](https://github.com/logicsquad/ibis/issues/15)
- `Dictionary.Builder.addWords()` is renamed `addBuiltInWords()`, and
  layers the builder's words over the shared built-in dictionary.
  [#20](https://github.com/logicsquad/ibis/issues/20),
  [#21](https://github.com/logicsquad/ibis/issues/21)
- Word lists are now recognised as gzipped by their content rather
  than a `.gz` name, streams and readers a caller opened are left open,
  and every loading failure throws `UncheckedIOException`.
  [#20](https://github.com/logicsquad/ibis/issues/20)

### Removed
- Removed `Checker.checkSpelling(Tokenizer)`,
  `Tokenizer.newInstance()`, `Word.of()` and `Word.suggestions()`.
  [#16](https://github.com/logicsquad/ibis/issues/16),
  [#17](https://github.com/logicsquad/ibis/issues/17)
- Removed `Dictionary.Builder.addWords(String)` in favour of
  `addWords(Class, String)`, which can see a caller's own resources.
  [#20](https://github.com/logicsquad/ibis/issues/20)
- `Handler`, `Rejector` and `DefaultTokenizer` are no longer public.
  [#18](https://github.com/logicsquad/ibis/issues/18)

### Fixed
- Fixed `Checker.checkSpelling(String)` always tokenizing as English,
  and the tokenizer depending on the JVM's default locale.
  [#15](https://github.com/logicsquad/ibis/issues/15),
  [#17](https://github.com/logicsquad/ibis/issues/17)

## [0.5] - 2026-10-26
### Added
- Added `Dictionary.suggestionsFor(String, int)`, which returns up to
  a given number of suggestions.
  [#22](https://github.com/logicsquad/ibis/issues/22)
- Added a design document, `docs/design.md`, and an overview in the
  package Javadoc. [#24](https://github.com/logicsquad/ibis/issues/24)
- Added copyright and licence information for every file, following
  the REUSE specification, and the licence texts to the jar and POM.
  [#25](https://github.com/logicsquad/ibis/issues/25)

### Changed
- Suggestions are now ordered best first, by edit distance, and
  `suggestionsFor(String)` and `Checker` return at most 10.
  [#22](https://github.com/logicsquad/ibis/issues/22)
- The edit distance now counts a transposition of adjacent characters
  as one edit, and ignores case.
  [#22](https://github.com/logicsquad/ibis/issues/22)
- Suggestions now include words one edit away from the misspelling,
  not only words that sound like it, and words differing only in case
  are suggested once.
  [#23](https://github.com/logicsquad/ibis/issues/23)
- Logic Squad's built-in word lists are now dedicated to the public
  domain under CC0 1.0.
  [#25](https://github.com/logicsquad/ibis/issues/25)

### Removed
- Removed the dependency on Apache Commons Text.
  [#22](https://github.com/logicsquad/ibis/issues/22)

### Fixed
- Fixed the documented source and licence of the built-in proper
  names list, which comes from 4.4BSD-Lite under the BSD 3-Clause
  License. [#25](https://github.com/logicsquad/ibis/issues/25)

## [0.6] - 2026-10-28
### Added
- Added a word frequency list, the 30,000 most common built-in words,
  which Ibis uses to rank suggestions. It was counted from public
  domain texts by the tools in `tools/frequency`, and is dedicated to
  the public domain under CC0 1.0.
  [#27](https://github.com/logicsquad/ibis/issues/27)

### Changed
- Suggestions are now ordered by a score that weighs each one's edit
  distance against how common it is, and whether it sounds like the
  misspelling. The intended word comes first for 94% of the 251
  misspellings in `SuggestionQualityTest`, up from 78%, and "the" now
  comes first for "teh" rather than 8th.
  [#27](https://github.com/logicsquad/ibis/issues/27)
- `http`, `https` and `ftp` outside a URL are now checked like any
  other word. [#28](https://github.com/logicsquad/ibis/issues/28)

### Fixed
- Fixed the parts of URLs and email addresses being checked as words,
  such as "asmade" in a link to legislation.gov.au. URLs, with a scheme
  or starting with "www.", and email addresses are now skipped whole.
  [#28](https://github.com/logicsquad/ibis/issues/28)
