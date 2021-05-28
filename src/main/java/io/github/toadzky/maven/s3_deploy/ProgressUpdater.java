package io.github.toadzky.maven.s3_deploy;

import com.amazonaws.event.ProgressEvent;
import com.amazonaws.event.ProgressListener;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.apache.maven.plugin.logging.Log;

@RequiredArgsConstructor
public class ProgressUpdater implements ProgressListener {

    private final Log log;
    private final String fileName;
    private final double totalBytes;

    private long uploaded;
    private Instant nextUpdateTime;

    @Override
    public void progressChanged(ProgressEvent event) {
        switch (event.getEventType()) {
            case TRANSFER_STARTED_EVENT:
                log.info("Started uploading " + fileName);
                nextUpdateTime = Instant.now().plusSeconds(5);
                break;
            case TRANSFER_COMPLETED_EVENT:
                log.info("Upload complete.");
                break;
            case REQUEST_BYTE_TRANSFER_EVENT:
                uploaded += event.getBytes();
                double percentage = uploaded / totalBytes * 100;
                if (nextUpdateTime.isBefore(Instant.now())) {
                    log.info("Uploading... " + (int) percentage + "% complete");
                    nextUpdateTime = Instant.now().plusSeconds(10);
                }
                break;
            default:
                log.debug("not handling event. type=" + event.getEventType());
        }
    }
}
