package io.github.toadzky.maven.s3_deploy;

import lombok.Data;
import org.apache.maven.plugins.annotations.Parameter;

@Data
public class Aws {

    @Parameter(property = "s3deploy.aws.profile")
    private String profile;
    @Parameter(property = "s3deploy.aws.region")
    private String region;
}
