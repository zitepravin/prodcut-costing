package com.automotive.costing.notification.service;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EmailTemplateRendererTest {

    private final EmailTemplateRenderer renderer = new EmailTemplateRenderer();

    @Test
    void replacesAllowedPlaceholders() {
        String out = renderer.render(
                "Hello {{userName}}, doc {{documentNumber}} status {{status}}",
                Map.of("userName", "Ada", "documentNumber", "PO-1", "status", "APPROVED")
        );
        assertEquals("Hello Ada, doc PO-1 status APPROVED", out);
    }

    @Test
    void unknownPlaceholderBecomesEmpty() {
        String out = renderer.render("Hi {{evilScript}}", Map.of("evilScript", "x"));
        assertEquals("Hi ", out);
        assertTrue(renderer.containsDisallowedPlaceholder("Hi {{evilScript}}"));
    }

    @Test
    void redactsSecretLookingValues() {
        assertEquals("[REDACTED]", EmailTemplateRenderer.sanitize("Bearer abc.def"));
        assertEquals("[REDACTED]", EmailTemplateRenderer.sanitize("password=secret"));
    }

    @Test
    void doesNotExecuteExpressions() {
        String out = renderer.render("{{userName}} ${system.exit}", Map.of("userName", "Bob"));
        assertEquals("Bob ${system.exit}", out);
    }
}
