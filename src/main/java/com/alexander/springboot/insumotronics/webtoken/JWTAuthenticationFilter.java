package com.alexander.springboot.insumotronics.webtoken;

import com.alexander.springboot.insumotronics.service.details.MyUserDetailsService;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Configuration
public class JWTAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private MyUserDetailsService myUserDetailsService;

    @Autowired
    private JWTService jwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        if(authHeader==null || !authHeader.startsWith("Bearer ")){
            filterChain.doFilter(request,response);
            return;
        }

        String jwt = authHeader.substring(7);
        String userName = null;

        try {
            userName = jwtService.extractUserName(jwt);
        } catch (ExpiredJwtException e) {
            // JWT expirado - devolver 401 con mensaje
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");

            String jsonResponse = "{\"error\":\"JWT token has expired\",\"type\":\"TOKEN_EXPIRED\"}";
            response.getWriter().write(jsonResponse);
            return;
        } catch (Exception e) {
            // Otras excepciones de JWT - continuar sin autenticar
            filterChain.doFilter(request, response);
            return;
        }

        if(userName!=null && SecurityContextHolder.getContext().getAuthentication() == null){
            try {
                UserDetails userDetails = myUserDetailsService.loadUserByUsername(userName);
                if(userDetails != null && jwtService.isTokenValid(jwt)){
                    UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                            userDetails,  //MyUserDetails completo
                            null,  //null porque ya verificamos el JWT
                            userDetails.getAuthorities()
                    );
                    authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                }
            } catch (ExpiredJwtException e) {
                // JWT expirado durante validación - devolver 401
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");

                String jsonResponse = "{\"error\":\"JWT token has expired\",\"type\":\"TOKEN_EXPIRED\"}";
                response.getWriter().write(jsonResponse);
                return;
            }
        }
        filterChain.doFilter(request,response);
    }
}

