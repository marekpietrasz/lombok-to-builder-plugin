package com.example.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** A second {@code @Builder} class to demonstrate nested builder conversions. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    private String reference;
    private User customer;
    private BigDecimal total;
}
