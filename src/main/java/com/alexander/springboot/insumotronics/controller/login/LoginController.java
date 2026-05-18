package com.alexander.springboot.insumotronics.controller.login;

/*
 * Este controlador funciona para todos aquellos usuarios que no esten logeados
 */

import com.alexander.springboot.insumotronics.exception.MyUserNotFoundException;
import com.alexander.springboot.insumotronics.model.LoginFormM;
import com.alexander.springboot.insumotronics.repository.MyUserRepository;
import com.alexander.springboot.insumotronics.webtoken.JWTService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseCookie;
import org.springframework.http.HttpHeaders;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@RequestMapping("/api")
@RestController
public class LoginController {

    @Autowired
    private Validator validator;

    @Autowired
    private MyUserRepository myUserRepository;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JWTService jwtService;

    private final Logger log = LoggerFactory.getLogger(LoginController.class);

    @PostMapping("/login")
    public ResponseEntity<?> authenticateAndGetToken(@RequestPart(value = "sendData") String sendData) {

        ObjectMapper objectMapper = new ObjectMapper();
        LoginFormM loginFormModel;

        try {
            loginFormModel = objectMapper.readValue(sendData, LoginFormM.class);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Invalid JSON format: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }

        // Validación manual usando el validador de SPRING
        Set<ConstraintViolation<LoginFormM>> violations = validator.validate(loginFormModel);
        if (!violations.isEmpty()) {
            Map<String, String> errors = new HashMap<>();
            for (ConstraintViolation<LoginFormM> violation : violations) {
                errors.put(violation.getPropertyPath().toString(), violation.getMessage());
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
        }

        try {
            String loginInput = loginFormModel.code();

            var adminUser = myUserRepository.findByCode(loginInput)
                    .orElseThrow(() -> new MyUserNotFoundException("User with code:" + loginInput + ", not found."));

            Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                    adminUser.getCode(),
                    loginFormModel.password()
            ));

            log.info("Usuario {} intentando iniciar sesion", adminUser.getCode());

            if (authentication.isAuthenticated()) {
                // Generar token con el code
                String token = jwtService.generateToken(adminUser);

                // Crear cookie HttpOnly
                ResponseCookie cookie = ResponseCookie.from("jwt", token)
                        .httpOnly(true)
                        .secure(true)
                        .path("/")
                        .maxAge(24 * 60 * 60) // 1 día de vida
                        .sameSite("none")
                        .build();

                // Crear respuesta JSON sin el token
                Map<String, Object> response = new HashMap<>();
                response.put("message", "Login successful");
                response.put("name", adminUser.getName() + " " +  adminUser.getLastname());
                response.put("userId", adminUser.getId());
                response.put("role", adminUser.getRole());
                response.put("code", adminUser.getCode());

                if(adminUser.getFirstLogin()==null){
                    adminUser.setFirstLogin(LocalDateTime.now());
                    adminUser.setLastLogin(LocalDateTime.now());
                }else{
                    adminUser.setLastLogin(LocalDateTime.now());
                }
                myUserRepository.save(adminUser);

                log.info("Usuario {} loggeado con exito", adminUser.getCode());

                return ResponseEntity.ok()
                        .header(HttpHeaders.SET_COOKIE, cookie.toString())
                        .body(response);
            } else {
                log.warn("Usuario {} intentando iniciar sesion", adminUser.getCode());
                throw new UsernameNotFoundException("Credenciales invalidas.");
            }

        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Authentication failed: " + e.getMessage());
            log.error("Error al iniciar sesion {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        ResponseCookie cookie = ResponseCookie.from("jwt", "")
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(0) // Expira inmediatamente
                .sameSite("Lax")
                .build();

        Map<String, String> response = new HashMap<>();
        response.put("message", "Logout successful");

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(response);
    }

    @GetMapping("/auth/me")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "No autenticado"));
        }

        String userCode = authentication.getName();
        var user = myUserRepository.findByCode(userCode)
                .orElseThrow(() -> new MyUserNotFoundException("Usuario no encontrado"));

        // Devolvemos la misma estructura que en el login
        Map<String, Object> response = new HashMap<>();
        response.put("name", user.getName() + " " + user.getLastname());
        response.put("userId", user.getId());
        response.put("role", user.getRole());
        response.put("code", user.getCode());

        return ResponseEntity.ok(response);
    }

}
