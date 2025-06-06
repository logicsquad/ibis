package net.logicsquad.ibis;

import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Tokenizer implements Iterable<String> {
	private final BreakIterator breakIterator = BreakIterator.getWordInstance();

	private final List<String> list = new ArrayList<>();

	public Tokenizer(String text) {
		extractWords(text, breakIterator);
		return;
	}

	private void extractWords(String target, BreakIterator wordIterator) {
		wordIterator.setText(target);
		int start = wordIterator.first();
		int end = wordIterator.next();
		while (end != BreakIterator.DONE) {
			String word = target.substring(start, end);
			if (Character.isLetterOrDigit(word.charAt(0))) {
				list.add(word);
			}
			start = end;
			end = wordIterator.next();
		}
		return;
	}

	@Override
	public Iterator<String> iterator() {
		return list.iterator();
	}

	List<String> list() {
		return list;
	}
}
