package nl.martderoos.trueshuffle.jobs;

import nl.martderoos.trueshuffle.TrueShuffleUser;
import nl.martderoos.trueshuffle.exceptions.UserNotFoundException;

/**
 * Used by jobs to resolve user identifiers to {@link TrueShuffleUser} instances.
 */
@FunctionalInterface
public interface TrueShuffleUserResolver {
    /**
     * Resolves the user identifier to the corresponding {@link TrueShuffleUser user}.
     *
     * @param userId the user identifier to resolve.
     * @return the corresponding {@link TrueShuffleUser user}, never null.
     * @throws UserNotFoundException if no user exists with that user identifier.
     */
    TrueShuffleUser resolve(String userId) throws UserNotFoundException;
}
