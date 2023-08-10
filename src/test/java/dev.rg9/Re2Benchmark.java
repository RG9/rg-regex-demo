package dev.rg9;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;

import com.google.re2j.PatternSyntaxException;

/**
 * Results:
 * Benchmark                                 Mode  Cnt       Score      Error  Units
 * Re2Benchmark.manyAlternation_jdk         thrpt    5       0.278 ±    0.002  ops/s
 * Re2Benchmark.manyAlternations_re2        thrpt    5     111.601 ±    0.324  ops/s
 * Re2Benchmark.redos_jdk                   thrpt    5  288430.799 ± 8397.499  ops/s
 * Re2Benchmark.redos_re2                   thrpt    5  562217.227 ± 9280.337  ops/s
 * Re2Benchmark.wholeWordsEndingWithNn_jdk  thrpt    5       4.496 ±    0.292  ops/s
 * Re2Benchmark.wholeWordsEndingWithNn_re2  thrpt    5       1.582 ±    0.124  ops/s
 */
@State(Scope.Benchmark)
@Fork(1)
public class Re2Benchmark {

	private static final String REGEX_WHOLE_WORDS_ENDING_WITH_NN = "\\b\\w+nn\\b";

	/**
	 * ReDOS attack: https://owasp.org/www-community/attacks/Regular_expression_Denial_of_Service_-_ReDoS
	 * See also: src/test/other/python-redos-test.sh
	 */
	private static final String REGEX_REDOS = "^(a+)+$";

	String redosInput;

	String sampleText;

	String regexManyAlternations;

	@Setup
	public void setup() throws Exception {
		redosInput = "a".repeat(30) + "!"; // for python it took ~28 seconds

		sampleText = new String(Re2Benchmark.class.getResourceAsStream("/3200.txt").readAllBytes(), StandardCharsets.UTF_8);

		var firstNames = new String(Re2Benchmark.class.getResourceAsStream("/first-names.txt").readAllBytes(), StandardCharsets.UTF_8);
		regexManyAlternations = Arrays.stream(firstNames.split("\r\n")).limit(100).collect(Collectors.joining("|", "(", ")"));
	}

	static class SmokeTest {

		public static void main(String[] args) throws Exception {
			Re2Benchmark benchmark = new Re2Benchmark();
			benchmark.setup();

			long startTime = System.currentTimeMillis();
			int matchCount = RegexEngine.JDK.countMatchesOfPatterInText(benchmark.regexManyAlternations, benchmark.sampleText);
			long endTime = System.currentTimeMillis();
			long duration = endTime - startTime;

			System.out.printf("count: %d, duration: %d ms\n", matchCount, duration);
		}

	}

	@Benchmark
	public void wholeWordsEndingWithNn_jdk(Blackhole bh) {
		bh.consume(RegexEngine.JDK.countMatchesOfPatterInText(REGEX_WHOLE_WORDS_ENDING_WITH_NN, sampleText));
	}

	@Benchmark
	public void wholeWordsEndingWithNn_re2(Blackhole bh) {
		bh.consume(RegexEngine.RE2.countMatchesOfPatterInText(REGEX_WHOLE_WORDS_ENDING_WITH_NN, sampleText));
	}

	@Benchmark
	public void redos_jdk(Blackhole bh) {
		bh.consume(RegexEngine.JDK.countMatchesOfPatterInText(REGEX_REDOS, redosInput));
	}

	@Benchmark
	public void redos_re2(Blackhole bh) {
		bh.consume(RegexEngine.RE2.countMatchesOfPatterInText(REGEX_REDOS, redosInput));
	}

	@Benchmark
	public void manyAlternation_jdk(Blackhole bh) {
		bh.consume(RegexEngine.JDK.countMatchesOfPatterInText(regexManyAlternations, sampleText));
	}

	@Benchmark
	public void manyAlternations_re2(Blackhole bh) {
		bh.consume(RegexEngine.RE2.countMatchesOfPatterInText(regexManyAlternations, sampleText));
	}

	@FunctionalInterface
	interface RegexEngine {

		int countMatchesOfPatterInText(String pattern, String text);

		RegexEngine JDK = (pattern, text) -> {
			java.util.regex.Pattern javaPattern = java.util.regex.Pattern.compile(pattern);
			Matcher javaMatcher = javaPattern.matcher(text);
			int matchCount = 0;
			while (javaMatcher.find()) {
				matchCount++;
			}
			return matchCount;
		};

		RegexEngine RE2 = (pattern, text) -> {
			com.google.re2j.Pattern re2jPattern;
			try {
				re2jPattern = com.google.re2j.Pattern.compile(pattern);
			} catch (PatternSyntaxException e) {
				throw new RuntimeException("Failed to compile RE2J pattern", e);
			}
			com.google.re2j.Matcher re2jMatcher = re2jPattern.matcher(text);
			int matchCount = 0;
			while (re2jMatcher.find()) {
				matchCount++;
			}
			return matchCount;
		};
	}

}
