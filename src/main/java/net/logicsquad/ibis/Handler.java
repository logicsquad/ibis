package net.logicsquad.ibis;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class Handler {
	public Word handle(Word word, String text, Deque<Word> queue) {
		if (word.text().contains("-") || word.text().contains("–") || word.text().contains("—")) {
			return handle(handleDashes(word, text, queue), text, queue);
		} else if ("e.g".equals(word.text())) {
			if ('.' == text.charAt(word.end())) {
				return Word.of("e.g.", word.start());
			} else {
				return word;
			}
		} else if ("i.e".equals(word.text())) {
			if ('.' == text.charAt(word.end())) {
				return Word.of("i.e.", word.start());
			} else {
				return word;
			}
		} else {
			return word;
		}
	}

	private Word handleDashes(Word word, String text, Deque<Word> queue) {
		List<Word> parts = new ArrayList<>();
		int i = 0;
		while (isDash(word.text().charAt(i))) {
			i++;
		}
		for (; i < word.length();) {
			int start = i;
			StringBuilder sb = new StringBuilder();
			while (i < word.length() && !isDash(word.text().charAt(i))) {
				sb.append(word.text().charAt(i));
				i++;
			}
			parts.add(Word.of(sb.toString(), start + word.start()));
			while (i < word.length() && isDash(word.text().charAt(i))) {
				i++;
			}
		}
		if (parts.size() == 0) {
			throw new IllegalArgumentException("No text found.");
		} else if (parts.size() > 1) {
			for (int j = parts.size() - 1; j > 0; j--) {
				queue.addFirst(handle(parts.get(j), text, queue));
			}
		}
		return parts.get(0);
	}

	private static boolean isDash(char c) {
		return c == '-' || c == '–' || c == '—'; 
	}
}
