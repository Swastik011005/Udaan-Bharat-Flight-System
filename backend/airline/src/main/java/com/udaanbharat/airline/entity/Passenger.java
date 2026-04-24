package com.udaanbharat.airline.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "passenger")
public class Passenger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Passenger_ID")
    private Integer passengerId;

    @Column(name = "Name", length = 100)
    private String name;

    @Column(name = "Age")
    private Integer age;

    @Column(name = "Gender", length = 10)
    private String gender;

    @Column(name = "Phone", length = 15)
    private String phone;

    @Column(name = "Email", unique = true, length = 100)
    private String email;

    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    // Getters and Setters
    public Integer getPassengerId() { return passengerId; }
    public void setPassengerId(Integer passengerId) { this.passengerId = passengerId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
}