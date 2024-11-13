package nl.martderoos.trueshuffle.model;


import nl.martderoos.trueshuffle.adhoc.LazyExpiringApiData;
import nl.martderoos.trueshuffle.requests.exceptions.FatalRequestResponseException;
import se.michaelthelin.spotify.model_objects.specification.Playlist;
import se.michaelthelin.spotify.model_objects.specification.PlaylistSimplified;

import java.util.*;
import java.util.stream.Collectors;

import static nl.martderoos.trueshuffle.utility.PlaylistUtil.toSimplifiedPlaylist;

/**
 * Thread-safe class that encapsulates a Spotify user's library.
 */
public class UserLibrary {
    /**
     * The maximum number of liked tracks to retrieve for a specific user.
     */
    public static final int LIKED_TRACKS_HARD_LIMIT = 2000;
    private final ShuffleApi api;
    private final String userId;

    private final LazyExpiringApiData<List<String>> userLikedTracksUris;
    private final LazyExpiringApiData<ShufflePlaylistIndex> index;

    public UserLibrary(ShuffleApi api) {
        this.api = Objects.requireNonNull(api);
        this.userId = api.getUserId();
        userLikedTracksUris = new LazyExpiringApiData<>(() -> api.streamUserLikedTracksUris(LIKED_TRACKS_HARD_LIMIT));
        this.index = new LazyExpiringApiData<>(this::createIndex);
    }

    /**
     * Get the URIs of the user's liked tracks.
     *
     * @return the list of user liked tracks with a maximum of {@link #LIKED_TRACKS_HARD_LIMIT} elements.
     */
    public synchronized List<String> getUserLikedTracksUris() throws FatalRequestResponseException {
        return userLikedTracksUris.getData();
    }

    /**
     * Retrieve the most recently played/created playlists.
     *
     * @param limit The maximum number of playlists to retrieve.
     * @return A shallow copy of the underlying playlists.
     */
    public synchronized List<PlaylistSimplified> getMostRecentPlaylists(int limit) throws FatalRequestResponseException {
        return new ArrayList<>(this.index.getData().getMostRecentPlaylists(limit));
    }

    /**
     * Retrieve a playlist by its unique identifier. This can be a public, user private or collaborative playlist.
     * Private playlists from other users cannot be retrieved.
     *
     * @param playlistId The id of the playlist.
     * @return the playlist identified by the provided id, never null.
     * @throws FatalRequestResponseException if the playlist does not exist or is not visible to this user.
     */
    public synchronized ShufflePlaylist getPlaylistById(String playlistId) throws FatalRequestResponseException {
        return index.getData().getPlaylistById(playlistId);
    }

    /**
     * Retrieves a playlist from the index by its name
     *
     * @param playlistName The playlist name to search for
     * @param mustBeOwner      Whether we should only include playlists that are owned by the current user
     */
    public synchronized List<ShufflePlaylist> getPlaylistByName(String playlistName, boolean mustBeOwner) throws FatalRequestResponseException {
        var result = index.getData().getPlaylistsByName(playlistName);
        if (mustBeOwner) {
            return result.stream().filter(this::isOwner).collect(Collectors.toList());
        }
        return result;
    }

    /**
     * Creates a new playlist for the user with provided name and description.
     *
     * @param name        The name of the new playlist.
     * @param description The description of the new playlist.
     * @return The newly created playlist.
     */
    public synchronized ShufflePlaylist createPlaylist(String name, String description) throws FatalRequestResponseException {
        var newPlaylist = api.uploadPlaylist(name, description);
        var simplified = toSimplifiedPlaylist(newPlaylist);
        return index.getData().addPlaylist(simplified);
    }

    private ShufflePlaylistIndex createIndex() throws FatalRequestResponseException {
        var index = new ShufflePlaylistIndex();
        index.reload();
        return index;
    }

    /**
     * Check if this library owns the provided playlist.
     *
     * @param playlist the playlist to check the ownership of.
     * @return true if this library owns the playlist, false otherwise.
     */

