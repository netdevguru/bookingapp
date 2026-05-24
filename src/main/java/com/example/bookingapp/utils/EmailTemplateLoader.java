package com.example.bookingapp.utils;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
public class EmailTemplateLoader {

    /**
     * Load an email template from resources and replace placeholders
     * @param templateName Name of the template file (e.g., "verification-email.html")
     * @param placeholders Map of placeholder names to their values (e.g., "firstName" -> "John")
     * @return Processed HTML content with placeholders replaced
     */
    public String loadTemplate(String templateName, Map<String, String> placeholders) {
        try {
            ClassPathResource resource = new ClassPathResource("email-templates/" + templateName);
            String content = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            
            // Replace all placeholders
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                content = content.replace("{{" + entry.getKey() + "}}", entry.getValue());
            }
            
            return content;
        } catch (IOException e) {
            System.err.println("Error loading email template: " + templateName);
            e.printStackTrace();
            throw new RuntimeException("Failed to load email template: " + templateName, e);
        }
    }
}
