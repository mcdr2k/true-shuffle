package nl.martderoos.trueshuffle.utility;

import nl.martderoos.trueshuffle.model.ShuffleApi;
import nl.martderoos.trueshuffle.model.ShufflePlaylist;
import nl.martderoos.trueshuffle.requests.exceptions.FatalRequestResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Utility class for shuffling a {@link ShufflePlaylist}.
 */
public class ShuffleUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(ShuffleUtil.class);

    private ShuffleUtil() {
    }

    /**
     * Shuffles provided list of tracks into target playlist. The provided list of tracks will be diffed with the current
     * tracks to reduce the number of api calls.
     *
     * @param api       The api.
     * @param target    The target playlist.
     * @param newTracks The list of tracks that should be in target playlist.
     */
    public static void shuffleInto(ShuffleApi api, ShufflePlaylist target, Collection<String> newTracks) throws FatalRequestResponse {
        var currentTracks = target.getPlaylistTracksUris();
        var currentTracksCounter = new ItemCounter<>(currentTracks);

        List<String> tracksToAdd = new ArrayList<>();
        for (var newTrack : newTracks) {
            if (!currentTracksCounter.remove(newTrack))
                tracksToAdd.add(newTrack);
        }

        var newTracksCounter = new ItemCounter<>(newTracks);

        List<String> tracksToRemove = new ArrayList<>();
        for (var currentTrack : currentTracks) {
            if (!newTracksCounter.remove(currentTrack))
                tracksToRemove.add(currentTrack);
        }

        LOGGER.info("Updating playlist '{}' for {} ({} tracks removed, {} tracks added)", target.getName(), api.getDisplayName(), tracksToRemove.size(), tracksToAdd.size());

        target.addAndRemoveTracks(tracksToAdd, tracksToRemove);
        target.shuffleInPlace();
    }
}
