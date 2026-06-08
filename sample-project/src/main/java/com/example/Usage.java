package com.example;

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
        User u = new User();
        u.setId(2L);
        u.setName("Linus Torvalds");
        u.setEmail("linus@example.com");
        u.setActive(false);
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

    // "Minimum values to convert" (default 3): this 2-value usage is left alone by default. Lower
    // the setting to 2 (or 1) to have the plugin offer the conversion here too.
    User tooFewValues() {
        User u = new User();
        u.setId(5L);
        u.setName("Carol");
        return u;
    }

    // The Fee(String) constructor's parameter (category) doesn't match the field (feeCategory),
    // so the plugin intentionally does NOT offer to convert this one.
    Fee handWrittenConstructor() {
        return new Fee("LATE");
    }

    public static void main(String[] args) {
        Usage usage = new Usage();
        System.out.println(usage.viaConstructor());
        System.out.println(usage.viaSetters());
        System.out.println(usage.nestedBuilders());
    }
}
