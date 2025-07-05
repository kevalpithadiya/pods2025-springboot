package com.keval.pods.marketplace;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.keval.pods.marketplace.model.Product;
import com.keval.pods.marketplace.repository.ProductRepository;
import com.opencsv.bean.CsvToBeanBuilder;

import lombok.extern.apachecommons.CommonsLog;

@SpringBootApplication
@CommonsLog
public class MarketplaceApplication implements ApplicationRunner {

    @Autowired
    private ProductRepository productRepository;

    public static void main(String[] args) {
        SpringApplication.run(MarketplaceApplication.class, args);
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        // Skip if products have been initialized already
        if(productRepository.count() > 0) {
          log.info("Products Already Initialized");
          return;
        }

        log.info("Initializing Products");

        try (InputStreamReader reader = new InputStreamReader(getClass().getResourceAsStream("/static/products.csv"))) {
            List<Product> productList = new CsvToBeanBuilder<Product>(reader)
                    .withType(Product.class)
                    .build()
                    .parse();

            productRepository.saveAll(productList);
            log.info("Successfully loaded data from CSV file");

        } catch (IOException ex) {
            log.error("Error loading products from CSV", ex);
            throw ex; // Re-throw the exception to stop application startup
        }


    }
}
