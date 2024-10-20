package nl.martderoos.trueshuffle.exceptions;

import java.util.Objects;

public class UserNotFoundException extends Exception {
    public UserNotFoundException(String user) {
        super(String.format("Could not find '%s' among the set of authorised users.", Objects.requireNonNull(user)));
    }
}
