package net.logicsquad.ibis;

import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class Tokenizer {
	private final BreakIterator breakIterator = BreakIterator.getWordInstance();

	private final String text;

	private int start;

	private int end;

	private Queue<Word> queue = new LinkedList<>(); 

	public Tokenizer(String text) {
		this.text = text;
		breakIterator.setText(text);
		start = breakIterator.first();
		end = breakIterator.next();
		return;
	}

	public boolean hasNext() {
		return end != BreakIterator.DONE;
	}

	private String candidateNext() {
		String word = text.substring(start, end);
		start = end;
		end = breakIterator.next();
		return word;
	}

	public Word next() {
		if (!queue.isEmpty()) {
			return queue.remove();
		}
		String word = candidateNext();
		while (!Character.isLetterOrDigit(word.charAt(0))) {
			word = candidateNext();
		}
		if (word.contains("—")) {
			int i = word.indexOf('—');
			queue.add(new Word(word.substring(i + 1), i + 1, end));
			return new Word(word.substring(0, i), start, i);
		} else {
			return new Word(word, start, end);
		}
	}
}
