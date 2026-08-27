package nl.martderoos.trueshuffle.model;


import nl.martderoos.trueshuffle.api.TrueShufflePlaylistOwner;
import nl.martderoos.trueshuffle.api.TrueShuffleUserLibrary;
import nl.martderoos.trueshuffle.adhoc.LazyExpiringApiData;
import nl.martderoos.trueshuffle.api.TrueShufflePlaylistMetadata;
import nl.martderoos.trueshuffle.exceptions.FatalRequestResponseException;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Thread-safe class that encapsulates a Spotify user's library.
 */
public class InternalTrueShuffleUserLibrary implements TrueShuffleUserLibrary {
    /**
     * The maximum number of liked tracks to retrieve for a specific user.
     */
    public static final int LIKED_TRACKS_HARD_LIMIT = 2000;
    private final TrueShuffleApi api;
    private final String userId;

    private final LazyExpiringApiData<List<String>> userLikedTracksUris;
    private final LazyExpiringApiData<ShufflePlaylistIndex> index;

    public InternalTrueShuffleUserLibrary(TrueShuffleApi api) {
        this.api = Objects.requireNonNull(api);
        this.userId = api.getUserId();
        userLikedTracksUris = new LazyExpiringApiData<>(() -> api.streamUserLikedTracksUris(LIKED_TRACKS_HARD_LIMIT));
        this.index = new LazyExpiringApiData<>(this::createIndex);
    }

    public synchronized List<String> getUserLikedTracksUris() throws FatalRequestResponseException {
        return new ArrayList<>(userLikedTracksUris.getData());
    }

    public synchronized List<TrueShufflePlaylistMetadata> getMostRecentPlaylists(int limit) throws FatalRequestResponseException {
        if (limit < 0) throw new IllegalArgumentException("Limit must be at least 0");
        return new ArrayList<>(this.index.getData().getMostRecentPlaylists(limit));
    }

    public synchronized ShufflePlaylist getPlaylistById(String playlistId) throws FatalRequestResponseException {
        return index.getData().getPlaylistById(playlistId);
    }

    public synchronized List<ShufflePlaylist> getPlaylistByName(String playlistName, boolean mustBeOwner) throws FatalRequestResponseException {
        var result = index.getData().getPlaylistsByName(playlistName);
        if (mustBeOwner) {
            return result.stream().filter(this::isOwnerOf).collect(Collectors.toList());
        }
        return result;
    }

    public synchronized ShufflePlaylist createPlaylist(String name, String description) throws FatalRequestResponseException {
        var newPlaylist = api.uploadPlaylist(name, description);
        return index.getData().addPlaylist(newPlaylist);
    }

    private ShufflePlaylistIndex createIndex() throws FatalRequestResponseException {
        var index = new ShufflePlaylistIndex();
        index.reload();
        return index;
    }

    private boolean isOwnerOf(TrueShufflePlaylistOwner owner) {
        return getUserId().equals(owner.id());
    }

    public String getUserId() {
        return userId;
    }

    /**
     * Allows lookup of playlists by identifier and name. Note that the index search by name is only updated
     * sometimes and may be inconsistent between multiple requests.
     */
    private class ShufflePlaylistIndex {
        private List<TrueShufflePlaylistMetadata> playlists;
        private final Map<String, ShufflePlaylist> pidToPlaylist = new HashMap<>();
        private final Map<String, List<ShufflePlaylist>> nameToPlaylist = new HashMap<>();

        public void reload() throws FatalRequestResponseException {
            clear();
            this.playlists = api.streamUserPlaylists(50);
            for (var simplified : playlists) {
                var mutable = isOwnerOf(simplified.owner());
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
        public List<TrueShufflePlaylistMetadata> getMostRecentPlaylists(int limit) {
            return this.playlists.subList(0, Math.min(this.playlists.size(), limit));
        }

        private ShufflePlaylist addPlaylist(TrueShufflePlaylistMetadata playlistData) {
            if (pidToPlaylist.containsKey(playlistData.id())) {
                return pidToPlaylist.get(playlistData.id());
            }
            playlists.add(0, playlistData);
            var playlist = new ShufflePlaylist(api, playlistData, isOwnerOf(playlistData.owner()));
            put(playlist, true);
            return playlist;
        }

        private void put(ShufflePlaylist playlist, boolean putFront) {
            pidToPlaylist.put(playlist.getPlaylistId(), playlist);

            var list = nameToPlaylist.computeIfAbsent(playlist.getMetadata().name(), k -> new ArrayList<>());

            if (putFront)
                list.add(0, playlist);
            else
                list.add(playlist);
        }

        public ShufflePlaylist getPlaylistById(String playlistId) throws FatalRequestResponseException {
            var shufflePlaylist = pidToPlaylist.get(playlistId);
            if (shufflePlaylist == null) {
                var playlist = api.streamPlaylistSimplified(playlistId);
                shufflePlaylist = new ShufflePlaylist(api, playlist, isOwnerOf(playlist.owner()));
                put(shufflePlaylist, true);
            }
            return shufflePlaylist;
        }

        public List<ShufflePlaylist> getPlaylistsByName(String playlistName) throws FatalRequestResponseException {
            // if list is not null, then we will not request playlists anymore which may be inconsistent
            // if some playlists were renamed
            var list = nameToPlaylist.get(playlistName);
            if (list == null) {
                list = new ArrayList<>();
                var simplifiedPlaylists = api.searchPlaylistByExactName(playlistName, 5);
                for (var playlist : simplifiedPlaylists)
                    list.add(addPlaylist(playlist));
                nameToPlaylist.put(playlistName, list);
            }
            return new ArrayList<>(list);
        }
    }
}
