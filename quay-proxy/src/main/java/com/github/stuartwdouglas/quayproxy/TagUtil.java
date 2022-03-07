package com.github.stuartwdouglas.quayproxy;

public class TagUtil {

    public static String tagName(String group, String artifact, String version) {
        String gavString = group + ":" + artifact + ":" + version;
        String tag = gavString;
        if (gavString.length() > 120) {
            tag = HashUtil.sha1(gavString);
        } else {
            tag = tag.replace(":","_");
        }
        return tag;
    }

}
