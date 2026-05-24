package com.example.bookingapp.config;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.crypto.SecretKey;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

public class JwtProvider {
    static SecretKey key=Keys.hmacShaKeyFor(JwtConstant.getSecretKey().getBytes());

    public static String generateToken(Authentication auth) {

        Collection<?extends GrantedAuthority> authorities = auth.getAuthorities();
        String roles = populateAuthorities(authorities);

        return Jwts.builder()
                .issuedAt(new Date())
                .expiration(new Date(new Date().getTime()+86400000))
                .claim("email",auth.getName())
                .claim("authorities", roles)
                .signWith(key)
                .compact();
    }
    
    public static String generateToken(Authentication auth, Long userId) {

        Collection<?extends GrantedAuthority> authorities = auth.getAuthorities();
        String roles = populateAuthorities(authorities);

        return Jwts.builder()
                .issuedAt(new Date())
                .expiration(new Date(new Date().getTime()+86400000))
                .claim("email",auth.getName())
                .claim("authorities", roles)
                .claim("userId", userId)
                .signWith(key)
                .compact();
    }

    public static String getEmailFromJwtToken(String jwt) {
        jwt=jwt.substring(7);
        Claims claims= Jwts.parser().verifyWith(key).build().parseSignedClaims(jwt).getPayload();
        return String.valueOf(claims.get("email"));
    }

    public static String populateAuthorities(Collection<?extends GrantedAuthority> collection) {
        Set<String> auths=new HashSet<>();
        for(GrantedAuthority authority:collection) {
            auths.add(authority.getAuthority());
        }
        //	"customer,admin,super-admin"
        return String.join(",", auths);
    }
}
