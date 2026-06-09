package com.example.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Demonstrates a nested {@code @Builder} class. The builder lives on the static nested
 * {@code Catalog.Item}, and {@code Usage} refers to it as {@code Catalog.Item} (its simple name
 * {@code Item} isn't in scope there). The conversion must keep the {@code Catalog.} qualifier and
 * emit {@code Catalog.Item.builder()} — a bare {@code Item.builder()} wouldn't compile.
 */
public class Catalog {

    @Data
    @Builder
    @AllArgsConstructor
    public static class Item {
        private String sku;
        private String name;
        private BigDecimal price;
    }
}
