package org.codehaus.mojo.mrm.jupiter;

/*
 * Copyright 2011 Stephen Connolly
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * A handle to a running Mock Repository Manager server.
 */
public class MockRepositoryManagerServer {

    private static final String SETTINGS_TEMPLATE_RESOURCE = "/org/codehaus/mojo/mrm/jupiter/settings.xml";

    private static final String SETTINGS_URL_TOKEN = "${mrm.repository.url}";

    private final String url;

    private final int port;

    private Path settingsFile;

    MockRepositoryManagerServer(String url, int port) {
        this.url = url;
        this.port = port;
    }

    /**
     * Returns the base URL of the running Mock Repository Manager server.
     *
     * @return the server URL, e.g. {@code http://localhost:8080}
     */
    public String getUrl() {
        return url;
    }

    /**
     * Returns the port on which the Mock Repository Manager server is listening.
     *
     * @return the bound port number
     */
    public int getPort() {
        return port;
    }

    /**
     * Returns the path to a generated temporary Maven {@code settings.xml} file configured to use this server.
     * The file is created lazily on first call and reused afterwards for this server handle.
     *
     * @return absolute path to the generated Maven settings file
     */
    public synchronized String getSettingsFile() {
        if (settingsFile == null) {
            settingsFile = createSettingsFile();
        }
        return settingsFile.toAbsolutePath().toString();
    }

    synchronized void cleanup() {
        if (settingsFile != null) {
            try {
                Files.deleteIfExists(settingsFile);
            } catch (IOException cleanupError) { // best-effort cleanup; deleteOnExit fallback avoids leaking temp files
                settingsFile.toFile().deleteOnExit();
            }
            settingsFile = null;
        }
    }

    private Path createSettingsFile() {
        try {
            Path file = Files.createTempFile("mrm-settings-", ".xml");
            file.toFile().deleteOnExit();
            String settingsContent = readSettingsTemplate().replace(SETTINGS_URL_TOKEN, url);
            Files.write(file, settingsContent.getBytes(StandardCharsets.UTF_8));
            return file;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create temporary Maven settings.xml", e);
        }
    }

    private String readSettingsTemplate() throws IOException {
        try (InputStream in = MockRepositoryManagerServer.class.getResourceAsStream(SETTINGS_TEMPLATE_RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException("Missing classpath resource: " + SETTINGS_TEMPLATE_RESOURCE);
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        }
    }
}
