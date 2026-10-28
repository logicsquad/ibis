package net.logicsquad.ibis;

/**
 * Computes the edit distance between two strings: the <a href=
 * "https://en.wikipedia.org/wiki/Damerau%E2%80%93Levenshtein_distance#Optimal_string_alignment_distance">optimal string alignment
 * distance</a>, which counts an insertion, deletion, substitution or transposition of adjacent characters as one edit, and never edits a
 * substring more than once.
 *
 * @author paulh
 * @since 0.5
 */
final class Distance {
	/**
	 * Private constructor for static-only class
	 */
	private Distance() {
		return;
	}

	/**
	 * Returns the optimal string alignment distance between {@code a} and {@code b}. Characters are compared exactly, so callers wanting a
	 * case-insensitive distance should convert both strings to the same case first.
	 *
	 * @param a a string
	 * @param b a string
	 * @return edit distance between {@code a} and {@code b}
	 */
	static int between(String a, String b) {
		int m = a.length();
		int n = b.length();
		// Three rows of the dynamic programming table: i - 2, i - 1 and i
		int[] previous2 = new int[n + 1];
		int[] previous = new int[n + 1];
		int[] current = new int[n + 1];
		for (int j = 0; j <= n; j++) {
			previous[j] = j;
		}
		for (int i = 1; i <= m; i++) {
			current[0] = i;
			for (int j = 1; j <= n; j++) {
				int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
				current[j] = Math.min(Math.min(previous[j] + 1, current[j - 1] + 1), previous[j - 1] + cost);
				if (i > 1 && j > 1 && a.charAt(i - 1) == b.charAt(j - 2) && a.charAt(i - 2) == b.charAt(j - 1)) {
					current[j] = Math.min(current[j], previous2[j - 2] + 1);
				}
			}
			int[] spare = previous2;
			previous2 = previous;
			previous = current;
			current = spare;
		}
		return previous[n];
	}
}
