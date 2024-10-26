package nl.martderoos.trueshuffle.exceptions;

/**
 * Indicates that something went wrong during initialization of a {@link nl.martderoos.trueshuffle.TrueShuffleClient}.
 */
public class InitializationException extends TrueShuffleException {
    public InitializationException(Exception e) {
        super(e);
    }
}
