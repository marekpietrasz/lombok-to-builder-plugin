package com.example.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * A real Lombok {@code @Builder} class. Lombok generates the all-args constructor, the no-args
 * constructor, the setters, and the {@code builder()} method, so the plugin's conversions resolve.
 */
@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private Long id;
    private String name;
    private String email;
    private boolean active;
}
