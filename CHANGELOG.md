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
