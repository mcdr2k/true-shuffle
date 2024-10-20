package nl.martderoos.trueshuffle.jobs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import nl.martderoos.trueshuffle.model.ShuffleApi;
import nl.martderoos.trueshuffle.model.ShufflePlaylist;
import nl.martderoos.trueshuffle.model.UserLibrary;
import nl.martderoos.trueshuffle.requests.exceptions.FatalRequestResponse;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executor;

public abstract class ShuffleJob {
    private static final Logger LOGGER = LoggerFactory.getLogger(ShuffleJob.class);
    public static final String TRUE_SHUFFLE_SUFFIX = " - TrueShuffle";
    public static final String LIKED_SONGS_TRUE_SHUFFLE = "Liked Songs" + TRUE_SHUFFLE_SUFFIX;
    private final ShuffleApi api;

    ShuffleJob(ShuffleApi api) {
        this.api = Objects.requireNonNull(api);
    }

    /**
     * Executes this job, following the provided executor's schedule.
     * @param executor The execution schedule.
     * @return The status of this job, which is updated continuously throughout the execution of this job.
     */
    public final ShuffleJobStatus execute(Executor executor) {
        var status = createStatus();
        executor.execute(() -> this.execute(status));
        return status;
    }

    private final void execute(ShuffleJobStatus status) {
        status.getJobStatus().setStatus(EJobStatus.EXECUTING);
        try {
            internalExecute(status);
            if (status.getJobStatus().getStatus() == EJobStatus.EXECUTING)
                status.getJobStatus().setStatus(EJobStatus.FINISHED);
        } catch (FatalRequestResponse e) {
            LOGGER.error("", e);
            status.setJobStatus(new JobStatus(EJobStatus.TERMINATED, "An unexpected error occurred"));
        }
    }

    protected abstract void internalExecute(ShuffleJobStatus status) throws FatalRequestResponse;

    private ShuffleJobStatus createStatus() {
        return new ShuffleJobStatus();
    }

    /**
     * Attempts to find or create a playlist with the given name. If 2 or more playlists already exist with the provided
     * name, then this function will update the job status to being {@link EJobStatus#SKIPPED}. If exactly 1 playlist
     * exists with the provided name, then it will be returned. Otherwise, a new playlist is created with such a name
     * and returned.
     * @return Null if the provided name is not unique for a user's playlists.
     */
    protected static ShufflePlaylist findOrCreateUniquePlaylistByName(UserLibrary library, String name, String description, JobStatus status) throws FatalRequestResponse {
        var list = library.getPlaylistByName(name, true);
        if (list == null || list.isEmpty()) {
            return library.createPlaylist(name, description);
        }

        if (list.size() == 1) {
            return list.get(0);
        }

        status.setStatus(EJobStatus.SKIPPED);
        status.setMessage(String.format("Multiple playlists exist already with the name '%s'", name));
        return null;
    }

    protected static boolean isUniqueName(JobStatus status, String name, Set<String> usedNames) {
        if (!usedNames.add(name)) {
            status.setStatus(EJobStatus.SKIPPED);
            status.setMessage("Name overlaps with a previous shuffle");
            return false;
        }
        return true;
    }

    protected final ShuffleApi getApi() {
        return api;
    }

    public final String getUserId() {
        return api.getUserId();
    }
}
