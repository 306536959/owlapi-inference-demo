package com.example.owlapi.api;

import com.example.owlapi.config.SystemBuiltinProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final String SESSION_AUTH = "AUTHENTICATED";
    private static final String SESSION_MODE = "AUTH_MODE";
    private static final String SESSION_USER = "AUTH_USER";

    private final SystemBuiltinProperties props;

    public AuthController(SystemBuiltinProperties props) {
        this.props = props;
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> request, HttpSession session) {
        String username = request.getOrDefault("username", "").trim();
        String password = request.getOrDefault("password", "");

        Map<String, Object> response = new HashMap<>();
        if (verifyWithGraphDb(username, password)) {
            session.setAttribute(SESSION_AUTH, true);
            session.setAttribute(SESSION_MODE, "auth");
            session.setAttribute(SESSION_USER, username);
            response.put("success", true);
            response.put("message", "Login successful");
            return ResponseEntity.ok(response);
        }

        response.put("success", false);
        response.put("message", "GraphDB 鉴权失败，请检查用户名或密码");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status(HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        boolean authenticated = Boolean.TRUE.equals(session.getAttribute(SESSION_AUTH));
        response.put("authenticated", authenticated);
        response.put("mode", session.getAttribute(SESSION_MODE));
        response.put("user", session.getAttribute(SESSION_USER));
        response.put("graphDbAuthRequired", isGraphDbAuthRequired());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        session.invalidate();
        response.put("success", true);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/no-auth")
    public ResponseEntity<Map<String, Object>> noAuth(HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        if (isGraphDbAuthRequired()) {
            response.put("success", false);
            response.put("message", "GraphDB 已开启鉴权，不能无鉴权进入");
            return ResponseEntity.ok(response);
        }
        session.setAttribute(SESSION_AUTH, true);
        session.setAttribute(SESSION_MODE, "no-auth");
        session.setAttribute(SESSION_USER, "guest");
        response.put("success", true);
        response.put("message", "GraphDB 未开启鉴权，已直接进入");
        return ResponseEntity.ok(response);
    }

    private boolean verifyWithGraphDb(String username, String password) {
        try {
            String graphDbUrl = props.getGraphDb().getUrl();
            RestTemplate restTemplate = new RestTemplate();
            if (username != null && !username.trim().isEmpty()) {
                restTemplate.getInterceptors().add((request, body, execution) -> {
                    request.getHeaders().setBasicAuth(username, password == null ? "" : password);
                    return execution.execute(request, body);
                });
            }
            // Use a lightweight endpoint protected by GraphDB auth.
            restTemplate.getForEntity(graphDbUrl + "/rest/locations", String.class);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isGraphDbAuthRequired() {
        try {
            String graphDbUrl = props.getGraphDb().getUrl();
            RestTemplate restTemplate = new RestTemplate();
            restTemplate.getForEntity(graphDbUrl + "/rest/locations", String.class);
            return false;
        } catch (org.springframework.web.client.HttpClientErrorException.Unauthorized e) {
            return true;
        } catch (org.springframework.web.client.HttpClientErrorException.Forbidden e) {
            return true;
        } catch (Exception e) {
            // Unknown/unreachable: default to requiring auth for safety.
            return true;
        }
    }
}
