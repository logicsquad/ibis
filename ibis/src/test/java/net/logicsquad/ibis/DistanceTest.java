package net.logicsquad.ibis;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Unit tests on {@link Distance}.
 * 
 * @author paulh
 */
public class DistanceTest {
	@Test
	public void distanceIsZeroForEqualStrings() {
		assertEquals(0, Distance.between("", ""));
		assertEquals(0, Distance.between("word", "word"));
		return;
	}

	@Test
	public void distanceToEmptyStringIsLength() {
		assertEquals(4, Distance.between("word", ""));
		assertEquals(4, Distance.between("", "word"));
		return;
	}

	@Test
	public void insertionDeletionAndSubstitutionCostOne() {
		assertEquals(1, Distance.between("goverment", "government"));
		assertEquals(1, Distance.between("forr", "for"));
		assertEquals(1, Distance.between("definately", "definitely"));
		return;
	}

	// https://github.com/logicsquad/ibis/issues/22
	@Test
	public void transpositionCostsOne() {
		assertEquals(1, Distance.between("teh", "the"));
		assertEquals(1, Distance.between("wierd", "weird"));
		return;
	}

	// Optimal string alignment never edits a substring twice, so "ca" to "abc" is 3 rather than 2
	@Test
	public void substringIsNotEditedTwice() {
		assertEquals(3, Distance.between("ca", "abc"));
		return;
	}

	@Test
	public void distanceIsCaseSensitive() {
		assertEquals(1, Distance.between("Word", "word"));
		return;
	}
}
