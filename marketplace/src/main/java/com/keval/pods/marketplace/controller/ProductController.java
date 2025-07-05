package com.keval.pods.marketplace.controller;

import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.keval.pods.marketplace.model.Product;
import com.keval.pods.marketplace.repository.ProductRepository;

@RestController
public class ProductController {
  // Obtain the product repository
  @Autowired
  ProductRepository productRepository;

  /* Endpoint 1: GET /products
   * Returns 200 OK with a list of all products with details
   */
  @GetMapping(path = "/products")
  public ResponseEntity<List<Product>> getProducts() {
    List<Product> productList = productRepository.findAll();
    return new ResponseEntity<>(productList, HttpStatus.OK);
  }

  /* Endpoint 2: GET /products/{productId} */
  @GetMapping(path = "/products/{productId}")
  public ResponseEntity<Product> getProductById(@PathVariable Integer productId) {
    // Try to find product with productId
    // Return 200 OK with product details if found
    try {
      Product product = productRepository.findById(productId).orElseThrow();
      return new ResponseEntity<>(product, HttpStatus.OK);
    }
    // Return 404 NOT FOUND if not found
    catch (NoSuchElementException e) {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
  }
}
