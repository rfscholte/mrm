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

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifies that a mock repository whose artifact is stored as a directory named {@code <artifact>.jar}
 * (exploded JAR) is served correctly without a {@code NoSuchArchiverException}.
 */
@MockRepositoryManager(mockRepos = @MockRepo(source = "src/test/resources/directory-jar-repo"), port = 0)
class DirectoryJarMockRepoTest {

    @Test
    void directoryNamedJarIsServedAsJar(MockRepositoryManagerServer server) throws IOException {
        String jarUrl = server.getUrl() + "/localhost/mrm-dir-jar/1.0/mrm-dir-jar-1.0.jar";
        HttpURLConnection connection = (HttpURLConnection) new URL(jarUrl).openConnection();
        connection.setRequestMethod("GET");
        assertEquals(200, connection.getResponseCode(), "Expected HTTP 200 for directory-backed JAR artifact");
        try (InputStream in = connection.getInputStream()) {
            // JAR/ZIP files start with the PK magic bytes 0x50 0x4B
            byte[] header = in.readNBytes(2);
            assertEquals(0x50, header[0] & 0xFF, "Expected PK zip/jar magic byte 0");
            assertEquals(0x4B, header[1] & 0xFF, "Expected PK zip/jar magic byte 1");
        }
    }
}
