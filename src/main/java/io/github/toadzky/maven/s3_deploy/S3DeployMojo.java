package io.github.toadzky.maven.s3_deploy;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.amazonaws.auth.AWSCredentialsProviderChain;
import com.amazonaws.auth.EC2ContainerCredentialsProviderWrapper;
import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.auth.SystemPropertiesCredentialsProvider;
import com.amazonaws.auth.WebIdentityTokenCredentialsProvider;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.profile.path.AwsProfileFileLocationProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import java.util.List;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Goal which touches a timestamp file.
 *
 * @goal touch
 * @phase process-sources
 */
@Mojo(
        name = "upload",
        defaultPhase = LifecyclePhase.DEPLOY
)
public class S3DeployMojo extends AbstractMojo {

    @Parameter
    private Aws aws;
    @Parameter
    private List<UploadableArtifact> artifacts;

    public void execute() throws MojoExecutionException {
        try {
            getLog().debug("creating s3 client. aws-config=" + aws);
            final AmazonS3 s3 = createClient();

            for (UploadableArtifact artifact : artifacts) {
                if (!"s3".equals(artifact.getDestination().getScheme())) {
                    throw new MojoExecutionException("Artifact URI must use S3 scheme");
                }
                final String bucketName = artifact.getDestination().getHost();
                // trim off the leading slash or it makes a root folder of `/` in the s3 console
                final String itemPath = artifact.getDestination().getPath().substring(1);
                getLog().debug(String.format(
                        "uploading artifact. file=%s bucket=%s path=%s",
                        artifact.getSource().getName(), bucketName, itemPath
                ));
                final PutObjectRequest request = new PutObjectRequest(
                        bucketName, itemPath, artifact.getSource()
                );
                if (artifact.getMetadata() != null && !artifact.getMetadata().isEmpty()) {
                    final ObjectMetadata metadata = new ObjectMetadata();
                    metadata.setUserMetadata(artifact.getMetadata());
                    request.setMetadata(metadata);
                }

                request.setGeneralProgressListener(
                        new ProgressUpdater(getLog(), artifact.getSource().getName(), (double) artifact.getSource().length())
                );

                s3.putObject(request);
            }
        } catch (Exception ex) {
            throw new MojoExecutionException("Deploying artifacts to S3 failed.", ex);
        }
    }

    private AmazonS3 createClient() {
        final AmazonS3ClientBuilder builder = AmazonS3ClientBuilder.standard().withRegion(aws.getRegion());
        if (aws.getProfile() != null) {
            if (!AwsProfileFileLocationProvider.DEFAULT_CONFIG_LOCATION_PROVIDER.getLocation().exists()) {
                getLog().warn(
                        "AWS config file not found in default locations. May have problems using profile: " + aws.getProfile()
                );
            }
            builder.setCredentials(
                    new AWSCredentialsProviderChain(
                            new EnvironmentVariableCredentialsProvider(),
                            new SystemPropertiesCredentialsProvider(),
                            WebIdentityTokenCredentialsProvider.create(),
                            new ProfileCredentialsProvider(aws.getProfile()),
                            new EC2ContainerCredentialsProviderWrapper()
                    )
            );
        }
        return builder.build();
    }
}
