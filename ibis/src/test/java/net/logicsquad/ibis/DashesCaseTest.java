package net.logicsquad.ibis;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Unit tests on {@link DashesCase}.
 * 
 * @author paulh
 */
public class DashesCaseTest {
	private static final char HYPHEN = '-';
	private static final char EN_DASH = '–';
	private static final char EM_DASH = '—';
	private static final char NON_DASH_1 = '!';
	private static final char NON_DASH_2 = '#';

	@Test
	public void isDashReturnsTrueForDashCharacters() {
		assertTrue(DashesCase.isDash(HYPHEN));
		assertTrue(DashesCase.isDash(EN_DASH));
		assertTrue(DashesCase.isDash(EM_DASH));
		return;
	}

	@Test
	public void isDashReturnsFalseForNonDashCharacters() {
		assertFalse(DashesCase.isDash(NON_DASH_1));
		assertFalse(DashesCase.isDash(NON_DASH_2));
		return;
	}
}
