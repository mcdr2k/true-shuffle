package nl.martderoos.trueshuffle.exceptions;

import nl.martderoos.trueshuffle.api.TrueShuffleClient;

/**
 * Indicates that something went wrong during initialization of a {@link TrueShuffleClient}.
 */
public class InitializationException extends TrueShuffleException {
    public InitializationException(Exception e) {
        super(e);
    }
}
