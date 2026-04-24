package com.udaanbharat.airline.repository;
 
import com.udaanbharat.airline.entity.Passenger;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
 
public interface PassengerRepository extends JpaRepository<Passenger, Integer> {
    Optional<Passenger> findByEmail(String email);
    boolean existsByEmail(String email);
}