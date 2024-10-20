package nl.martderoos.trueshuffle.jobs;

public class ShuffleJobStatus {
    private ShuffleJobPlaylistStatus sourcePlaylist;
    private ShuffleJobPlaylistStatus targetPlaylist;
    private JobStatus jobStatus = new JobStatus();

    ShuffleJobStatus() {

    }

    public ShuffleJobPlaylistStatus getSourcePlaylist() {
        return sourcePlaylist;
    }

    void setSourcePlaylist(ShuffleJobPlaylistStatus sourcePlaylist) {
        this.sourcePlaylist = sourcePlaylist;
    }

    public ShuffleJobPlaylistStatus getTargetPlaylist() {
        return targetPlaylist;
    }

    void setTargetPlaylist(ShuffleJobPlaylistStatus targetPlaylist) {
        this.targetPlaylist = targetPlaylist;
    }

    public JobStatus getJobStatus() {
        return jobStatus;
    }

    void setJobStatus(JobStatus jobStatus) {
        this.jobStatus = jobStatus;
    }
}
