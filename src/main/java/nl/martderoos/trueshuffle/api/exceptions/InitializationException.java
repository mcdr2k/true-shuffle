package nl.martderoos.trueshuffle.api.exceptions;

/**
 * Indicates that something went wrong during initialization of a {@link nl.martderoos.trueshuffle.api.model.TrueShuffleClient}.
 */
public class InitializationException extends TrueShuffleException {
    public InitializationException(Exception e) {
        super(e);
    }
}
