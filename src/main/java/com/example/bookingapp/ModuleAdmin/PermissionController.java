package com.example.bookingapp.ModuleAdmin;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/permissions")
public class PermissionController {

    @GetMapping("/metadata")
    public ResponseEntity<Map<String, Object>> getPermissionMetadata() {
        Map<String, Object> metadata = new HashMap<>();
        
        // Available roles
        List<Map<String, String>> roles = new ArrayList<>();
        roles.add(createRole("ADMIN", "Administrator", "Full system access"));
        roles.add(createRole("MANAGER", "Manager", "Manage users and content"));
        roles.add(createRole("SUPPORT", "Support", "Customer support access"));
        roles.add(createRole("CUSTOMER", "Customer", "Standard user access"));
        
        metadata.put("roles", roles);
        
        // Permissions by role
        Map<String, List<String>> permissions = new HashMap<>();
        permissions.put("ADMIN", Arrays.asList(
            "user.create", "user.read", "user.update", "user.delete",
            "role.assign", "system.configure", "reports.view", "all.access"
        ));
        permissions.put("MANAGER", Arrays.asList(
            "user.read", "user.update", "content.manage", "reports.view"
        ));
        permissions.put("SUPPORT", Arrays.asList(
            "user.read", "ticket.manage", "chat.access"
        ));
        permissions.put("CUSTOMER", Arrays.asList(
            "profile.read", "profile.update", "content.view"
        ));
        
        metadata.put("permissions", permissions);
        
        return ResponseEntity.ok(metadata);
    }

    @GetMapping("/roles")
    public ResponseEntity<List<Map<String, String>>> getRoles() {
        List<Map<String, String>> roles = new ArrayList<>();
        roles.add(createRole("ADMIN", "Administrator", "Full system access"));
        roles.add(createRole("MANAGER", "Manager", "Manage users and content"));
        roles.add(createRole("SUPPORT", "Support", "Customer support access"));
        roles.add(createRole("CUSTOMER", "Customer", "Standard user access"));
        
        return ResponseEntity.ok(roles);
    }

    private Map<String, String> createRole(String value, String label, String description) {
        Map<String, String> role = new HashMap<>();
        role.put("value", value);
        role.put("label", label);
        role.put("description", description);
        return role;
    }
}
