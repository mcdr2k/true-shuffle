package nl.martderoos.trueshuffle.jobs;

import nl.martderoos.trueshuffle.TrueShuffleUser;
import nl.martderoos.trueshuffle.requests.exceptions.FatalRequestResponse;
import nl.martderoos.trueshuffle.utility.ShuffleUtil;

public class ShuffleLikedJob extends ShuffleJob {

    public ShuffleLikedJob(TrueShuffleUser user) {
        super(user);
    }

    @Override
    protected void internalExecute(ShuffleJobStatus status) throws FatalRequestResponse {
        // if (isUniqueName(stat, LIKED_SONGS_TRUE_SHUFFLE, usedBatchNames)
        var user = getUser();
        var library = user.getUserLibrary();
        var api = user.getApi();

        status.setSourcePlaylist(new ShuffleJobPlaylistStatus(LIKED_SONGS_TRUE_SHUFFLE, null));
            var target = findOrCreateUniquePlaylistByName(
                    library,
                    LIKED_SONGS_TRUE_SHUFFLE,
                    "Liked Songs shuffled by TrueShuffle",
                    status.getJobStatus());

            if (target == null)
                return;

            status.setTargetPlaylist(new ShuffleJobPlaylistStatus(target.getName(), target.getImages()));
            ShuffleUtil.shuffleInto(api, target, library.getUserLikedTracksUris());

            status.setTargetPlaylist(new ShuffleJobPlaylistStatus(target.getName(), target.getImages()));
    }
}
