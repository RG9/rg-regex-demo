package dev.rg9;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.util.List;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class JavaRegexJdk20NamedCapturingGroupsTest {

	@ParameterizedTest
	@CsvSource(textBlock = """
		Id: 123 | 123 | # happy path
		Id: 1 | 1 | # shorter id
		Id: . | | # missing id
		Id: unknown | | # text instead of number
		Id  123 | 123 | # space instead of ':'
		ID: 123 | 123 | # upper case
		Id:123 | 123 | #missing separating space
		OtherId: 123 |  |
		""", delimiterString = "|")
	void matchId(String input, String expectedId) {
		matchIdWithGroupJava11(input, expectedId);
		matchIdWithLookBehind(input, expectedId);
		matchIdWithGroupJava20(input, expectedId);
	}

	private static void matchIdWithGroupJava11(String input, String expectedId) {
		var pattern = Pattern.compile("(?i)\\bId[: ]*?(\\d+)");
		var matcher = pattern.matcher(input);

		var id = matcher.results().findFirst().map(m -> m.group(1)).orElse(null);
		assertThat(id).isEqualTo(expectedId);
	}

	private static void matchIdWithGroupJava20(String input, String expectedId) {
		var pattern = Pattern.compile("(?i)\\bId[: ]*?(?<id>\\d+)");
		var matcher = pattern.matcher(input);

		var id = matcher.results().findFirst().map(m -> m.group("id")).orElse(null);
		assertThat(id).isEqualTo(expectedId);
	}

	private static void matchIdWithLookBehind(String input, String expectedId) {
		var pattern = Pattern.compile("(?i)(?<=\\bId[: ]{0,10})\\d+");
		var matcher = pattern.matcher(input);

		var id = matcher.results().findFirst().map(MatchResult::group).orElse(null);
		assertThat(id).isEqualTo(expectedId);
	}

	@Test
	void matchDifferentTypesOfIds() {
		String text = """
			Some text. Tax Id: 123. Some text.
			Some text, Court Id: 456, Stats Id: 789.
			""";
		List<IdEntry> idEntries = matchIdsWithGroupJava11(text);
		List<IdEntry> idEntries20 = matchIdsWithGroupJava20(text);

		assertThat(idEntries).isEqualTo(idEntries20);

		assertThat(idEntries)
			.extracting(IdEntry::type, IdEntry::id)
			.containsExactly(tuple("Tax Id", "123"),
				tuple("Court Id", "456"),
				tuple("Stats Id", "789"));
	}

	@Test
	void jdk20FeaturesOfNamedGroups() {
		String text = """
			Some text. Tax Id: 123. Some text.
			Some text, Court Id: 456, Stats Id: 789.
			""";

		var pattern = Pattern.compile("(?i)(?<type>\\w+ *Id)[: ]*?(?<id>\\d+)");
		var matcher = pattern.matcher(text);

		assertThat(matcher.results())
			.extracting(m -> m.group("type"), m -> m.group("id"), m -> m.start("type"), m -> m.end("id"))
			.containsExactly(tuple("Tax Id", "123", 11, 22),
				tuple("Court Id", "456", 46, 59),
				tuple("Stats Id", "789", 61, 74));

		assertThat(pattern.namedGroups())
			.isEqualTo(matcher.namedGroups());
		assertThat(pattern.namedGroups())
			.asString()
			.isEqualTo("{id=2, type=1}");

	}

	private static List<IdEntry> matchIdsWithGroupJava11(String input) {
		var pattern = Pattern.compile("(?i)(?<type>\\w+ *Id)[: ]*?(?<id>\\d+)");
		var matcher = pattern.matcher(input);

		return matcher.results().map(m -> new IdEntry(m.group(1), m.group(2))).toList();
	}

	private static List<IdEntry> matchIdsWithGroupJava20(String input) {
		var pattern = Pattern.compile("(?i)(?<type>\\w+ *Id)[: ]*?(?<id>\\d+)");
		var matcher = pattern.matcher(input);

		return matcher.results().map(m -> new IdEntry(m.group("type"), m.group("id"))).toList();
	}

	record IdEntry(String type, String id) {
	}

}
