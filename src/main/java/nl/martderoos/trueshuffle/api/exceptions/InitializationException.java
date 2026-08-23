package nl.martderoos.trueshuffle.api.exceptions;

import nl.martderoos.trueshuffle.internal.InternalTrueShuffleClient;

/**
 * Indicates that something went wrong during initialization of a {@link InternalTrueShuffleClient}.
 */
public class InitializationException extends TrueShuffleException {
    public InitializationException(Exception e) {
        super(e);
    }
}
