package nl.martderoos.trueshuffle.model;

import nl.martderoos.trueshuffle.requests.exceptions.FatalRequestResponseException;
import java.util.List;

/**
 * Consumer-facing view of a Spotify playlist managed by TrueShuffle.
 *
 * <p>Playlist tracks are loaded lazily. The only playlist mutation exposed by this interface is shuffling.</p>
 */
public interface TrueShufflePlaylist {
    String getPlaylistId();

    String getOwnerId();

    String getName() throws FatalRequestResponseException;

    List<TrueShuffleImage> getImages() throws FatalRequestResponseException;

    boolean isMutable();

    List<String> getPlaylistTracksUris() throws FatalRequestResponseException;

    void shuffleInPlace() throws FatalRequestResponseException;
}
