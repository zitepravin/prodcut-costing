package com.automotive.costing.notification.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.notification")
public class NotificationProperties {

    /** When true, outbound email is attempted via JavaMailSender. */
    private boolean emailEnabled = false;

    private String mailFrom = "noreply@product-costing.local";

    private String mailFromName = "Product Costing";
}
