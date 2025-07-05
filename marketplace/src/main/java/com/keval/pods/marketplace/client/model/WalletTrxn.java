package com.keval.pods.marketplace.client.model;

import lombok.Data;

@Data     // Adds getters and setters for attributes
public class WalletTrxn {
  public static String DEBIT = "debit";
  public static String CREDIT = "credit";

  private String action;
  private Integer amount;
}
