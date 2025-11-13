package com.hungersaviour.restaurant.repository;

import com.hungersaviour.restaurant.model.Restaurant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {
    List<Restaurant> findByOwnerId(Long ownerId);
    List<Restaurant> findByIsActiveTrue();
    List<Restaurant> findByCuisineContainingIgnoreCase(String cuisine);
}
