package com.example.vibefitapp;

import java.util.HashMap;
import java.util.Map;

public class CategoryUtil {
    public static final Map<String, String> displayToValueMap = new HashMap<String, String>() {{
        put("Tutorial", "tutorial");
        put("Pattern Guide", "pattern");
        put("Forum", "forum");
        put("Trends", "trends");
    }};

    public static final Map<String, String> valueToDisplayMap = new HashMap<String, String>() {{
        put("tutorial", "Tutorial");
        put("pattern", "Pattern Guide");
        put("forum", "Forum");
        put("trends", "Trends");
    }};
}

