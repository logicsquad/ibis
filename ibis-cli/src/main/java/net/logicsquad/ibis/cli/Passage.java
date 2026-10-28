package net.logicsquad.ibis.cli;

/**
 * A run of text to check as a whole, with where each of its characters came from in a file's text.
 *
 * @param text    the text
 * @param offsets for each character of {@code text}, its offset in the file's text, or -1 for a space that separates the text of two
 *                elements; or {@code null} if each character's offset is its index in {@code text}
 * @author paulh
 * @since 0.7
 */
record Passage(String text, int[] offsets) {
	/**
	 * Returns the offset in the file's text of the character at {@code index} in {@link #text()}.
	 *
	 * @param index an index into {@link #text()}
	 * @return offset in the file's text, or -1 for a space that separates the text of two elements
	 */
	int offset(int index) {
		return offsets == null ? index : offsets[index];
	}
}
