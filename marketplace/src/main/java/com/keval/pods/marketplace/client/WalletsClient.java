package com.keval.pods.marketplace.client;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import com.keval.pods.marketplace.client.model.WalletTrxn;

import lombok.extern.apachecommons.CommonsLog;

@CommonsLog
public class WalletsClient {
  /**
   * Send transaction request to wallet by userId
   * <p> Returns 200 OK if successful, 400 BAD_REQEUST if insufficient funds </p>
   */
  public HttpStatus trxnByUserId(String walletsUrl, Integer userId, String trxnType, Integer amount) {
    RestClient restClient = RestClient.create();

    WalletTrxn walletTrxn = new WalletTrxn();
    walletTrxn.setAction(trxnType);
    walletTrxn.setAmount(amount);

    try {
      restClient.put()
        .uri(walletsUrl + "/wallets/{userId}", userId)
        .contentType(MediaType.APPLICATION_JSON)
        .body(walletTrxn)
        .retrieve()
        .body(String.class);
      
      log.debug("Successful Transaction: " + userId + walletTrxn.toString());
      
      return HttpStatus.OK;
    }
    // 4xx Error -> 400 BAD_REQUEST
    // Caused due to insufficient funds when trying to debit
    catch (RestClientResponseException e) {
      log.debug(e, e);
      return HttpStatus.BAD_REQUEST;
    }
    // Any other error, return 500 INTERNAL_SERVER_ERROR
    catch (Exception e) {
      log.error(e, e);
      return HttpStatus.INTERNAL_SERVER_ERROR;
    }
  }
}
