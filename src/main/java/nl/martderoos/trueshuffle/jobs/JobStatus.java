package nl.martderoos.trueshuffle.jobs;

import java.util.Objects;

public class JobStatus {
    private EJobStatus status = EJobStatus.WAITING;
    private String message;

    JobStatus() {

    }

    public JobStatus(EJobStatus status, String message) {
        this.status = Objects.requireNonNull(status);
        this.message = message;
    }

    public EJobStatus getStatus() {
        return status;
    }

    void setStatus(EJobStatus status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    void setMessage(String message) {
        this.message = message;
    }
}
