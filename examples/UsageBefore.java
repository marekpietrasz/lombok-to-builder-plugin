package examples;

/** Usages of {@link User} before conversion. Open this file and try the plugin on it. */
public class UsageBefore {

    // Caret on `new User(...)` -> "Convert constructor to builder".
    User fromConstructor() {
        return new User(1L, "Ada", "ada@example.com", true);
    }

    // Caret on the declaration (or any setter line) -> "Convert setters to builder".
    User fromSetters() {
        User u = new User();
        u.setId(2L);
        u.setName("Linus");
        u.setEmail("linus@example.com");
        u.setActive(false);
        return u;
    }

    // Right-click -> "Convert Lombok Usages to Builder" converts both of the above at once.
}
