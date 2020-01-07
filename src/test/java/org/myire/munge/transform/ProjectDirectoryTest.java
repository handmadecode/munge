/*
 * Copyright 2019 Peter Franzen. All rights reserved.
 *
 * Licensed under the Apache License v2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
package org.myire.munge.transform;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.After;
import org.junit.Before;

import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;


/**
 * Base class for unit tests that operate on a Gradle project directory.
 */
public class ProjectDirectoryTest
{
    protected Project fProject;
    protected Path fProjectDirectory;


    @Before
    public void createProject() throws IOException
    {
        fProjectDirectory = Files.createTempDirectory("munge-unit-test").toRealPath();
        fProject = ProjectBuilder.builder().withProjectDir(fProjectDirectory.toFile()).build();
    }


    @After
    public void deleteProjectDirectory()
    {
        deepDelete(fProjectDirectory);
    }


    /**
     * Delete the file system object denoted by a {@code Path}, including any file system objects
     * contained within it.
     *
     * @param pPath The path to the file system object to delete.
     */
    static private void deepDelete(Path pPath)
    {
        try
        {
            if (Files.isDirectory(pPath))
            {
                Files.newDirectoryStream(pPath).forEach(ProjectDirectoryTest::deepDelete);
                Files.delete(pPath);
            }
            else if (Files.isRegularFile(pPath))
                Files.delete(pPath);
        }
        catch (IOException ioe)
        {
            throw new UncheckedIOException(ioe);
        }
    }
}
