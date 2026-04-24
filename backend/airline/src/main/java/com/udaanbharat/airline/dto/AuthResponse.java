package com.udaanbharat.airline.dto;
 
public class AuthResponse {
    private String token;
    private Integer userId;
    private String name;
    private String email;
 
    public AuthResponse(String token, Integer userId, String name, String email) {
        this.token = token;
        this.userId = userId;
        this.name = name;
        this.email = email;
    }
 
    public String getToken() { return token; }
    public Integer getUserId() { return userId; }
    public String getName() { return name; }
    public String getEmail() { return email; }
}
 