package com.keval.pods.marketplace.client;

import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClient;

import com.keval.pods.marketplace.client.model.User;

import lombok.extern.apachecommons.CommonsLog;

@CommonsLog
public class UsersClient {
  /**
   * Obtain user by user_id
   * <p> User model only contains id and discount_availed </p>
   */
  public Optional<User> getUserById(String usersUrl, Integer userId) {
    RestClient restClient = RestClient.create();

    try {
      User user = restClient.get()
        .uri(usersUrl + "/users/{userId}", userId)
        .retrieve()
        .body(User.class);
      
      log.debug("Retrieved User: " + user.toString());
      
      return Optional.ofNullable(user);
    }
    catch (Exception e) {
      log.debug(e, e);
      return Optional.empty();
    }
  }

  /**
   * Set discount_availed to be true for user with user_id
   */
  public HttpStatus setDiscount_availedById(String usersUrl, Integer userId) {
    RestClient restClient = RestClient.create();
    
    User user = new User();
    user.setId(userId);
    user.setDiscount_availed(true);

    try {
      restClient.put()
        .uri(usersUrl + "/users/{userId}", userId)
        .body(user)
        .retrieve()
        .body(String.class);

      log.debug("Successfully set discount_availed=true for " + userId);
      
      return HttpStatus.OK;
    }
    catch (Exception e) {
      log.debug(e, e);
      return HttpStatus.BAD_REQUEST;
    }
  }
}
