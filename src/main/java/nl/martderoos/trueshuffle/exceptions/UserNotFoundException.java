package nl.martderoos.trueshuffle.exceptions;

import java.util.Objects;

/**
 * Indicates that the specified user could not be found
 */
public class UserNotFoundException extends TrueShuffleException {
    public UserNotFoundException(String user) {
        super(String.format("Could not find '%s' among the set of authorised users.", Objects.requireNonNull(user)));
    }
}
