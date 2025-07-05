package com.keval.pods.marketplace.model;

import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;

@Entity   // Marks as Persistent Object for JPA
@Data     // Adds getters and setters for attributes
@Table(name = "Orders")
public class Order {
  @Id @GeneratedValue(strategy = GenerationType.AUTO)
  private Integer order_id;
  private Integer user_id;
  private Integer total_price;

  public static String STATUS_PLACED = "PLACED";
  public static String STATUS_CANCELLED = "CANCELLED";
  public static String STATUS_DELIVERED = "DELIVERED";

  private String status;

  // One-to-Many mapping from Order to OrderItems
  @OneToMany(cascade = CascadeType.ALL)
  // Specifies the foreign-key column name to make in OrderItem table
  @JoinColumn(name = "order_id")
  List<OrderItem> items;
}
