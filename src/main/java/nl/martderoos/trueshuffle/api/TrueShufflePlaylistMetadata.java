package nl.martderoos.trueshuffle.api;

import java.util.List;
import java.util.Objects;

public record TrueShufflePlaylistMetadata(
        String id,
        String name,
        TrueShufflePlaylistOwner owner,
        String description,
        boolean collaborative,
        boolean publicPlaylist,
        String snapshotId,
        int trackCount,
        List<TrueShuffleImage> images
) {
    public TrueShufflePlaylistMetadata {
        Objects.requireNonNull(id);
        Objects.requireNonNull(name);
        Objects.requireNonNull(owner);
        images = images == null ? List.of() : List.copyOf(images);
    }
}
