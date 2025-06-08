package net.logicsquad.ibis;

import java.util.Objects;

public class Word {
	private final String text;

	private final int start;

	private Word(String text, int start) {
		Objects.requireNonNull(text);
		this.text = text;
		this.start = start;
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
		Objects.requireNonNull(text);
		if (start < 0) {
			throw new IllegalArgumentException("start cannot be negative.");
		}
		return new Word(text, start);
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
		if (!(obj instanceof Word)) {
			return false;
		}
		Word other = (Word) obj;
		return start == other.start && Objects.equals(text, other.text);
	}

	@Override
	public String toString() {
		return "Word [start=" + start + ", text=" + text + "]";
	}

	public int length() {
		return text().length();
	}

	public char charAt(int i) {
		return text().charAt(i);
	}
}
