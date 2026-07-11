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
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@MockRepositoryManager
class MockRepositoryManagerExtensionTest {

    @Test
    void serverIsRunning(MockRepositoryManagerServer server) throws IOException {
        assertNotNull(server.getUrl());
        assertTrue(server.getUrl().startsWith("http://localhost:"), "URL should start with http://localhost:");
        assertTrue(server.getPort() > 0, "Port should be positive");

        Path settingsPath = Path.of(server.getSettingsFile());
        assertTrue(Files.isRegularFile(settingsPath), "Settings file should exist");
        String settings = Files.readString(settingsPath);
        assertTrue(settings.contains(server.getUrl()), "Settings file should contain the server URL");

        assertEquals(
                settingsPath.toAbsolutePath().toString(),
                server.getSettingsFile(),
                "Settings file path should be stable for the same server handle");
    }
}
