package nl.martderoos.trueshuffle.exceptions;

public class TrueShuffleAuthorisationException extends Exception {
    public TrueShuffleAuthorisationException(String message, Exception e) {
        super(message, e);
    }
}
