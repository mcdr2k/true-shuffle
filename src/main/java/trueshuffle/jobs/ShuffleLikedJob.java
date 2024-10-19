package trueshuffle.jobs;

import trueshuffle.model.ShuffleApi;
import trueshuffle.utility.ShuffleUtil;
import trueshuffle.requests.exceptions.FatalRequestResponse;

public class ShuffleLikedJob extends ShuffleJob {

    public ShuffleLikedJob(ShuffleApi api) {
        super(api);
    }

    @Override
    protected void internalExecute(ShuffleJobStatus status) throws FatalRequestResponse {
        // if (isUniqueName(stat, LIKED_SONGS_TRUE_SHUFFLE, usedBatchNames)
        var api = getApi();

        status.setSourcePlaylist(new ShuffleJobPlaylistStatus(LIKED_SONGS_TRUE_SHUFFLE, null));
            var target = findOrCreateUniquePlaylistByName(
                    api.getUserLibrary(),
                    LIKED_SONGS_TRUE_SHUFFLE,
                    "Liked Songs shuffled by TrueShuffle",
                    status.getJobStatus());

            if (target == null)
                return;

            status.setTargetPlaylist(new ShuffleJobPlaylistStatus(target.getName(), target.getImages()));
            ShuffleUtil.shuffleInto(api, target, api.getUserLibrary().getUserSavedTracksUris());

            status.setTargetPlaylist(new ShuffleJobPlaylistStatus(target.getName(), target.getImages()));
    }
}
