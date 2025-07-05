package com.keval.pods.marketplace.client.model;

import lombok.Data;

@Data     // Adds getters and setters for attributes
public class User {
  private Integer id;
  private Boolean discount_availed;
}
