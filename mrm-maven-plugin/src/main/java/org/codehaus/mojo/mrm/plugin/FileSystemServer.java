package org.codehaus.mojo.mrm.plugin;

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

import org.codehaus.mojo.mrm.api.FileSystem;

/**
 * Compatibility wrapper around the shared {@link org.codehaus.mojo.mrm.servlet.FileSystemServer}.
 */
public class FileSystemServer extends org.codehaus.mojo.mrm.servlet.FileSystemServer {

    /**
     * Creates a new file system server that will serve a {@link FileSystem} over HTTP on the specified port.
     *
     * @param name        The name of the file system server thread.
     * @param port        The port to serve on or <code>0</code> to pick a random, but available, port.
     * @param contextPath The root context path for server
     * @param fileSystem  the file system to serve.
     * @param debugServer the server debug mode
     */
    public FileSystemServer(String name, int port, String contextPath, FileSystem fileSystem, boolean debugServer) {
        super(name, port, contextPath, fileSystem, debugServer);
    }
}