    public boolean isOwner(Playlist playlist) {
        return this.userId.equals(playlist.getOwner().getId());
    }

    /**
     * Check if this library owns the provided playlist.
     *
     * @param playlist the playlist to check the ownership of.
     * @return true if this library owns the playlist, false otherwise.
     */

    public boolean isOwner(PlaylistSimplified playlist) {
        return this.userId.equals(playlist.getOwner().getId());
    }

    /**
     * Check if this library owns the provided playlist.
     *
     * @param playlist the playlist to check the ownership of.
     * @return true if this library owns the playlist, false otherwise.
     */
    public boolean isOwner(ShufflePlaylist playlist) {
        return this.userId.equals(playlist.getOwnerId());
    }

    /**
     * Class that allows lookup of playlists by identifier and name. Note that the index search by name is only updated
     * sometimes and may be inconsistent between multiple requests.
     */
    private class ShufflePlaylistIndex {
        private List<PlaylistSimplified> playlists;
        private final Map<String, ShufflePlaylist> pidToPlaylist = new HashMap<>();
        private final Map<String, List<ShufflePlaylist>> nameToPlaylist = new HashMap<>();

        public void reload() throws FatalRequestResponseException {
            clear();
            this.playlists = api.streamUserPlaylists(50);
            for (var simplified : playlists) {
                var mutable = isOwner(simplified);
                var shufflePlaylist = new ShufflePlaylist(api, simplified, mutable);
                put(shufflePlaylist, false);
            }
        }

        public void clear() {
            playlists = null;
            pidToPlaylist.clear();
            nameToPlaylist.clear();
        }

        /**
         * Retrieve the most recently played/created playlists.
         *
         * @param limit The maximum number of playlists to retrieve.
         * @return A <b>view</b> of the underlying playlists.
         */
        public List<PlaylistSimplified> getMostRecentPlaylists(int limit) {
            return this.playlists.subList(0, Math.min(this.playlists.size(), limit));
        }

        private ShufflePlaylist addPlaylist(PlaylistSimplified playlistSimplified) throws FatalRequestResponseException {
            if (pidToPlaylist.containsKey(playlistSimplified.getId())) {
                return pidToPlaylist.get(playlistSimplified.getId());
            }
            playlists.add(0, playlistSimplified);
            var playlist = new ShufflePlaylist(api, playlistSimplified, isOwner(playlistSimplified));
            put(playlist, true);
            return playlist;
        }

        private void put(ShufflePlaylist playlist, boolean putFront) throws FatalRequestResponseException {
            pidToPlaylist.put(playlist.getPlaylistId(), playlist);

            var list = nameToPlaylist.computeIfAbsent(playlist.getName(), k -> new ArrayList<>());

            if (putFront)
                list.add(0, playlist);
            else
                list.add(playlist);
        }

        public ShufflePlaylist getPlaylistById(String playlistId) throws FatalRequestResponseException {
            var shufflePlaylist = pidToPlaylist.get(playlistId);
            if (shufflePlaylist == null) {
                var playlist = api.streamPlaylistSimplified(playlistId);
                shufflePlaylist = new ShufflePlaylist(api, playlist, isOwner(playlist));
                put(shufflePlaylist, true);
            }
            return shufflePlaylist;
        }

        public List<ShufflePlaylist> getPlaylistsByName(String playlistName) throws FatalRequestResponseException {
            // if list is not null, then we will not request playlists anymore which may be inconsistent
            // if some playlists were renamed
            var list = nameToPlaylist.get(playlistName);
            if (list == null) {
                var simplifiedPlaylists = api.searchPlaylistByExactName(playlistName, 4);
                for (var playlist : simplifiedPlaylists)
                    addPlaylist(playlist);
                list = simplifiedPlaylists.stream().map((p) -> new ShufflePlaylist(api, p, isOwner(p))).collect(Collectors.toList());
                nameToPlaylist.put(playlistName, list);
            }
            return list;
        }
    }
}
