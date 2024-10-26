package nl.martderoos.trueshuffle.requests.exceptions;

/**
 * Indicates that a Spotify user has revoked their authorization for TrueShuffle to access their account.
 */
public class AuthorizationRevokedException extends FatalRequestResponseException {
    public AuthorizationRevokedException(String message) {
        super(message);
    }
}
