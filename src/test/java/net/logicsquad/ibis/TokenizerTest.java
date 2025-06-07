package net.logicsquad.ibis;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

public class TokenizerTest {
	private static final String TEST_1 = """
			Now (is the time) for all, good—men to come to the aid of the party.
			""";

	private static final List<String> EXPECTED_1 = List.of("Now", "is", "the", "time", "for", "all", "good", "men", "to", "come", "to", "the", "aid", "of",
			"the", "party");

	private static final String TEST_2 = """
			Sometimes, e.g. and then later i.e. Crazy!
			""";

	private static final List<String> EXPECTED_2 = List.of("Sometimes", "e.g.", "and", "then", "later", "i.e.", "Crazy");

	@Test
	public void test1() {
		Tokenizer tokenizer = new Tokenizer(TEST_1);
		for (int i = 0; i < EXPECTED_1.size(); i++) {
			assertEquals(EXPECTED_1.get(i), tokenizer.next().text());
		}
		return;
	}

	@Test
	public void test2() {
		Tokenizer tokenizer = new Tokenizer(TEST_2);
		for (int i = 0; i < EXPECTED_2.size(); i++) {
			assertEquals(EXPECTED_2.get(i), tokenizer.next().text());
		}
		return;
	}
}
