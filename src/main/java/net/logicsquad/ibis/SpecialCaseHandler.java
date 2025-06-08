package net.logicsquad.ibis;

import java.util.Queue;

public class SpecialCaseHandler {
	public Word handle(Word word, String text, Queue<Word> queue) {
		if (word.text().contains("—")) {
			int i = word.text().indexOf('—');
			queue.add(Word.of(word.text().substring(i + 1), word.start() + i + 1));
			return Word.of(word.text().substring(0, i), word.start());
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
}
