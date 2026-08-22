package nl.martderoos.trueshuffle.model;

import nl.martderoos.trueshuffle.requests.exceptions.FatalRequestResponseException;

import java.util.List;

/**
 * Consumer-facing view of a Spotify playlist managed by TrueShuffle.
 */
public interface TrueShufflePlaylist {
    /**
     * @return the unique identifier of the playlist
     */
    String getPlaylistId();

    /**
     * @return the unique identifier of the owner of this playlist
     */
    String getOwnerId();

    /**
     * Retrieve this playlist's metadata. The returned metadata may be stale.
     *
     * @return the playlist metadata, never null.
     */
    TrueShufflePlaylistMetadata getMetadata();

    /**
     * @return True if modifications can be made to this playlist, false otherwise
     */
    boolean isMutable();

    /**
     * Attempt to retrieve the playlist's tracks.
     *
     * @return the playlist's tracks, which are the unique identifiers of the tracks. Never null.
     * @throws FatalRequestResponseException if an attempt to get the playlist's tracks from the server fails
     */
    List<String> getTracksUris() throws FatalRequestResponseException;
}
