package io.github.venshine.sample;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class GreeterTest {

    @Test
    public void greetUsesGivenName() {
        assertEquals("Hello, Gradle!", Greeter.greet(" Gradle "));
    }

    @Test
    public void greetFallsBackForNullOrBlank() {
        assertEquals("Hello, world!", Greeter.greet(null));
        assertEquals("Hello, world!", Greeter.greet("   "));
    }
}
