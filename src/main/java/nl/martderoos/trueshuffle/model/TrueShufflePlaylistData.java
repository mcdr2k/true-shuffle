package nl.martderoos.trueshuffle.model;

import java.util.List;
import java.util.Objects;

public record TrueShufflePlaylistData(
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
    public TrueShufflePlaylistData {
        Objects.requireNonNull(id);
        Objects.requireNonNull(name);
        Objects.requireNonNull(owner);
        images = images == null ? List.of() : List.copyOf(images);
    }
}
