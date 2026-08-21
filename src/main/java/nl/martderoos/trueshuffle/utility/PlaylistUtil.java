package nl.martderoos.trueshuffle.utility;

import nl.martderoos.trueshuffle.model.TrueShuffleImage;
import nl.martderoos.trueshuffle.model.TrueShufflePlaylistData;
import nl.martderoos.trueshuffle.model.TrueShufflePlaylistOwner;
import se.michaelthelin.spotify.model_objects.specification.Playlist;
import se.michaelthelin.spotify.model_objects.specification.PlaylistSimplified;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * utility class for {@link Playlist}.
 */
public class PlaylistUtil {
    private PlaylistUtil() {}

    public static TrueShufflePlaylistData toPlaylistData(Playlist playlist) {
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

    public static TrueShufflePlaylistData toPlaylistData(PlaylistSimplified playlist) {
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

    private static TrueShufflePlaylistData toPlaylistData(
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
        return new TrueShufflePlaylistData(
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
