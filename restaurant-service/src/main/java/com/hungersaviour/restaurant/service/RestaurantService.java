package com.hungersaviour.restaurant.service;

import com.hungersaviour.restaurant.model.MenuItem;
import com.hungersaviour.restaurant.model.Restaurant;
import com.hungersaviour.restaurant.repository.MenuItemRepository;
import com.hungersaviour.restaurant.repository.RestaurantRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RestaurantService {

    @Autowired
    private RestaurantRepository restaurantRepository;

    @Autowired
    private MenuItemRepository menuItemRepository;

    @CacheEvict(value = "restaurants", allEntries = true)
    public Restaurant createRestaurant(Restaurant restaurant) {
        return restaurantRepository.save(restaurant);
    }

    @Cacheable(value = "restaurants", key = "#id")
    public Restaurant getRestaurantById(Long id) {
        return restaurantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Restaurant not found"));
    }

    @Cacheable(value = "restaurants", key = "'all'")
    public List<Restaurant> getAllRestaurants() {
        return restaurantRepository.findByIsActiveTrue();
    }

    @Cacheable(value = "restaurants", key = "'owner-' + #ownerId")
    public List<Restaurant> getRestaurantsByOwner(Long ownerId) {
        return restaurantRepository.findByOwnerId(ownerId);
    }

    @CacheEvict(value = "restaurants", allEntries = true)
    public Restaurant updateRestaurant(Long id, Restaurant restaurant) {
        Restaurant existing = getRestaurantById(id);
        existing.setName(restaurant.getName());
        existing.setAddress(restaurant.getAddress());
        existing.setCuisine(restaurant.getCuisine());
        existing.setDescription(restaurant.getDescription());
        existing.setPhoneNumber(restaurant.getPhoneNumber());
        return restaurantRepository.save(existing);
    }

    @CacheEvict(value = "restaurants", allEntries = true)
    public void deleteRestaurant(Long id) {
        Restaurant restaurant = getRestaurantById(id);
        restaurant.setIsActive(false);
        restaurantRepository.save(restaurant);
    }

    @CacheEvict(value = "menuItems", allEntries = true)
    public MenuItem addMenuItem(Long restaurantId, MenuItem menuItem) {
        Restaurant restaurant = getRestaurantById(restaurantId);
        menuItem.setRestaurant(restaurant);
        return menuItemRepository.save(menuItem);
    }

    @Cacheable(value = "menuItems", key = "#restaurantId")
    public List<MenuItem> getMenuItems(Long restaurantId) {
        return menuItemRepository.findByRestaurantIdAndIsAvailableTrue(restaurantId);
    }

    @CacheEvict(value = "menuItems", allEntries = true)
    public MenuItem updateMenuItem(Long id, MenuItem menuItem) {
        MenuItem existing = menuItemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Menu item not found"));
        existing.setName(menuItem.getName());
        existing.setDescription(menuItem.getDescription());
        existing.setPrice(menuItem.getPrice());
        existing.setCategory(menuItem.getCategory());
        existing.setIsAvailable(menuItem.getIsAvailable());
        return menuItemRepository.save(existing);
    }

    @CacheEvict(value = "menuItems", allEntries = true)
    public void deleteMenuItem(Long id) {
        MenuItem menuItem = menuItemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Menu item not found"));
        menuItem.setIsAvailable(false);
        menuItemRepository.save(menuItem);
    }
}
