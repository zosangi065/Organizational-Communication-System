package com.ocs.util;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/** Parsing of URL-encoded form bodies and query strings. */
public final class FormParser {
    private FormParser() { }

    public static Map<String, String> form(HttpExchange ex) throws IOException {
        return parse(new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
    }

    public static Map<String, String> query(HttpExchange ex) {
        return parse(ex.getRequestURI().getRawQuery());
    }

    public static Map<String, String> parse(String s) {
        Map<String, String> map = new LinkedHashMap<>();
        if (s == null || s.isEmpty()) return map;
        for (String pair : s.split("&")) {
            int i = pair.indexOf('=');
            if (i < 0) continue;
            try {
                map.put(URLDecoder.decode(pair.substring(0, i), StandardCharsets.UTF_8),
                        URLDecoder.decode(pair.substring(i + 1), StandardCharsets.UTF_8));
            } catch (IllegalArgumentException ignored) { }
        }
        return map;
    }

    public static int toInt(String s, int def) {
        try { return Integer.parseInt(s.trim()); } catch (RuntimeException e) { return def; }
    }
}
