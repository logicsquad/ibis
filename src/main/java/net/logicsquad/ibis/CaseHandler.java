package net.logicsquad.ibis;

import java.util.List;
import java.util.Queue;

/**
 * {@link Handler} implementation that works by running a {@link Word} through a list of potential {@link Case}s that might modify it.
 * 
 * @author paulh
 * @since 0.2
 */
class CaseHandler implements Handler {
	/**
	 * List of component {@link Case}s
	 */
	private final List<Case> cases;

	/**
	 * Constructor
	 * 
	 * @param cases list of component {@link Case}s
	 */
	public CaseHandler(List<Case> cases) {
		this.cases = cases;
		return;
	}

	@Override
	public Word handle(Word word, String text, Queue<Word> queue) {
		for (Case c : cases) {
			if (c.predicate(word)) {
				if (c.recurseOnResult()) {
					return handle(c.handle(word, text, queue), text, queue);
				} else {
					return c.handle(word, text, queue);
				}
			}
		}
		return word;
	}
}
