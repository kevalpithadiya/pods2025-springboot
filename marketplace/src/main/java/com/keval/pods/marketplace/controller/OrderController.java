package com.keval.pods.marketplace.controller;

import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.keval.pods.marketplace.client.UsersClient;
import com.keval.pods.marketplace.client.WalletsClient;
import com.keval.pods.marketplace.client.model.User;
import com.keval.pods.marketplace.client.model.WalletTrxn;
import com.keval.pods.marketplace.model.Order;
import com.keval.pods.marketplace.model.OrderItem;
import com.keval.pods.marketplace.model.Product;
import com.keval.pods.marketplace.repository.OrderRepository;
import com.keval.pods.marketplace.repository.ProductRepository;


import lombok.extern.apachecommons.CommonsLog;

@RestController
@CommonsLog
public class OrderController {
  // Obtain the order repository
  @Autowired
  private OrderRepository orderRepository;

  // Obtain the product repository
  @Autowired
  private ProductRepository productRepository;

  // Read users and wallets service URLs from configuration files
  @Value("${pods.usersUrl}")
  private String usersUrl;
  @Value("${pods.walletsUrl}")
  private String walletsUrl;

  // Clients for the users and wallets services
  private UsersClient usersClient = new UsersClient();
  private WalletsClient walletsClient = new WalletsClient();

  /* Endpoint 0: GET /hello
   * Testing endpoint
   * Returns the configured marketplace and wallets service URLs
  */
  @GetMapping(path = "/hello")
  public String hello() {
    return String.format("Hello World! <br/><br/> Configuration: <br/> Users URL: %s <br/> Wallets URL: %s", usersUrl, walletsUrl);
  }

