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

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Configures a mock Maven repository to be served by the Mock Repository Manager.
 * <p>
 * This annotation is used as an element of {@link MockRepositoryManager#mockRepos()}.
 * The {@link #source()} directory is scanned for POM files; companion artifacts are derived
 * automatically from the POM coordinates and the directory layout.
 * </p>
 * <p>
 * Optionally, the source directory can be cloned to {@link #cloneTo()} before the server
 * starts, which is useful when the test needs an isolated copy to avoid state pollution.
 * </p>
 *
 * @since 2.0
 * @see MockRepositoryManager#mockRepos()
 */
@Documented
@Target({})
@Retention(RetentionPolicy.RUNTIME)
public @interface MockRepo {

    /**
     * Path to the directory containing the mock repository content.
     * Relative paths are resolved against the working directory.
     *
     * @return path to the mock repository source directory
     */
    String source();

    /**
     * If set, the {@link #source()} directory will be cloned to this path before the server starts.
     * Relative paths are resolved against the working directory.
     * Defaults to an empty string (no cloning).
     *
     * @return path to the clone target directory, or empty string to skip cloning
     */
    String cloneTo() default "";

    /**
     * When {@link #cloneTo()} is set, controls whether the clone target directory is cleaned
     * before copying. Defaults to {@code false}.
     *
     * @return {@code true} to clean the clone target before each run
     */
    boolean cloneClean() default false;

    /**
     * Controls whether directory content is archived lazily (on first access) or eagerly at startup.
     * Defaults to {@code true} (lazy).
     *
     * @return {@code true} to archive directory content on demand
     */
    boolean lazyArchiver() default true;
}
