package com.udaanbharat.airline.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "airport")
public class Airport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "airport_id")
    private Integer airportId;

    @Column(name = "name", length = 255)
    private String name;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "code", unique = true, length = 10)
    private String code;

    // Getters and Setters
    public Integer getAirportId() { return airportId; }
    public void setAirportId(Integer airportId) { this.airportId = airportId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
}