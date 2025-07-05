package com.keval.pods.marketplace.model;

import com.opencsv.bean.CsvBindByName;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

@Entity   // Marks as Persistent Object for JPA
@Data     // Adds getters and setters for attributes
public class Product {
  @Id @CsvBindByName
  private Integer id;

  @CsvBindByName
  private String name;

  @CsvBindByName
  private String description;

  @CsvBindByName
  private Integer price;

  @CsvBindByName
  private Integer stock_quantity;
}
