package com.keval.pods.marketplace.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.keval.pods.marketplace.model.Product;


@Repository
public interface ProductRepository extends JpaRepository<Product, Integer> {
  // Query all products
  List<Product> findAll();

  // Query product by id
  Optional<Product> findById(Integer id);

  // Decrease product stock_quantity by quantity
  @Modifying
  @Transactional
  @Query("UPDATE Product SET stock_quantity = stock_quantity - :quantity WHERE id = :id AND stock_quantity >= :quantity")
  int decreaseProductStock_quantityById(Integer id, Integer quantity);

  // Increase product stock_quantity by quantity
  @Modifying
  @Transactional
  @Query("UPDATE Product SET stock_quantity = stock_quantity + :quantity WHERE id = :id")
  void increaseProductStock_quantityById(Integer id, Integer quantity);
}
