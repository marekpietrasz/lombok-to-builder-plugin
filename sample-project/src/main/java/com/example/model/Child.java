package com.example.model;

/**
 * A child that points back at its parent. Plain class (no {@code @Builder}); it exists so the
 * {@link Parent} example can construct a child that references the parent being built.
 */
public class Child {
    private final String label;
    private final Parent parent;

    public Child(String label, Parent parent) {
        this.label = label;
        this.parent = parent;
    }

    public String getLabel() {
        return label;
    }

    public Parent getParent() {
        return parent;
    }
}
