package examples;

/** What {@link UsageBefore} looks like after running the plugin's conversions. */
public class UsageAfter {

    User fromConstructor() {
        return User.builder().id(1L).name("Ada").email("ada@example.com").active(true).build();
    }

    User fromSetters() {
        return User.builder().id(2L).name("Linus").email("linus@example.com").active(false).build();
    }
}
