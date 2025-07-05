package com.keval.pods.marketplace.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.keval.pods.marketplace.model.Order;

@Repository
public interface OrderRepository extends JpaRepository<Order, Integer> {
  // Query order by user_id
  @Query("SELECT o FROM Order o WHERE o.user_id = :user_id")
  List<Order> findByUserId(Integer user_id);
  
  // Query order by id
  Optional<Order> findById(Integer id);

  // Query to cancel orders by user_id and status
  // Query order by user_id and status, if status is PLACED make it CANCELLED
  // Return the number of records updated
  @Modifying
  @Transactional
  @Query("UPDATE Order o SET o.status = :cancelledStatus WHERE o.user_id = :user_id AND o.status = :placedStatus")
  int cancelOrdersByUserId(@Param("user_id") Integer user_id, 
                            @Param("placedStatus") String placedStatus, 
                            @Param("cancelledStatus") String cancelledStatus);

  // Query to cancel all orders in PLACED status
  // Query order by status, if status is PLACED make it CANCELLED
  // Return the number of records updated
  @Modifying
  @Transactional
  @Query("UPDATE Order o SET o.status = :cancelledStatus WHERE o.status = :placedStatus")
  int cancelAllPlacedOrders(@Param("placedStatus") String placedStatus, 
                            @Param("cancelledStatus") String cancelledStatus);

  // Query to find all orders in PLACED status
  // Query order by status
  @Query("SELECT o FROM Order o WHERE o.status = :placedStatus")
  List<Order> findAllPlacedOrders(@Param("placedStatus") String placedStatus);

}
  
