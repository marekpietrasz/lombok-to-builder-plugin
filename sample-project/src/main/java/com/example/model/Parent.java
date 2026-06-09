package com.example.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Demonstrates the self-reference case. A {@link Child} holds a back-reference to its parent, so
 * {@code parent.setChild(new Child("leaf", parent))} references {@code parent} and cannot be folded
 * into the builder (that would read {@code parent} before it is assigned). The plugin keeps such a
 * setter right after the builder — see the "Keep self-referencing setters after the builder" setting.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Parent {
    private String name;
    private Child child;
}
