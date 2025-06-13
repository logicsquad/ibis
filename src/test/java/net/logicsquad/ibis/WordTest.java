package net.logicsquad.ibis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/**
 * Unit tests on {@link Word}.
 * 
 * @author paulh
 */
public class WordTest {
	@Test
	public void textCannotBeNull() {
		assertThrows(NullPointerException.class, () -> Word.of(null, 0));
		return;
	}

	@Test
	public void startCannotBeNegative() {
		assertThrows(IllegalArgumentException.class, () -> Word.of("foo", -1));
		return;
	}

	@Test
	public void toLowerCaseReturnsExpectedResult() {
		assertEquals("expected", Word.of("expected", 0).toLowerCase());
		assertEquals("expected", Word.of("Expected", 0).toLowerCase());
		assertEquals("expected", Word.of("EXPECTED", 0).toLowerCase());
		assertEquals("expected", Word.of("eXPECted", 0).toLowerCase());
		assertEquals("expected", Word.of("expecteD", 0).toLowerCase());
		return;
	}

	@Test
	public void toInitialCapReturnsExpectedResult() {
		assertEquals("Expected", Word.of("expected", 0).toInitialCap());
		assertEquals("Expected", Word.of("Expected", 0).toInitialCap());
		assertEquals("Expected", Word.of("EXPECTED", 0).toInitialCap());
		assertEquals("Expected", Word.of("eXPECted", 0).toInitialCap());
		assertEquals("Expected", Word.of("expecteD", 0).toInitialCap());
		return;
	}
}
