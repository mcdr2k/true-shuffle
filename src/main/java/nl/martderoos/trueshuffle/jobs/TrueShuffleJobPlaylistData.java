package nl.martderoos.trueshuffle.jobs;

import se.michaelthelin.spotify.model_objects.specification.Image;

import java.util.Objects;

/**
 * Immutable class that carries details about a playlist used during shuffles.
 */
public final class TrueShuffleJobPlaylistData {
    private final String playlistId;
    private final String name;
    private final Image[] images;

    private TrueShuffleJobPlaylistData(String playlistId, String name, Image[] images) {
        this.playlistId = playlistId;
        this.name = name;
        this.images = images;
    }

    /**
     * Factory method for creating playlist data that references any real playlist.
     *
     * @param playlistId the unique identifier of the playlist (non-nullable).
     * @param name       the name of the playlist (non-nullable).
     * @param images     the images (thumbnails) of this playlist (nullable).
     * @return a new instance.
     * @throws NullPointerException if either playlistId or name is null.
     * @throws IllegalArgumentException if either playlistId or name is blank.
     */
    public static TrueShuffleJobPlaylistData newPlaylistData(String playlistId, String name, Image[] images) {
        Objects.requireNonNull(playlistId);
        Objects.requireNonNull(name);
        if (playlistId.isBlank()) throw new IllegalArgumentException("playlistId is blank");
        if (name.isBlank()) throw new IllegalArgumentException("name is blank");
        return new TrueShuffleJobPlaylistData(playlistId, name, images);
    }

    /**
     * Factory method for creating playlist data that references a user's liked songs pseudo playlist.
     *
     * @param name the name of the liked songs pseudo playlist (not nullable).
     * @return a new instance.
     * @throws NullPointerException if the argument is null.
     * @throws IllegalArgumentException if name is blank.
     */
    public static TrueShuffleJobPlaylistData newLikedSongsData(String name) {
        Objects.requireNonNull(name);
        if (name.isBlank()) throw new IllegalArgumentException("name is blank");
        return new TrueShuffleJobPlaylistData(null, name, null);
    }

    /**
     * Get the unique identifier of the playlist.
     *
     * @return the unique identifier. Never null unless we are referencing the liked songs pseudo playlist.
     */
    public String getPlaylistId() {
        return playlistId;
    }

    /**
     * Get the name of the playlist.
     *
     * @return the name of the playlist, never null.
     */
    public String getName() {
        return name;
    }

    /**
     * Get the images (thumbnails) of this playlist in different resolutions.
     *
     * @return the playlist's images, possibly null. If this instance is referencing the liked songs pseudo playlist,
     * then this method will always return null. The returned array may be empty.
     */
    public Image[] getImages() {
        return images;
    }

    /**
     * @return true if this instance is referencing a user's liked songs (pseudo) playlist, false otherwise.
     */
    public boolean isLikedSongsPlaylist() {
        return playlistId == null;
    }
}
