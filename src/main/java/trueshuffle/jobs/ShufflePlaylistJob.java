package trueshuffle.jobs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import trueshuffle.model.ShuffleApi;
import trueshuffle.model.ShufflePlaylist;
import trueshuffle.utility.ShuffleUtil;
import trueshuffle.requests.exceptions.FatalRequestResponse;

import java.util.Objects;

public class ShufflePlaylistJob extends ShuffleJob {
    private static final Logger LOGGER = LoggerFactory.getLogger(ShufflePlaylistJob.class);
    private final String playlistId;

    public ShufflePlaylistJob(ShuffleApi api, String playlistId) {
        super(api);
        this.playlistId = Objects.requireNonNull(playlistId);
    }

    @Override
    protected void internalExecute(ShuffleJobStatus status) throws FatalRequestResponse {
        var api = getApi();

        var playlist = api.getUserLibrary().getPlaylistById(playlistId);
        if (!playlist.getOwnerId().equals(getUserId()))
            shuffleCopy(api, status, playlist);
        else
            shuffleInPlace(status, playlist);
    }

    private void shuffleInPlace(ShuffleJobStatus status, ShufflePlaylist source) throws FatalRequestResponse {
        // if (isUniqueName(stat, name, usedBatchNames))
        var name = source.getName();
        status.setSourcePlaylist(new ShuffleJobPlaylistStatus(name, source.getImages()));
        status.setTargetPlaylist(new ShuffleJobPlaylistStatus(name, source.getImages()));

        source.shuffleInPlace();

        status.setTargetPlaylist(new ShuffleJobPlaylistStatus(name, source.getImages()));
    }

    private void shuffleCopy(ShuffleApi api, ShuffleJobStatus status, ShufflePlaylist source) throws FatalRequestResponse {
        // if (isUniqueName(stat, name, usedBatchNames))
        var name = source.getName();
        if (!name.endsWith(TRUE_SHUFFLE_SUFFIX))
            name += TRUE_SHUFFLE_SUFFIX;

        LOGGER.info("Copying {} to {} before shuffling", source.getName(), name);
        status.setSourcePlaylist(new ShuffleJobPlaylistStatus(source.getName(), source.getImages()));
        status.setTargetPlaylist(new ShuffleJobPlaylistStatus(name, null));

        var target = findOrCreateUniquePlaylistByName(
                api.getUserLibrary(),
                name,
                source.getName() + " shuffled by TrueShuffle",
                status.getJobStatus());

        if (target == null)
            return;

        status.setTargetPlaylist(new ShuffleJobPlaylistStatus(target.getName(), target.getImages()));

        ShuffleUtil.shuffleInto(api, target, source.getPlaylistTracksUris());

        status.setTargetPlaylist(new ShuffleJobPlaylistStatus(target.getName(), target.getImages()));
    }

    public String getPlaylistId() {
        return playlistId;
    }
}
