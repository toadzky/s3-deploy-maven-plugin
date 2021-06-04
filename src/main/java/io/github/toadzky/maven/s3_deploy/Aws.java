package io.github.toadzky.maven.s3_deploy;

import lombok.Data;
import org.apache.maven.plugins.annotations.Parameter;

@Data
public class Aws {

    private String profile;
    private String region;
}
