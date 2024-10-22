package nl.martderoos.trueshuffle.exceptions;

/**
 * Indicates that something went wrong during initialization of a {@link nl.martderoos.trueshuffle.TrueShuffleClient}
 */
public class TrueShuffleInitializationException extends TrueShuffleException {
    public TrueShuffleInitializationException(Exception e) {
        super(e);
    }
}
