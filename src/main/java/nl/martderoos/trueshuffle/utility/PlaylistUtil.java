package nl.martderoos.trueshuffle.utility;

import se.michaelthelin.spotify.model_objects.miscellaneous.PlaylistTracksInformation;
import se.michaelthelin.spotify.model_objects.specification.Playlist;
import se.michaelthelin.spotify.model_objects.specification.PlaylistSimplified;

/**
 * utility class for {@link Playlist}.
 */
public class PlaylistUtil {
    private PlaylistUtil() {}

    /**
     * Convert a rich {@link Playlist} instance to a {@link PlaylistSimplified} instance.
     */
    public static PlaylistSimplified toSimplifiedPlaylist(Playlist playlist) {
        return new PlaylistSimplified.Builder()
                .setCollaborative(playlist.getIsCollaborative())
                .setExternalUrls(playlist.getExternalUrls())
                .setTracks(new PlaylistTracksInformation.Builder().setTotal(playlist.getTracks().getTotal()).setHref(playlist.getTracks().getHref()).build())
                .setHref(playlist.getHref())
                .setId(playlist.getId())
                .setImages(playlist.getImages())
                .setName(playlist.getName())
                .setOwner(playlist.getOwner())
                .setPublicAccess(playlist.getIsPublicAccess())
                .setSnapshotId(playlist.getSnapshotId())
                .setType(playlist.getType())
                .setUri(playlist.getUri())
                .build();
    }
}
