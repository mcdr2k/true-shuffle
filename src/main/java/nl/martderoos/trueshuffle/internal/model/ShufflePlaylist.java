package nl.martderoos.trueshuffle.internal.model;

import nl.martderoos.trueshuffle.internal.adhoc.LazyExpiringApiData;
import nl.martderoos.trueshuffle.api.model.TrueShufflePlaylist;
import nl.martderoos.trueshuffle.api.model.TrueShufflePlaylistMetadata;
import nl.martderoos.trueshuffle.api.exceptions.ImmutablePlaylistException;
import nl.martderoos.trueshuffle.internal.requests.exceptions.FatalRequestResponseException;
import nl.martderoos.trueshuffle.internal.utility.PlaylistUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import se.michaelthelin.spotify.model_objects.specification.PlaylistSimplified;

import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Thread-safe class for modifying a user's playlist data (if allowed).
 */
public class ShufflePlaylist implements TrueShufflePlaylist {
    /**
     * The maximum number of tracks we may retrieve for any playlist. Currently, 2000.
     */
    public static final int PLAYLIST_TRACKS_HARD_LIMIT = 2000;
    private static final Logger LOGGER = LogManager.getLogger(ShufflePlaylist.class);

    private final TrueShuffleApi api;
    private final boolean mutable;
    private final Object mutationLock = new Object();

    private final String playlistId;
    private final String ownerId;

    private final LazyExpiringApiData<TrueShufflePlaylistMetadata> playlistData;
    private final LazyExpiringApiData<List<String>> playlistTracksUris;

    /**
     * Create a new shuffle playlist.
     *
     * @param api      the api that the playlist may leverage to get more data for this playlist
     * @param playlist the initial playlist's data
     * @param mutable  whether the playlist is mutable
     */
    public ShufflePlaylist(TrueShuffleApi api, PlaylistSimplified playlist, boolean mutable) {
        this(api, PlaylistUtil.toPlaylistData(playlist), mutable);
    }

    public ShufflePlaylist(TrueShuffleApi api, TrueShufflePlaylistMetadata playlist, boolean mutable) {
        this.api = Objects.requireNonNull(api);
        this.mutable = mutable;

        this.playlistId = playlist.id();
        this.ownerId = playlist.owner().id();

        playlistData = new LazyExpiringApiData<>(() -> api.streamPlaylistSimplified(playlistId), true, 10, TimeUnit.MINUTES);
        playlistData.setData(Objects.requireNonNull(playlist));
        playlistTracksUris = new LazyExpiringApiData<>(() -> api.streamPlaylistTracksUris(playlistId, PLAYLIST_TRACKS_HARD_LIMIT));
    }

    /**
     * Add and remove tracks to this playlist by leveraging the Spotify API. Adding and removing tracks has been
     * merged to better suit the Spotify api. <strong>Removal of tracks is always done before adding any tracks.</strong>
     * This method will throw an exception if you are not allowed to make modifications to this playlist.
     * Check {@link #isMutable()} beforehand.
     *
     * @param tracksToAdd    The tracks to add to the playlist (nullable).
     * @param tracksToRemove The tracks to remove from the playlist (nullable)
     * @throws ImmutablePlaylistException if this playlist is immutable.
     */
    public void addAndRemoveTracks(List<String> tracksToAdd, List<String> tracksToRemove) throws FatalRequestResponseException, ImmutablePlaylistException {
        verifyMutable();
        var tracksToAddCopy = copyTracks(tracksToAdd);
        var tracksToRemoveCopy = copyTracks(tracksToRemove);
        if (tracksToAddCopy.isEmpty() && tracksToRemoveCopy.isEmpty()) {
            return;
        }

        synchronized (mutationLock) {
            String playlistId = getPlaylistId();
            String snapshot = getSnapshotId();

            try {
                snapshot = api.removeTracks(playlistId, snapshot, tracksToRemoveCopy);
                api.addTracks(playlistId, snapshot, tracksToAddCopy);
            } finally {
                invalidate();
            }
        }
    }

    /**
     * Shuffles the playlist's tracks in-place. Internally this is done by moving songs at random to the front,
     * ensuring that every song is only reordered once. This method will throw an exception if the user of the api this
     * playlist is linked to is not the owner of this playlist. This method will throw an exception if you are not
     * allowed to make modifications to this playlist. Check {@link #isMutable()} beforehand.
     *
     * @throws ImmutablePlaylistException if this playlist is immutable.
     */
    public void shuffleInPlace() throws FatalRequestResponseException, ImmutablePlaylistException {
        verifyMutable();

        synchronized (mutationLock) {
            var id = getPlaylistId();
            var playlist = playlistData.getData();
            var snapshot = playlist.snapshotId();
            int total = playlist.trackCount();

            LOGGER.info("Shuffling {} in-place by reordering {} tracks", playlist.name(), total);

            try {
                Random random = new Random();
                for (int i = 0; i < total; i++) {
                    int moveFront = random.nextInt(i, total);
                    snapshot = api.reorderTrack(id, moveFront, 0, snapshot);
                }
            } finally {
                invalidate();
            }
        }
    }

    private void verifyMutable() throws ImmutablePlaylistException {
        if (!mutable) {
            throw new ImmutablePlaylistException(String.format("Playlist %s is immutable", getPlaylistId()));
        }
    }

    public boolean isMutable() {
        return mutable;
    }

    public String getPlaylistId() {
        return playlistId;
    }

    public String getOwnerId() {
        return ownerId;
    }

    private String getSnapshotId() throws FatalRequestResponseException {
        return playlistData.getData().snapshotId();
    }

    public TrueShufflePlaylistMetadata getMetadata() {
        try {
            return playlistData.getData();
        } catch (FatalRequestResponseException exception) {
            LOGGER.info("Could not refresh metadata for playlist {}", playlistId, exception);
            return playlistData.getCachedData();
        }
    }

    /**
     * Attempt to retrieve the playlist's tracks.
     *
     * @return the playlist's tracks, which are the unique identifiers of the tracks. Never null.
     * @throws FatalRequestResponseException if an attempt to get the playlist's tracks from the server fails
     */
    public List<String> getTracksUris() throws FatalRequestResponseException {
        return List.copyOf(playlistTracksUris.getData());
    }

    private void invalidate() {
        playlistData.expire();
        playlistTracksUris.invalidate();
    }

    private List<String> copyTracks(List<String> tracks) {
        if (tracks == null || tracks.isEmpty())
            return List.of();
        return List.copyOf(tracks);
    }
}
