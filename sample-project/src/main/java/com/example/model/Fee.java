package com.example.model;

import lombok.Builder;
import lombok.Getter;

/**
 * Demonstrates a case the plugin intentionally leaves alone: the Lombok builder methods come from
 * the fields ({@code feeCategory}, {@code amount}, ...), but the hand-written convenience
 * constructor's parameters are named differently ({@code category}, {@code value}, ...) and it even
 * assigns a field through a setter. Since those parameter names don't match any field, the plugin
 * does not offer to convert {@code new Fee(...)} (it would otherwise emit an invalid
 * {@code .category(...)} call).
 *
 * <p>The convenience constructor takes four arguments, so the "Minimum values to convert" threshold
 * (default 3) is satisfied — the conversion is skipped purely because the names don't map to fields.
 */
@Getter
public class Fee {
    private String feeCategory;
    private int amount;
    private String currency;
    private String description;
    private boolean recurring;

    // @Builder uses this constructor, so the builder methods match the field names.
    @Builder
    public Fee(String feeCategory, int amount, String currency, String description, boolean recurring) {
        this.feeCategory = feeCategory;
        this.amount = amount;
        this.currency = currency;
        this.description = description;
        this.recurring = recurring;
    }

    // Convenience constructor with parameter names that DON'T match the fields. Distinct arity from
    // the @Builder constructor, so it doesn't clash.
    public Fee(String category, int value, String currencyCode, String text) {
        setFeeCategory(category);
        this.amount = value;
        this.currency = currencyCode;
        this.description = text;
    }

    public void setFeeCategory(String feeCategory) {
        this.feeCategory = feeCategory;
    }
}
