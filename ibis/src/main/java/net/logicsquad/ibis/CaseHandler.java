package net.logicsquad.ibis;

import java.util.List;

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
	public List<Word> handle(Word word, String text) {
		for (Case c : cases) {
			if (c.predicate().test(word)) {
				List<Word> result = c.handle(word, text);
				return c.recurseOnResult() ? result.stream().flatMap(w -> handle(w, text).stream()).toList() : result;
			}
		}
		return List.of(word);
	}
}
