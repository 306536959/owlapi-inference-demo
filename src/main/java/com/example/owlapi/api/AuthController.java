package com.example.owlapi.api;

import com.example.owlapi.config.SystemBuiltinProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

        String expectedUser = props.getGraphDb().getUsername() == null ? "" : props.getGraphDb().getUsername();
        String expectedPass = props.getGraphDb().getPassword() == null ? "" : props.getGraphDb().getPassword();

        Map<String, Object> response = new HashMap<>();
        if (expectedUser.equals(username) && expectedPass.equals(password)) {
            session.setAttribute(SESSION_AUTH, true);
            session.setAttribute(SESSION_MODE, "auth");
            session.setAttribute(SESSION_USER, username);
            response.put("success", true);
            response.put("message", "Login successful");
            return ResponseEntity.ok(response);
        }

        response.put("success", false);
        response.put("message", "用户名或密码错误");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/no-auth")
    public ResponseEntity<Map<String, Object>> noAuth(HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        session.setAttribute(SESSION_AUTH, true);
        session.setAttribute(SESSION_MODE, "no-auth");
        session.setAttribute(SESSION_USER, "guest");
        response.put("success", true);
        response.put("message", "No-auth login successful");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status(HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        boolean authenticated = Boolean.TRUE.equals(session.getAttribute(SESSION_AUTH));
        response.put("authenticated", authenticated);
        response.put("mode", session.getAttribute(SESSION_MODE));
        response.put("user", session.getAttribute(SESSION_USER));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        session.invalidate();
        response.put("success", true);
        return ResponseEntity.ok(response);
    }
}
