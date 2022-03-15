package com.github.stuartwouglas.repoexplorer.utils;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PropertyReplacer {

    public static final Pattern PATTERN = Pattern.compile("\\$\\{(a:.*?)}");

    public static final String MISSING = "<<MISSING>>";
    public static String replace(String original, Map<String, String> properties) {
        Matcher matcher = PATTERN.matcher(original);
        if (!matcher.find()) {
            return original;
        }
        matcher.reset();
        return matcher.replaceAll(s -> {
            String result = properties.get(s.group(1));
            if (result == null) {
                return "MISSING";
            }
            return Pattern.quote(replace(result, properties));
        });
    }

}