  /* Endpoint 1: POST /orders
   * Request Body:
   *  {
   *    "user_id": ...,
   *    "items": [
   *      {"product_id": ..., "quantity": ...},
   *      ...
   *    ]
   *  }
   *
   * Concurrency Concerns:
   * 1) Single user sending their first two orders concurrently may cause both of them being discounted
   *    due to delay between reading and updating the `discount_availed` field.
   * 2) (FIXED) Stock being deducted even though sufficient quantity is not available due to another order
   *    deducting the stock between time of check and deduction.
   */
  @Transactional
  @PostMapping(path = "/orders", consumes = "application/json")
  public ResponseEntity<Order> createOrder(@RequestBody Order order) {
    log.info(order);
    // Check if required fields are present: user_id and items
    // Return 400 BAD_REQUEST otherwise
    if (order.getUser_id() == null || order.getItems() == null || order.getItems().size() == 0) {
      return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }

    User user;
    try { user = usersClient.getUserById(usersUrl, order.getUser_id()).orElseThrow(); }
    catch (NoSuchElementException e) { return new ResponseEntity<>(HttpStatus.BAD_REQUEST); }

    try {
      // Verify products and calculate total price
      Integer totalPrice = 0;
      for (OrderItem orderItem : order.getItems()) {
        // Check if product_id is present and ordered quantity is non-positive
        if (orderItem.getProduct_id() == null || orderItem.getQuantity() <= 0)
          return new ResponseEntity<>(HttpStatus.BAD_REQUEST);

        // Verify product with given product_id exists
        Product product = productRepository.findById(orderItem.getProduct_id()).orElseThrow();

        // Verify stock_quantity is sufficient
        if (product.getStock_quantity() < orderItem.getQuantity())
          return new ResponseEntity<>(HttpStatus.BAD_REQUEST);

        // Accumulate totalPrice
        totalPrice += product.getPrice() * orderItem.getQuantity();
      }

      // Apply 10% discount if applicable
      if (!user.getDiscount_availed()) {
        totalPrice -= (totalPrice / 10);
        usersClient.setDiscount_availedById(usersUrl, order.getUser_id());
      }

      // Set totalPrice of order entity
      order.setTotal_price(totalPrice);

      // Try to debit totalPrice from user's wallet
      HttpStatus trxnStatus = walletsClient.trxnByUserId(walletsUrl, order.getUser_id(), WalletTrxn.DEBIT, order.getTotal_price());
      // If unsuccessful, throw exception
      if (!trxnStatus.is2xxSuccessful())
        throw new Exception();

      List<OrderItem> items = order.getItems();
      // Deduct stock_quantity for all ordered products
      for (Integer i = 0; i < items.size(); i++) {
        OrderItem orderItem = items.get(i);
        Integer rowsUpdated = productRepository.decreaseProductStock_quantityById(orderItem.getProduct_id(), orderItem.getQuantity());
        // If no row was updated, product has insufficient quantity
        // - Add back previously deducted items, refund user, and revert discount_availed if needed
        // - Return 400 BAD_REQUEST
        if (rowsUpdated == 0) {
          // Refund user
          HttpStatus refundStatus = walletsClient.trxnByUserId(walletsUrl, order.getUser_id(), WalletTrxn.CREDIT, order.getTotal_price());
          if (!refundStatus.is2xxSuccessful())
            log.error("Failed to refund failed order!");

          return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
      }

      // Set PLACED order status and save order to repository
      order.setStatus(Order.STATUS_PLACED);
      Order savedOrder = orderRepository.save(order);

      // Returns 201 CREATED with newly created order as JSON
      return new ResponseEntity<>(savedOrder, HttpStatus.CREATED);
    }
    // Returns 400 BAD_REQEUST
    catch (Exception e) {
      log.debug("createOrder: " + e.toString(), e);
      TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
      
      return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }
  }

  /* Endpoint 2: GET /orders/{orderId}
   * Get order details by orderId
   */
  @GetMapping(path = "/orders/{orderId}")
  public ResponseEntity<Order> getOrderById(@PathVariable Integer orderId) {
    try {
      Order order = orderRepository.findById(orderId).orElseThrow();
      return new ResponseEntity<>(order, HttpStatus.OK);
    }
    // Return 404 NOT_FOUND if not found
    catch (NoSuchElementException e) {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
    // Return 500 INTERNAL_SERVER_ERROR for any other errors
    catch (Exception e) {
      return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  /* Endpoint 3: GET /orders/users/{userId}
   * Get all orders of user by userId
   */
  @Transactional
  @GetMapping(path = "/orders/users/{userId}")
  public ResponseEntity<List<Order>> getOrdersByUserId(@PathVariable Integer userId) {
    List<Order> orderList = orderRepository.findByUserId(userId);
    return new ResponseEntity<>(orderList, HttpStatus.OK);
  }

  /* Endpoint 4: DELETE /orders/{orderId}
   * Cancel order by orderId
   * Refunds the user and restores stock if successful
  */
  @Transactional(isolation=Isolation.REPEATABLE_READ)
  @DeleteMapping(path = "/orders/{orderId}")
  public ResponseEntity<Order> cancelOrderById(@PathVariable Integer orderId) {
    try {
      Order order = orderRepository.findById(orderId).orElseThrow();

      // If order is already delivered or cancelled, return 400 BAD_REQUEST
      // Otherwise, set status as cancelled
      if (order.getStatus() != Order.STATUS_PLACED)
        return new ResponseEntity<>(HttpStatus.BAD_REQUEST);

      order.setStatus(Order.STATUS_CANCELLED);
      orderRepository.save(order);

      // Restore product stock
      for (OrderItem orderItem : order.getItems()) {
        productRepository.increaseProductStock_quantityById(orderItem.getProduct_id(), orderItem.getQuantity());
      }

      // Refund the total_price to user
      HttpStatus trxnStatus = walletsClient.trxnByUserId(walletsUrl, order.getUser_id(), WalletTrxn.CREDIT, order.getTotal_price());
      if (!trxnStatus.is2xxSuccessful())
        return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
      return new ResponseEntity<>(HttpStatus.OK);
    }
    catch (NoSuchElementException e) {
      return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }
  }

  /* Endpoint 5: PUT /order/{orderId}
   * Marks a placed order as delivered by orderId
   * Request Body:
   *  {
   *    "order_id": ...,
   *    "status": "DELIVERED"
   *  }
   */
  @Transactional
  @PutMapping(path = "/orders/{orderId}")
  public ResponseEntity<Order> markOrderDeliveredById(@PathVariable Integer orderId, @RequestBody Order order) {
    try {
      // Check if orderId and order.order_id are same
      if (!orderId.equals(order.getOrder_id()))
        return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        
      // If the status in request body is not DELIVERED, return 400 BAD_REQEUST
      if (!order.getStatus().equals(Order.STATUS_DELIVERED))
        return new ResponseEntity<>(HttpStatus.BAD_REQUEST);

      Order savedOrder = orderRepository.findById(orderId).orElseThrow();

      // If the status of the saved order is not PLACED, return 400 BAD_REQUEST
      if (!savedOrder.getStatus().equals(Order.STATUS_PLACED))
        return new ResponseEntity<>(HttpStatus.BAD_REQUEST);

      // Set the order status to DELIVERED and return 200 OK
      savedOrder.setStatus(Order.STATUS_DELIVERED);
      orderRepository.save(savedOrder);
      return new ResponseEntity<>(HttpStatus.OK);
    }
    catch (NoSuchElementException e) {
      log.debug(e);
      return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }
  }
}
