package dev.snowdrop.mtool.validate.ui;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

final class Patterns {

    private Patterns() {
    }

    static List<Pattern> caseInsensitive(String... regexes) {
        return Arrays.stream(regexes).map(r -> Pattern.compile(r, Pattern.CASE_INSENSITIVE)).toList();
    }
}
