/**
 * <p>
 * A simple spelling checker for Java applications.
 * </p>
 *
 * <p>
 * A {@link net.logicsquad.ibis.Checker} holds a {@link net.logicsquad.ibis.Dictionary} and a {@link net.logicsquad.ibis.Tokenizer}.
 * {@link net.logicsquad.ibis.Checker#checkSpelling(String)} uses the {@code Tokenizer} to split a text into
 * {@link net.logicsquad.ibis.Word}s, and returns a {@link net.logicsquad.ibis.Misspelling} for each word the {@code Dictionary} doesn't
 * recognise, with up to 10 suggestions, best first:
 * </p>
 *
 * <pre>{@code
 * Checker checker = new Checker();
 * for (Misspelling m : checker.checkSpelling("Now is the time forr all good men.")) {
 *     System.out.println(m.word().text() + ": " + m.suggestions());
 * }
 * }</pre>
 *
 * <p>
 * A {@code Dictionary} stores words by phonetic code. A word is correct if the {@code Dictionary} contains it verbatim, in lower case, or
 * with an initial capital. Suggestions are words that sound like the misspelling, or are one edit away from it, ranked by edit distance.
 * {@code Checker}, {@code Dictionary} and the built-in {@code Tokenizer} are immutable and thread-safe.
 * </p>
 *
 * <p>
 * <a href="https://github.com/logicsquad/ibis/blob/master/docs/design.md">How Ibis Works</a> describes the design in more detail.
 * </p>
 */
package net.logicsquad.ibis;
