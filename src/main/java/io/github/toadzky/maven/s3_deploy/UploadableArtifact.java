package io.github.toadzky.maven.s3_deploy;

import java.io.File;
import java.net.URI;
import java.util.Map;
import lombok.Data;
import org.apache.maven.plugins.annotations.Parameter;

@Data
public class UploadableArtifact {

    @Parameter
    private File source;
    @Parameter
    private URI destination;
    @Parameter
    private Map<String, String> metadata;

}
