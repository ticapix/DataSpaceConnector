/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.transfer.demo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.eclipse.dataspaceconnector.provision.aws.AwsTemporarySecretToken;
import org.eclipse.dataspaceconnector.schema.s3.S3BucketSchema;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.eclipse.dataspaceconnector.spi.transfer.flow.DataFlowController;
import org.eclipse.dataspaceconnector.spi.transfer.flow.DataFlowInitiateResponse;
import org.eclipse.dataspaceconnector.spi.transfer.response.ResponseStatus;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.jetbrains.annotations.NotNull;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.io.InputStream;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Objects;

public class DemoS3FlowController implements DataFlowController {
    private final Vault vault;
    private final Monitor monitor;
    private final RetryPolicy<Object> retryPolicy;

    public DemoS3FlowController(Vault vault, Monitor monitor) {
        this.vault = vault;
        this.monitor = monitor;
        retryPolicy = new RetryPolicy<>()
                .withBackoff(500, 5000, ChronoUnit.MILLIS)
                .withMaxRetries(3);
    }

    @Override
    public boolean canHandle(DataRequest dataRequest) {
        return true;
    }

    @Override
    public @NotNull DataFlowInitiateResponse initiateFlow(DataRequest dataRequest) {

        var awsSecretName = dataRequest.getDataDestination().getKeyName();
        var awsSecret = vault.resolveSecret(awsSecretName);
        var bucketName = dataRequest.getDataDestination().getProperty(S3BucketSchema.BUCKET_NAME);

        var region = dataRequest.getDataDestination().getProperty(S3BucketSchema.REGION);
        var dt = convertSecret(awsSecret);

        return copyToBucket(bucketName, region, dt);

    }

    @NotNull
    private DataFlowInitiateResponse copyToBucket(String bucketName, String region, AwsTemporarySecretToken dt) {


        try (S3Client s3 = S3Client.builder()
                .credentialsProvider(StaticCredentialsProvider.create(AwsSessionCredentials.create(dt.getAccessKeyId(), dt.getSecretAccessKey(), dt.getSessionToken())))
                .region(Region.of(region))
                .build()) {

            String etag = null;
            PutObjectRequest request = createRequest(bucketName, "demo-image");
            PutObjectRequest completionMarker = createRequest(bucketName, "asdf.complete");

            try {
                monitor.debug("Data request: begin transfer...");
                var response = Failsafe.with(retryPolicy).get(() -> s3.putObject(request, RequestBody.fromBytes(createRandomContent())));
                var response2 = Failsafe.with(retryPolicy).get(() -> s3.putObject(completionMarker, RequestBody.empty()));
                monitor.debug("Data request done.");
                etag = response.eTag();
            } catch (S3Exception tmpEx) {
                monitor.info("Data request: transfer not successful");
            }

            return new DataFlowInitiateResponse(ResponseStatus.OK, etag);
        } catch (S3Exception | EdcException ex) {
            monitor.severe("Data request: transfer failed!");
            return new DataFlowInitiateResponse(ResponseStatus.FATAL_ERROR, ex.getLocalizedMessage());
        }
    }

    private AwsTemporarySecretToken convertSecret(String awsSecret) {
        try {
            var mapper = new ObjectMapper();
            return mapper.readValue(awsSecret, AwsTemporarySecretToken.class);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;
    }

    private PutObjectRequest createRequest(String bucketName, String objectKey) {
        return PutObjectRequest.builder()
                .bucket(bucketName)
                .metadata(Map.of("name", "demo_image.jpg"))
                .key(objectKey)
                .build();
    }

    private byte[] createRandomContent() {
        InputStream resourceAsStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("demo_image.jpg");
        try {
            return Objects.requireNonNull(resourceAsStream).readAllBytes();
        } catch (IOException e) {
            throw new EdcException(e);
        }
    }
}

