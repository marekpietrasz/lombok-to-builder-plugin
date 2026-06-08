package com.example.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * Demonstrates a case the plugin intentionally leaves alone: the Lombok builder methods come from
 * the fields ({@code feeCategory}, {@code amount}), but the hand-written convenience constructor's
 * parameter is named {@code category} and assigns the field through a setter. Since {@code category}
 * doesn't match any field, {@code new Fee("…")} cannot be mapped to a real builder method, so the
 * plugin does not offer to convert it (rather than emitting an invalid {@code .category(...)} call).
 */
@Getter
@Builder
@AllArgsConstructor
public class Fee {
    private String feeCategory;
    private int amount;

    public Fee(String category) {
        setFeeCategory(category);
    }

    private void setFeeCategory(String feeCategory) {
        this.feeCategory = feeCategory;
    }
}
