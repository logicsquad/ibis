package net.logicsquad.ibis;

import java.util.List;
import java.util.Objects;

public class Word {
	private final String text;

	private final int start;

	private final List<String> suggestions;

	private Word(String text, int start) {
		this(text, start, null);
	}

	private Word(String text, int start, List<String> suggestions) {
		Objects.requireNonNull(text);
		this.text = text;
		this.start = start;
		this.suggestions = suggestions;
		return;
	}

	public String text() {
		return text;
	}

	public int start() {
		return start;
	}

	public int end() {
		return start + text.length();
	}

	public static Word of(String text, int start) {
		return of(text, start, null);
	}

	public static Word of(String text, int start, List<String> suggestions) {
		Objects.requireNonNull(text);
		if (start < 0) {
			throw new IllegalArgumentException("start cannot be negative.");
		}
		return new Word(text, start, suggestions);
	}

	@Override
	public int hashCode() {
		return Objects.hash(start, text);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof Word other)) {
			return false;
		}
        return start == other.start && Objects.equals(text, other.text);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Word [start=");
		sb.append(start);
		sb.append(", text=");
		sb.append(text);
		if (suggestions != null) {
			sb.append(", suggestions=(");
			sb.append(String.join(", ", suggestions));
			sb.append(")");
		}
		sb.append(']');
		return sb.toString();
	}

	public int length() {
		return text().length();
	}

	public char charAt(int i) {
		return text().charAt(i);
	}

	public List<String> suggestions() {
		return suggestions;
	}

	public Word withSuggestions(List<String> suggestions) {
		return new Word(text, start, suggestions);
	}
}
