package com.example;

import com.example.model.Catalog;
import com.example.model.Fee;
import com.example.model.Order;
import com.example.model.User;

import java.math.BigDecimal;

/**
 * Open this file in the sandbox IDE and try the plugin:
 *
 * <ul>
 *   <li>Put the caret on a {@code new User(...)} call -> Alt+Enter -> "Convert constructor to builder".</li>
 *   <li>Put the caret on the {@code User u = new User();} block (or a {@code u.setX(...)} line)
 *       -> Alt+Enter -> "Convert setters to builder".</li>
 *   <li>Right-click in the editor (no selection = whole file) or on the {@code sample-project}
 *       folder in the Project view -> "Convert Lombok Usages to Builder".</li>
 * </ul>
 */
public class Usage {

    User viaConstructor() {
        return new User(1L, "Ada Lovelace", "ada@example.com", true);
    }

    User viaSetters() {
        User u = User.builder()
                .id(2L)
                .name("Linus Torvalds")
                .email("linus@example.com")
                .active(false)
                .build();
        return u;
    }

    Order nestedBuilders() {
        return new Order("ORD-1", new User(3L, "Grace Hopper", "grace@example.com", true), new BigDecimal("42.00"));
    }

    // "Skip setting null values" (Settings → Tools → Lombok To Builder, default ON): the null email
    // is dropped, so this converts to User.builder().id(4L).name("Bob").active(false).build().
    User withNullValue() {
        return new User(4L, "Bob", null, false);
    }

    // Setter blocks are always converted, regardless of "Minimum values to convert" (that setting
    // gates constructor calls only) — so this 2-setter block is offered even with the default of 3.
    User fewSetters() {
        User u = new User();
        u.setId(5L);
        u.setName("Carol");
        return u;
    }

    // The Fee convenience constructor's parameters (category, value, ...) don't match the fields
    // (feeCategory, amount, ...). It takes four arguments, so the minimum-values threshold is met —
    // the plugin still does NOT offer to convert it, purely because the names don't map to fields.
    Fee handWrittenConstructor() {
        return new Fee("LATE", 10, "USD", "Late fee");
    }

    // The @Builder lives on the nested Catalog.Item; here it's referred to as Catalog.Item (its
    // simple name Item isn't imported). The conversion keeps the qualifier:
    // Catalog.Item.builder().sku("SKU-1").name("Widget").price(...).build() — NOT Item.builder().
    Catalog.Item nestedClassBuilder() {
        return new Catalog.Item("SKU-1", "Widget", new BigDecimal("9.99"));
    }

    public static void main(String[] args) {
        Usage usage = new Usage();
        System.out.println(usage.viaConstructor());
        System.out.println(usage.viaSetters());
        System.out.println(usage.nestedBuilders());
        System.out.println(usage.nestedClassBuilder());
    }
}
