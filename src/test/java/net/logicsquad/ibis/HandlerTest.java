package net.logicsquad.ibis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Deque;
import java.util.LinkedList;

import org.junit.jupiter.api.Test;

/**
 * Unit tests on {@link Handler}.
 * 
 * @author paulh
 */
public class HandlerTest {
	private static final String TEXT_1 = " e.g. ";
	private static final Word WORD_1 = Word.of("e.g", 1);
	private static final Word EXPECTED_1 = Word.of("e.g.", 1);

	private static final String TEXT_2 = "em—dash";
	private static final Word WORD_2A = Word.of("em", 0);
	private static final Word WORD_2B = Word.of("dash", 3);

	private static final String TEXT_3 = "en–dash";
	private static final Word WORD_3A = Word.of("en", 0);
	private static final Word WORD_3B = Word.of("dash", 3);

	private static final String TEXT_4 = "non-conforming";
	private static final Word WORD_4A = Word.of("non", 0);
	private static final Word WORD_4B = Word.of("conforming", 4);

	private static final String TEXT_5 = "tit-for-tat";
	private static final Word WORD_5A = Word.of("tit", 0);
	private static final Word WORD_5B = Word.of("for", 4);
	private static final Word WORD_5C = Word.of("tat", 8);

	private static final char HYPHEN = '-';
	private static final char EN_DASH = '–';
	private static final char EM_DASH = '—';
	private static final char NON_DASH_1 = '!';
	private static final char NON_DASH_2 = '#';

	private static final String TEXT_6 = "the licensee's list";
	private static final Word WORD_6 = Word.of("licensee's", 4);
	private static final Word EXPECTED_6 = Word.of("licensee", 4);
	private static final String TEXT_7 = "entity's";
	private static final Word WORD_7 = Word.of("entity's", 0);
	private static final Word EXPECTED_7 = Word.of("entity", 0);

	@Test
	public void handleReturnsExpectedWordForEg() {
		Handler handler = new Handler();
		Word result = handler.handle(WORD_1, TEXT_1, new LinkedList<>());
		assertEquals(EXPECTED_1, result);
		return;
	}

	private void testHandlerWithTextAndTwoWords(String text, Word first, Word second) {
		Handler handler = new Handler();
		Deque<Word> queue = new LinkedList<>();
		Word result = handler.handle(Word.of(text, 0), text, queue);
		assertEquals(first, result);
		assertTrue(queue.size() == 1);
		assertEquals(second, queue.remove());
		return;
	}

	@Test
	public void handlerSplitsOnEmDash() {
		testHandlerWithTextAndTwoWords(TEXT_2, WORD_2A, WORD_2B);
		return;
	}

	@Test
	public void handlerSplitsOnEnDash() {
		testHandlerWithTextAndTwoWords(TEXT_3, WORD_3A, WORD_3B);
		return;
	}

	@Test
	public void handlerSplitsOnHyphen() {
		testHandlerWithTextAndTwoWords(TEXT_4, WORD_4A, WORD_4B);
		return;
	}

	@Test
	public void handlerSplitsOnMultiHyphen() {
		Handler handler = new Handler();
		Deque<Word> queue = new LinkedList<>();
		Word result = handler.handle(Word.of(TEXT_5, 0), TEXT_5, queue);
		assertEquals(WORD_5A, result);
		assertTrue(queue.size() == 2);
		assertEquals(WORD_5B, queue.remove());
		assertEquals(WORD_5C, queue.remove());
		return;
	}

	@Test
	public void isDashReturnsTrueForDashCharacters() {
		assertTrue(Handler.isDash(HYPHEN));
		assertTrue(Handler.isDash(EN_DASH));
		assertTrue(Handler.isDash(EM_DASH));
		return;
	}

	@Test
	public void isDashReturnsFalseForNonDashCharacters() {
		assertFalse(Handler.isDash(NON_DASH_1));
		assertFalse(Handler.isDash(NON_DASH_2));
		return;
	}

	@Test
	public void handleDealsWithPossessiveForms() {
		Handler handler = new Handler();
		Word result1 = handler.handle(WORD_6, TEXT_6, new LinkedList<>());
		assertEquals(EXPECTED_6, result1);
		Word result2 = handler.handle(WORD_7, TEXT_7, new LinkedList<>());
		assertEquals(EXPECTED_7, result2);
		return;
	}
}
