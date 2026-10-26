package net.logicsquad.ibis;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Unit tests on {@link Word}.
 * 
 * @author paulh
 */
public class WordTest {
	@Test
	public void textCannotBeNull() {
		assertThrows(NullPointerException.class, () -> new Word(null, 0));
		return;
	}

	@Test
	public void startCannotBeNegative() {
		assertThrows(IllegalArgumentException.class, () -> new Word("foo", -1));
		return;
	}
}
