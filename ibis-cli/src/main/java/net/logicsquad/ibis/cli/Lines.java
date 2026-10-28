package net.logicsquad.ibis.cli;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Finds the line and column of an offset in a file's text, as GNU's format for messages counts them: from 1, with a tab advancing to the
 * next of every 8 columns, so that an editor such as Emacs goes to the right place. A line ends at a line feed, a carriage return, or
 * both together.
 *
 * @author paulh
 * @since 0.7
 */
final class Lines {
	/**
	 * Columns between tab stops
	 */
	private static final int TAB_WIDTH = 8;

	/**
	 * The file's text
	 */
	private final String text;

	/**
	 * Offset of the start of each line
	 */
	private final int[] starts;

	/**
	 * Constructor
	 *
	 * @param text a file's text
	 * @throws NullPointerException if {@code text} is {@code null}
	 */
	Lines(String text) {
		this.text = Objects.requireNonNull(text);
		List<Integer> list = new ArrayList<>();
		list.add(0);
		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			if (c == '\n' || c == '\r' && (i + 1 == text.length() || text.charAt(i + 1) != '\n')) {
				list.add(i + 1);
			}
		}
		starts = list.stream().mapToInt(Integer::intValue).toArray();
		return;
	}

	/**
	 * Returns the line that {@code offset} is on.
	 *
	 * @param offset an offset in the file's text
	 * @return line, from 1
	 */
	int line(int offset) {
		int index = Arrays.binarySearch(starts, offset);
		return index >= 0 ? index + 1 : -index - 1;
	}

	/**
	 * Returns the column of {@code offset} on its line.
	 *
	 * @param offset an offset in the file's text
	 * @return column, from 1
	 */
	int column(int offset) {
		int column = 1;
		for (int i = starts[line(offset) - 1]; i < offset; i++) {
			char c = text.charAt(i);
			if (c == '\t') {
				column = (column - 1) / TAB_WIDTH * TAB_WIDTH + TAB_WIDTH + 1;
			} else if (!Character.isLowSurrogate(c)) {
				// A surrogate pair is one character
				column++;
			}
		}
		return column;
	}
}
