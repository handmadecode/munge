/*
 * Copyright 2019 Peter Franzen. All rights reserved.
 *
 * Licensed under the Apache License v2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
package org.myire.munge.transform;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


public class FilesSpecTest extends ProjectDirectoryTest
{
    @Test
    public void addedFilesAreResolved() throws IOException
    {
        // Given
        Path aFile1 = Files.createFile(fProjectDirectory.resolve("f1"));
        Path aFile2 = Files.createFile(fProjectDirectory.resolve("f2"));
        FilesSpec aSpec = new FilesSpec();

        // When
        aSpec.add(aFile1.getFileName().toString(), aFile2.getFileName().toString());
        Set<File> aFiles = aSpec.resolve(fProject).getFiles();

        // Then
        assertEquals(2, aFiles.size());
        assertTrue(aFiles.contains(aFile1.toFile()));
        assertTrue(aFiles.contains(aFile2.toFile()));
    }


    @Test
    public void allFilesInAddedDirectoryAreResolved() throws IOException
    {
        // Given
        // Given
        Path aSubDirectory = Files.createDirectory(fProjectDirectory.resolve("sub"));
        Path aFile1 = Files.createFile(aSubDirectory.resolve("f1"));
        Path aFile2 = Files.createFile(aSubDirectory.resolve("f2"));
        FilesSpec aSpec = new FilesSpec();

        // When
        aSpec.add(aSubDirectory.getFileName().toString());
        Set<File> aFiles = aSpec.resolve(fProject).getFiles();

        // Then
        assertEquals(2, aFiles.size());
        assertTrue(aFiles.contains(aFile1.toFile()));
        assertTrue(aFiles.contains(aFile2.toFile()));
    }


    @Test
    public void onlyFilesMatchingDirectoryIncludeSpecAreResolved() throws IOException
    {
        // Given
        Path aSubDirectory = Files.createDirectory(fProjectDirectory.resolve("sub"));
        Path aMatchingFile1 = Files.createFile(aSubDirectory.resolve("match1"));
        Path aMatchingFile2 = Files.createFile(aSubDirectory.resolve("match2"));
        Files.createFile(aSubDirectory.resolve("mismatch1"));
        Files.createFile(aSubDirectory.resolve("mismatch2"));
        FilesSpec aSpec = new FilesSpec();
        aSpec.add(aSubDirectory.getFileName().toString(), new PatternFilterableClosure("match*", null));

        // When
        Set<File> aFiles = aSpec.resolve(fProject).getFiles();

        // Then
        assertEquals(2, aFiles.size());
        assertTrue(aFiles.contains(aMatchingFile1.toFile()));
        assertTrue(aFiles.contains(aMatchingFile2.toFile()));
    }


    @Test
    public void filesMatchingDirectoryExcludeSpecAreNotResolved() throws IOException
    {
        // Given
        Path aSubDirectory = Files.createDirectory(fProjectDirectory.resolve("sub"));
        Path aMatchingFile1 = Files.createFile(aSubDirectory.resolve("match1"));
        Path aMatchingFile2 = Files.createFile(aSubDirectory.resolve("match2"));
        Files.createFile(aSubDirectory.resolve("mismatch1"));
        Files.createFile(aSubDirectory.resolve("mismatch2"));
        FilesSpec aSpec = new FilesSpec();
        aSpec.add(aSubDirectory.getFileName().toString(), new PatternFilterableClosure("*match*", "mis*"));

        // When
        Set<File> aFiles = aSpec.resolve(fProject).getFiles();

        // Then
        assertEquals(2, aFiles.size());
        assertTrue(aFiles.contains(aMatchingFile1.toFile()));
        assertTrue(aFiles.contains(aMatchingFile2.toFile()));
    }

}
