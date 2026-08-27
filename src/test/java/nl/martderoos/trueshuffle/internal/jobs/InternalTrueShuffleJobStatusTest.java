package nl.martderoos.trueshuffle.internal.jobs;

import nl.martderoos.trueshuffle.jobs.ETrueShuffleJobStatus;
import nl.martderoos.trueshuffle.jobs.InternalTrueShuffleJobStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class InternalTrueShuffleJobStatusTest {
    @Test
    public void testStatusCannotBeNull() {
        assertThrows(NullPointerException.class, () -> new InternalTrueShuffleJobStatus(null, null));
    }

    @Test
    public void testStatusStoresFailure() {
        var failure = new IllegalStateException("failure");
        var status = new InternalTrueShuffleJobStatus(ETrueShuffleJobStatus.WAITING, "waiting");

        status.setStatusMessage(ETrueShuffleJobStatus.TERMINATED, "terminated", failure);

        assertEquals(ETrueShuffleJobStatus.TERMINATED, status.getStatus());
        assertEquals("terminated", status.getMessage());
        assertSame(failure, status.getFailure());
    }

    @Test
    public void testTerminalStatusCannotBeOverwritten() {
        var failure = new IllegalStateException("failure");
        var status = new InternalTrueShuffleJobStatus(ETrueShuffleJobStatus.TERMINATED, "terminated");

        status.setStatusMessage(ETrueShuffleJobStatus.COMPLETED, "completed", failure);

        assertEquals(ETrueShuffleJobStatus.TERMINATED, status.getStatus());
        assertEquals("terminated", status.getMessage());
        assertNull(status.getFailure());
    }
}
