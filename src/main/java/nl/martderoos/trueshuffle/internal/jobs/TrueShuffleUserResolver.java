package nl.martderoos.trueshuffle.internal.jobs;

import nl.martderoos.trueshuffle.internal.InternalTrueShuffleUser;
import nl.martderoos.trueshuffle.api.exceptions.UserNotFoundException;

/**
 * Used by jobs to resolve user identifiers to {@link InternalTrueShuffleUser} instances.
 */
@FunctionalInterface
public interface TrueShuffleUserResolver {
    /**
     * Resolves the user identifier to the corresponding {@link InternalTrueShuffleUser user}.
     *
     * @param userId the user identifier to resolve.
     * @return the corresponding {@link InternalTrueShuffleUser user}, never null.
     * @throws UserNotFoundException if no user exists with that user identifier.
     */
    InternalTrueShuffleUser resolve(String userId) throws UserNotFoundException;
}
