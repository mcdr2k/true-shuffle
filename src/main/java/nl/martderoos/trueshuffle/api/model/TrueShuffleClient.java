package nl.martderoos.trueshuffle.api.model;

import nl.martderoos.trueshuffle.api.exceptions.AuthorizationException;
import nl.martderoos.trueshuffle.api.exceptions.InitializationException;
import nl.martderoos.trueshuffle.api.exceptions.UserNotFoundException;
import nl.martderoos.trueshuffle.internal.jobs.TrueShuffleJob;
import nl.martderoos.trueshuffle.internal.jobs.TrueShuffleJobExecution;

import java.net.URI;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executor;

public interface TrueShuffleClient {
    /**
     * Generates a random 'state' to be used for security purposes. Note that this generated state should be stored
     * by the user in their own code as to be able to use it further down the authorization flow. This program
     * itself does nothing with it unless provided to some of this instance's functions, like {@link #getAuthorizationURI(String)}.
     * The random state generated is produced by a call to {@link UUID#randomUUID()}.
     */
    static String generateRandomState() {
        return UUID.randomUUID().toString();
    }

    void initialize() throws InitializationException;

    TrueShuffleUser addAuthorizedUser(String userId, TrueShuffleUserCredentials credentials) throws AuthorizationException;

    TrueShuffleUser addAuthorizedUser(String code) throws AuthorizationException;

    void removeAuthorizedUser(String userId);

    Set<String> getAuthorizedUsers();

    TrueShuffleUser getAuthorizedUser(String userId) throws UserNotFoundException;

    TrueShuffleJobExecution execute(TrueShuffleJob job, Executor executor) throws UserNotFoundException;

    TrueShuffleJobExecution shuffleLikedSongs(String userId, Executor executor) throws UserNotFoundException;

    TrueShuffleJobExecution shufflePlaylist(String userId, String playlistId, Executor executor) throws UserNotFoundException;

    URI getAuthorizationURI(String state);

    URI getRedirectUri();
}
