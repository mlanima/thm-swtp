package de.thm.swtp.api.project;

import java.util.Map;
import java.util.regex.Pattern;

public final class ProjectUrlUtils {

    public static final Pattern URL_PATTERN = Pattern.compile("^[a-z0-9]+(?:-[a-z0-9]+)*$");
    static final int MIN_URL_LENGTH = 3;
    static final int MAX_URL_LENGTH = 30;

    private static final Map<Character, String> UMLAUT_MAP = Map.of(
            'ä', "ae", 'ö', "oe", 'ü', "ue", 'ß', "ss",
            'Ä', "ae", 'Ö', "oe", 'Ü', "ue"
    );

    private ProjectUrlUtils() {}

    public static String generateSlug(String name) {
        if (name == null || name.isBlank()) {
            return "";
        }

        StringBuilder expanded = new StringBuilder();
        for (char c : name.toCharArray()) {
            expanded.append(UMLAUT_MAP.getOrDefault(c, String.valueOf(c)));
        }

        String slug = expanded.toString()
                .toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .trim()
                .replaceAll("[\\s_]+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-+|-+$", "");

        if (slug.length() > MAX_URL_LENGTH) {
            slug = slug.substring(0, MAX_URL_LENGTH).replaceAll("-+$", "");
        }

        return slug;
    }

    public static boolean isValidUrl(String url) {
        if (url == null || url.length() < MIN_URL_LENGTH || url.length() > MAX_URL_LENGTH) {
            return false;
        }
        return URL_PATTERN.matcher(url).matches();
    }
}