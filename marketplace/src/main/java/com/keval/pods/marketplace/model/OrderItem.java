package com.keval.pods.marketplace.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;

@Entity   // Marks as Persistent Object for JPA
@Data     // Adds getters and setters for attributes
public class OrderItem {
  @Id @GeneratedValue(strategy = GenerationType.AUTO)
  private Integer id;
  private Integer product_id;
  private Integer quantity;
}
