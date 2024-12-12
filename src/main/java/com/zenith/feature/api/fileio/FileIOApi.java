package com.zenith.feature.api.fileio;

import com.zenith.feature.api.Api;
import com.zenith.feature.api.fileio.model.FileIOResponse;

import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;

import static com.zenith.Shared.DEFAULT_LOG;
import static com.zenith.Shared.OBJECT_MAPPER;

public class FileIOApi extends Api {
    public static final FileIOApi INSTANCE = new FileIOApi();

    public FileIOApi() {
        super("https://file.io");
    }

    public Optional<FileIOResponse> uploadFile(String fileName, InputStream contents) {

        String boundary = "---------------------------" + System.currentTimeMillis();
        var reqBuilder = buildBaseRequest("/")
            .header("Accept", "application/json")
            .header("Content-Type", "multipart/form-data; boundary=" + boundary)
            .timeout(Duration.ofMinutes(60));
        boundary = "--" + boundary;

        // write form body to a temp file
        // avoids keeping entire body in memory which could cause OOM
        HttpRequest req;
        try {
            Path tempFile = Files.createTempFile("fileio", "form");
            try (var out = Files.newOutputStream(tempFile)) {
                out.write((boundary + "\r\n").getBytes());
                out.write(("Content-Disposition: form-data; name=\"file\"; filename=\"" + fileName + "\"\r\n").getBytes());
                out.write(("Content-Type: application/octet-stream\r\n\r\n").getBytes());
                contents.transferTo(out);
                out.write(("\r\n" + boundary + "\r\n").getBytes());
                out.write(("Content-Disposition: form-data; name=\"expires\"\r\n\r\n1w\r\n").getBytes());
                out.write((boundary + "\r\n").getBytes());
                out.write(("Content-Disposition: form-data; name=\"maxDownloads\"\r\n\r\n1\r\n").getBytes());
                out.write((boundary + "\r\n").getBytes());
                out.write(("Content-Disposition: form-data; name=\"autoDelete\"\r\n\r\ntrue\r\n").getBytes());
                out.write((boundary + "--").getBytes());
            }
            req = reqBuilder
                .POST(HttpRequest.BodyPublishers.ofInputStream(() -> {
                    try {
                        return Files.newInputStream(tempFile);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }))
                .build();
        } catch (final Exception e) {
            DEFAULT_LOG.error("Failed to write form body to temp file", e);
            return Optional.empty();
        }

        try (HttpClient client = buildHttpClient()) {
            var response = client.send(req, HttpResponse.BodyHandlers.ofString());
            var body = response.body();
            try {
                return Optional.of(OBJECT_MAPPER.readValue(body, FileIOResponse.class));
            } catch (final Exception e) {
                DEFAULT_LOG.error("Failed to parse response from file.io: {}", body, e);
                return Optional.empty();
            }
        } catch (final Exception e) {
            DEFAULT_LOG.error("Failed to upload file: {} to file.io", fileName, e);
            return Optional.empty();
        }
    }
}
