package nl.martderoos.trueshuffle.model;

import nl.martderoos.trueshuffle.adhoc.LazyExpiringApiData;
import nl.martderoos.trueshuffle.exceptions.ImmutablePlaylistException;
import nl.martderoos.trueshuffle.requests.exceptions.FatalRequestResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.michaelthelin.spotify.model_objects.specification.Image;
import se.michaelthelin.spotify.model_objects.specification.PlaylistSimplified;

import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Thread-safe class for modifying a user's playlist data (if allowed).
 */
public class ShufflePlaylist {
    /**
     * The maximum number of tracks we may retrieve for any playlist. Currently, 2000.
     */
    public static final int PLAYLIST_TRACKS_HARD_LIMIT = 2000;
    private static final Logger LOGGER = LoggerFactory.getLogger(ShufflePlaylist.class);

    private final ShuffleApi api;
    private final boolean mutable;

    private final String playlistId;
    private final String ownerId;

    private final LazyExpiringApiData<PlaylistSimplified> playlistData;
    private final LazyExpiringApiData<List<String>> playlistTracksUris;

    /**
     * Create a new shuffle playlist.
     * @param api the api that the playlist may leverage to get more data for this playlist
     * @param playlist the initial playlist's data
     * @param mutable whether the playlist is mutable
     */
    public ShufflePlaylist(ShuffleApi api, PlaylistSimplified playlist, boolean mutable) {
        this.api = Objects.requireNonNull(api);
        this.mutable = mutable;

        this.playlistId = playlist.getId();
        this.ownerId = playlist.getOwner().getId();

        playlistData = new LazyExpiringApiData<>(() -> api.streamPlaylistSimplified(playlistId), true, 10, TimeUnit.MINUTES);
        playlistData.setData(Objects.requireNonNull(playlist));
        playlistTracksUris = new LazyExpiringApiData<>(() -> api.streamPlaylistTracksUris(getPlaylistId(), PLAYLIST_TRACKS_HARD_LIMIT));
    }

    /**
     * Add and remove tracks to this playlist by leveraging the Spotify API. Adding and removing tracks has been
     * merged to better suit the Spotify api. <strong>Due to Spotify's implementation, tracks are first removed and then
     * added.</strong> This method will throw an exception if you are not allowed to make modifications to this playlist.
     * Check {@link #isMutable()} beforehand.
     *
     * @param tracksToAdd    The tracks to add to the playlist (nullable).
     * @param tracksToRemove The tracks to remove from the playlist (nullable)
     * @throws ImmutablePlaylistException if this playlist is immutable.
     */
    public synchronized void addAndRemoveTracks(List<String> tracksToAdd, List<String> tracksToRemove) throws FatalRequestResponse, ImmutablePlaylistException {
        verifyMutable();
        String playlistId = getPlaylistId();
        String snapshot = getSnapshotId();

        boolean changed = false;
        if (tracksToRemove != null && !tracksToRemove.isEmpty()) {
            snapshot = api.removeTracks(playlistId, snapshot, tracksToRemove);
            changed = true;
        }

        if (tracksToAdd != null && !tracksToAdd.isEmpty()) {
            snapshot = api.addTracks(playlistId, snapshot, tracksToAdd);
            changed = true;
        }

        if (changed) {
            playlistData.invalidate();
            playlistTracksUris.invalidate();
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
    public synchronized void shuffleInPlace() throws FatalRequestResponse, ImmutablePlaylistException {
        verifyMutable();
        var id = getPlaylistId();
        var playlist = playlistData.getData();
        var snapshot = playlist.getSnapshotId();
        int total = playlist.getTracks().getTotal();

        LOGGER.info("Shuffling {} in-place by reordering {} tracks", playlist.getName(), total);

        Random random = new Random();

        for (int i = 0; i < total; i++) {
            int moveFront = random.nextInt(i, total);
            snapshot = api.reorderTrack(id, moveFront, 0, snapshot);
        }

        playlistData.invalidate();
        playlistTracksUris.invalidate();
    }

    private void verifyMutable() throws ImmutablePlaylistException {
        if (!mutable) {
            throw new ImmutablePlaylistException(String.format("Playlist %s from %s is immutable", getPlaylistId(), getOwnerId()));
        }
    }

    /**
     * @return True if modifications can be made to this playlist, false otherwise
     */
    public boolean isMutable() {
        return mutable;
    }

    /**
     * @return the unique identifier of the playlist
     */
    public String getPlaylistId() {
        return playlistId;
    }

    /**
     * @return the unique identifier of the owner of this playlist
     */
    public String getOwnerId() {
        return ownerId;
    }

    private synchronized String getSnapshotId() throws FatalRequestResponse {
        return playlistData.getData().getSnapshotId();
    }

    /**
     * Attempt to retrieve the playlist's tracks
     *
     * @return the playlist's tracks
     * @throws FatalRequestResponse if an attempt to get the playlist's tracks from the server fails
     */
    public synchronized List<String> getPlaylistTracksUris() throws FatalRequestResponse {
        return playlistTracksUris.getData();
    }

    /**
     * Attempt to retrieve the name of the playlist
     *
     * @return the name of the playlist
     * @throws FatalRequestResponse if an attempt to get the playlist's name from the server fails
     */
    public synchronized String getName() throws FatalRequestResponse {
        return playlistData.getData().getName();
    }

    /**
     * Attempt to retrieve the set of images for the thumbnail (different resolutions)
     *
     * @return the array of images (same image, different resolution)
     * @throws FatalRequestResponse if an attempt to get the playlist's thumbnails from the server fails
     */
    public synchronized Image[] getImages() throws FatalRequestResponse {
        return playlistData.getData().getImages();
    }
}
