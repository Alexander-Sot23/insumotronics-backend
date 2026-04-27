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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;

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

            if (authentication.isAuthenticated()) {
                // Generar token con el code
                String token = jwtService.generateToken(adminUser);

                // Crear respuesta JSON con el token
                Map<String, Object> response = new HashMap<>();
                response.put("token", token);
                response.put("message", "Login successful");
                response.put("code", adminUser.getCode());
                response.put("name", adminUser.getName() + " " +  adminUser.getLastname());
                response.put("email", adminUser.getEmail());
                response.put("userId", adminUser.getId());
                response.put("role", adminUser.getRole());
                response.put("status", adminUser.getStatus());
                response.put("firstLogin", adminUser.getFirstLogin());
                response.put("lastLogin", adminUser.getLastLogin());

                if(adminUser.getFirstLogin()==null){
                    adminUser.setFirstLogin(LocalDateTime.now());
                    adminUser.setLastLogin(LocalDateTime.now());
                }else{
                    adminUser.setLastLogin(LocalDateTime.now());
                }
                myUserRepository.save(adminUser);

                return ResponseEntity.ok(response);
            } else {
                throw new UsernameNotFoundException("Credenciales invalidas.");
            }

        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Authentication failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
    }

}

