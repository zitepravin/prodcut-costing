package com.automotive.costing.notification.service;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Wave F — safe placeholder substitution only.
 * Supports {{key}} tokens. Never evaluates expressions or executes code.
 * Unknown placeholders are left blank (not errors) after validation of allowed keys.
 */
@Component
public class EmailTemplateRenderer {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{\\s*([a-zA-Z][a-zA-Z0-9_]*)\\s*\\}\\}");

    /** Allowed placeholder names from master prompt. */
    public static final java.util.Set<String> ALLOWED = java.util.Set.of(
            "userName", "customerName", "documentNumber", "supplierName",
            "amount", "currency", "dueDate", "status", "reason", "approvalUrl"
    );

    public String render(String template, Map<String, String> values) {
        if (template == null || template.isEmpty()) {
            return "";
        }
        Matcher m = PLACEHOLDER.matcher(template);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String key = m.group(1);
            String replacement = "";
            if (ALLOWED.contains(key) && values != null && values.get(key) != null) {
                replacement = sanitize(values.get(key));
            }
            m.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    /** Reject secrets-looking values from being injected. */
    static String sanitize(String raw) {
        if (raw == null) {
            return "";
        }
        String v = raw.trim();
        String lower = v.toLowerCase();
        if (lower.contains("bearer ") || lower.contains("password")
                || lower.contains("jwt") || lower.startsWith("eyj")) {
            return "[REDACTED]";
        }
        if (v.length() > 500) {
            return v.substring(0, 500);
        }
        return v;
    }

    public boolean containsDisallowedPlaceholder(String template) {
        if (template == null) {
            return false;
        }
        Matcher m = PLACEHOLDER.matcher(template);
        while (m.find()) {
            if (!ALLOWED.contains(m.group(1))) {
                return true;
            }
        }
        return false;
    }
}
