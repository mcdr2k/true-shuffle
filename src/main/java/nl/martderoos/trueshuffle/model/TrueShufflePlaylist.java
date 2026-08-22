package nl.martderoos.trueshuffle.model;

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
}
