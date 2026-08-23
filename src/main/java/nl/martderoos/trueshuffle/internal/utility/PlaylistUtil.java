package nl.martderoos.trueshuffle.internal.utility;

import nl.martderoos.trueshuffle.api.model.TrueShuffleImage;
import nl.martderoos.trueshuffle.api.model.TrueShufflePlaylistMetadata;
import nl.martderoos.trueshuffle.api.model.TrueShufflePlaylistOwner;
import se.michaelthelin.spotify.model_objects.specification.Playlist;
import se.michaelthelin.spotify.model_objects.specification.PlaylistSimplified;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * utility class for {@link Playlist}.
 */
public class PlaylistUtil {
    private PlaylistUtil() {}

    public static TrueShufflePlaylistMetadata toPlaylistData(Playlist playlist) {
        return toPlaylistData(
                playlist.getId(),
                playlist.getName(),
                playlist.getOwner().getId(),
                playlist.getOwner().getDisplayName(),
                playlist.getDescription(),
                playlist.getIsCollaborative(),
                playlist.getIsPublicAccess(),
                playlist.getSnapshotId(),
                playlist.getTracks().getTotal(),
                playlist.getImages()
        );
    }

    public static TrueShufflePlaylistMetadata toPlaylistData(PlaylistSimplified playlist) {
        return toPlaylistData(
                playlist.getId(),
                playlist.getName(),
                playlist.getOwner().getId(),
                playlist.getOwner().getDisplayName(),
                null,
                playlist.getIsCollaborative(),
                playlist.getIsPublicAccess(),
                playlist.getSnapshotId(),
                playlist.getTracks().getTotal(),
                playlist.getImages()
        );
    }

    private static TrueShufflePlaylistMetadata toPlaylistData(
            String id,
            String name,
            String ownerId,
            String ownerDisplayName,
            String description,
            boolean collaborative,
            boolean publicPlaylist,
            String snapshotId,
            int trackCount,
            se.michaelthelin.spotify.model_objects.specification.Image[] images
    ) {
        var domainImages = images == null
                ? List.<TrueShuffleImage>of()
                : Arrays.stream(images)
                .filter(Objects::nonNull)
                .map(image -> new TrueShuffleImage(image.getUrl(), image.getWidth(), image.getHeight()))
                .toList();
        return new TrueShufflePlaylistMetadata(
                id,
                name,
                new TrueShufflePlaylistOwner(ownerId, ownerDisplayName),
                description,
                collaborative,
                publicPlaylist,
                snapshotId,
                trackCount,
                domainImages
        );
    }
}
