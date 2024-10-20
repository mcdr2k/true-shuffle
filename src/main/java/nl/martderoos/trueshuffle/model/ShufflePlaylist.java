package nl.martderoos.trueshuffle.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.michaelthelin.spotify.model_objects.specification.Image;
import se.michaelthelin.spotify.model_objects.specification.PlaylistSimplified;
import nl.martderoos.trueshuffle.adhoc.LazyExpiringApiData;
import nl.martderoos.trueshuffle.requests.exceptions.FatalRequestResponse;

import java.beans.Transient;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Thread-safe class for modifying a user's playlist data (if allowed).
 */
public class ShufflePlaylist {
    private static final Logger LOGGER = LoggerFactory.getLogger(ShufflePlaylist.class);

    private final ShuffleApi api;

    private final String playlistId;
    private final String ownerId;
    private final LazyPlaylistData playlistData = new LazyPlaylistData();
    private final LazyExpiringApiData<List<String>> playlistTracksUris;

    public ShufflePlaylist(ShuffleApi api, PlaylistSimplified playlist) {
        this.api = Objects.requireNonNull(api);
        playlistTracksUris = new LazyExpiringApiData<>(() -> api.streamPlaylistTracksUris(getPlaylistId()));
        playlistData.validateWith(Objects.requireNonNull(playlist));
        this.playlistId = playlist.getId();
        this.ownerId = playlist.getOwner().getId();
    }

    /**
     * Function which allows you to add and remove tracks through the Spotify API. Adding and removing tracks has been
     * merged to better suit the Spotify api. <strong>Due to Spotify's implementation, tracks are first removed and then
     * added.</strong> This method will throw an exception if you are not allowed to make modifications to this playlist.
     * Check {@link #canModify()} beforehand.
     *
     * @param tracksToAdd    The tracks to add to the playlist (nullable).
     * @param tracksToRemove The tracks to remove from the playlist (nullable)
     */
    public synchronized void addAndRemoveTracks(List<String> tracksToAdd, List<String> tracksToRemove) throws FatalRequestResponse {

        boolean changed = false;
        if (tracksToRemove != null && !tracksToRemove.isEmpty()) {
            api.removeTracks(this, tracksToRemove);
            changed = true;
        }

        if (tracksToAdd != null && !tracksToAdd.isEmpty()) {
            api.addTracks(this, tracksToAdd);
            changed = true;
        }

        if (changed)
            playlistData.invalidate();
    }

    /**
     * Shuffles the playlist's tracks in-place. Internally this is done by moving songs at random to the front,
     * ensuring that every song is only reordered once. This method will throw an exception if the user of the api this
     * playlist is linked to is not the owner of this playlist. This method will throw an exception if you are not
     * allowed to make modifications to this playlist. Check {@link #canModify()} beforehand.
     */
    public synchronized void shuffleInPlace() throws FatalRequestResponse {
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
    }

    /**
     * Makes a copy of this playlist in the user's library.
     *
     * @param newName        The name of the copy.
     * @param newDescription The description of the copy.
     */
    public synchronized void copyToNewPlaylist(String newName, String newDescription) throws FatalRequestResponse {
        var tracks = getPlaylistTracksUris();
        var playlist = getUserLibrary().createPlaylist(newName, newDescription);
        playlist.addAndRemoveTracks(tracks, null);
    }

    public synchronized List<String> getPlaylistTracksUris() throws FatalRequestResponse {
        return playlistTracksUris.getData();
    }

    public synchronized String getSnapshotId() throws FatalRequestResponse {
        return playlistData.getData().getSnapshotId();
    }

    public String getOwnerId() {
        return ownerId;
    }

    public String getPlaylistId() {
        return playlistId;
    }

    /**
     * Check whether you are allowed to make modifications to this playlist. Calling a modifying function
     * on this playlist while not being allowed to modify it will result in exceptions.
     *
     * @return True if modifications are allowed, false otherwise.
     */
    public boolean canModify() {
        return getUserLibrary().isOwner(this);
    }

    public synchronized String getName() throws FatalRequestResponse {
        return playlistData.getData().getName();
    }

    public UserLibrary getUserLibrary() {
        return api.getUserLibrary();
    }

    @Transient
    public synchronized Image[] getImages() throws FatalRequestResponse {
        return playlistData.getData().getImages();
    }

    /**
     * Private class for playlist data. Because the playlist data is strongly tied to track
     * data, this class also invalidates or revalidates the tracks whenever a change is made
     * to this class.
     */
    private class LazyPlaylistData extends LazyExpiringApiData<PlaylistSimplified> {
        public LazyPlaylistData() {
            super(() -> api.streamPlaylistSimplified(playlistId), true, 10, TimeUnit.MINUTES);
        }

        @Override
        public void invalidate() {
            super.invalidate();
            playlistTracksUris.invalidate();
        }

        @Override
        public void validateForAtLeast(long validForAtLeast, TimeUnit timeUnit) {
            super.validateForAtLeast(validForAtLeast, timeUnit);
            playlistTracksUris.validateForAtLeast(validForAtLeast, timeUnit);
        }
    }
}
