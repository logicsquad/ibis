package net.logicsquad.ibis;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

/**
 * Provides some utility methods for tests.
 * 
 * @author paulh
 */
public abstract class AbstractTest {
	/**
	 * Returns {@code resource} as a string.
	 * 
	 * @param resource a file available on the classpath
	 * @return {@code resource} as a string
	 */
	public static String stringFromResource(String resource) {
		try (InputStream is = AbstractTest.class.getResourceAsStream(resource)) {
			if (is == null) {
				return null;
			}
			try (InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8); BufferedReader reader = new BufferedReader(isr)) {
				return reader.lines().collect(Collectors.joining(System.lineSeparator()));
			}
		} catch (IOException e) {
			return null;
		}
	}
}
