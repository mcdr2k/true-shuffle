package nl.martderoos.trueshuffle.api;

import nl.martderoos.trueshuffle.exceptions.FatalRequestResponseException;

import java.util.List;

public interface TrueShuffleUserLibrary {
    /**
     * Get the URIs of the user's liked tracks.
     *
     * @return the list of user liked tracks.
     */
    List<String> getUserLikedTracksUris() throws FatalRequestResponseException;

    /**
     * Retrieve the most recently played/created playlists.
     *
     * @param limit The maximum number of playlists to retrieve.
     * @return A shallow copy of the underlying playlists.
     */
    List<TrueShufflePlaylistMetadata> getMostRecentPlaylists(int limit) throws FatalRequestResponseException;

    /**
     * Retrieve a playlist by its unique identifier. This can be a public, user private or collaborative playlist.
     * Private playlists from other users cannot be retrieved.
     *
     * @param playlistId The id of the playlist.
     * @return the playlist identified by the provided id, never null.
     * @throws FatalRequestResponseException if the playlist does not exist or is not visible to this user.
     */
    TrueShufflePlaylist getPlaylistById(String playlistId) throws FatalRequestResponseException;

    /**
     * Retrieves all playlists from the index that have the provided name.
     *
     * @param playlistName the playlist name to search for
     * @param mustBeOwner  whether we should only include playlists that are owned by the current user
     */
    List<? extends TrueShufflePlaylist> getPlaylistByName(String playlistName, boolean mustBeOwner) throws FatalRequestResponseException;

    /**
     * Creates a new playlist for the user with provided name and description.
     *
     * @param name        The name of the new playlist.
     * @param description The description of the new playlist.
     * @return The newly created playlist.
     */
    TrueShufflePlaylist createPlaylist(String name, String description) throws FatalRequestResponseException;

    /**
     * Check if this library owns the provided playlist.
     *
     * @param playlist the playlist to check the ownership of.
     * @return true if this library owns the playlist, false otherwise.
     */
    default boolean isOwnerOf(TrueShufflePlaylist playlist) {
        return getUserId().equals(playlist.getOwnerId());
    }

    /**
     * @return the library's user id
     */
    String getUserId();
}
