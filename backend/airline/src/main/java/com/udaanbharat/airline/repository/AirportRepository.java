package com.udaanbharat.airline.repository;
 
import com.udaanbharat.airline.entity.Airport;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
 
public interface AirportRepository extends JpaRepository<Airport, Integer> {
    Optional<Airport> findByCode(String code);
}