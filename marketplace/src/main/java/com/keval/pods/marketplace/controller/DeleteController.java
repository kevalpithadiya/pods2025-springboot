package com.keval.pods.marketplace.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.keval.pods.marketplace.client.WalletsClient;
import com.keval.pods.marketplace.client.model.WalletTrxn;
import com.keval.pods.marketplace.model.Order;
import com.keval.pods.marketplace.model.OrderItem;
import com.keval.pods.marketplace.repository.OrderRepository;
import com.keval.pods.marketplace.repository.ProductRepository;


import lombok.extern.apachecommons.CommonsLog;

@RestController
@CommonsLog
public class DeleteController {
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
    private WalletsClient walletsClient = new WalletsClient();

    /* 
    * Endpoint 1: DELETE /marketplace/users/{userId}
    * Cancel any Orders for the given user that are in "PLACED" status, by making the DELETE requests.
    * Return 200 if at least one record was removed, else return 404 if the user had no orders.
    */
    @Modifying
    @Transactional(isolation=Isolation.REPEATABLE_READ)
    @DeleteMapping("/marketplace/users/{userId}")
    public ResponseEntity<Void> 
    cancelOrdersByUserId(@PathVariable Integer userId) {
        List<Order> orders = orderRepository.findByUserId(userId);
        if (orders.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        else {
            for (Order order : orders) {
                if (order.getStatus().equals(Order.STATUS_PLACED)) {
                    order.setStatus(Order.STATUS_CANCELLED); //Cancel order
                    //refunds the user's wallet
                    walletsClient.trxnByUserId(walletsUrl, userId, WalletTrxn.CREDIT, order.getTotal_price());
                          // Restore product stock
                for (OrderItem orderItem : order.getItems()) {
                    productRepository.increaseProductStock_quantityById(orderItem.getProduct_id(), orderItem.getQuantity());
                }

                }
            }
            return ResponseEntity.ok().build();
        }
    }

    /* REMOVED
     * Endpoint 2: DELETE /marketplace      
     * Cancel all orders in PLACED status
     * Return 200
     */
    // @Modifying
    // @Transactional
    // @DeleteMapping("/marketplace")
    // public ResponseEntity<Void> cancelAllPlacedOrders() {
    //     List<Order> orders = orderRepository.findAllPlacedOrders(Order.STATUS_PLACED);
    //     if (orders.isEmpty()) {
    //         return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    //     }
    //     else {
    //         for (Order order : orders) {
    //             if (order.getStatus().equals(Order.STATUS_PLACED)) {
    //                 order.setStatus(Order.STATUS_CANCELLED); //Cancel order
    //                 Integer user_id = order.getUser_id();
    //                 //refunds the user's wallet
    //                 walletsClient.trxnByUserId(walletsUrl, user_id, WalletTrxn.CREDIT, order.getTotal_price());
    //             // Restore product stock
    //             for (OrderItem orderItem : order.getItems()) {
    //                 productRepository.increaseProductStock_quantityById(orderItem.getProduct_id(), orderItem.getQuantity());
    //             }

    //             }
    //         }
            
    //         return ResponseEntity.ok().build();
    //     }

    // }    
}
