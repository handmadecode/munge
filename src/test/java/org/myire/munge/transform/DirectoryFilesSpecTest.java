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


public class DirectoryFilesSpecTest extends ProjectDirectoryTest
{
    @Test
    public void allFilesAreResolvedWhenNoPatternsAreSpecified() throws IOException
    {
        // Given
        Path aSubDirectory = Files.createDirectory(fProjectDirectory.resolve("sub"));
        Path aFile1 = Files.createFile(aSubDirectory.resolve("f1"));
        Path aFile2 = Files.createFile(aSubDirectory.resolve("f2"));
        DirectoryFilesSpec aSpec = new DirectoryFilesSpec(aSubDirectory.getFileName().toString());

        // When
        Set<File> aFiles = aSpec.resolve(fProject).getFiles();

        // Then
        assertEquals(2, aFiles.size());
        assertTrue(aFiles.contains(aFile1.toFile()));
        assertTrue(aFiles.contains(aFile2.toFile()));
    }


    @Test
    public void onlyFilesMatchingIncludePatternAreResolved() throws IOException
    {
        // Given
        Path aSubDirectory = Files.createDirectory(fProjectDirectory.resolve("sub"));
        Path aMatchingFile = Files.createFile(aSubDirectory.resolve("match"));
        Files.createFile(aSubDirectory.resolve("mismatch1"));
        Files.createFile(aSubDirectory.resolve("mismatch2"));
        DirectoryFilesSpec aSpec = new DirectoryFilesSpec(aSubDirectory.getFileName().toString());
        aSpec.include("match*");

        // When
        Set<File> aFiles = aSpec.resolve(fProject).getFiles();

        // Then
        assertEquals(1, aFiles.size());
        assertTrue(aFiles.contains(aMatchingFile.toFile()));
    }


    @Test
    public void filesMatchingExcludePatternAreNotResolved() throws IOException
    {
        // Given
        Path aSubDirectory = Files.createDirectory(fProjectDirectory.resolve("sub"));
        Path aMatchingFile = Files.createFile(aSubDirectory.resolve("match"));
        Files.createFile(aSubDirectory.resolve("mismatch1"));
        Files.createFile(aSubDirectory.resolve("mismatch2"));
        DirectoryFilesSpec aSpec = new DirectoryFilesSpec(aSubDirectory.getFileName().toString());
        aSpec.exclude("mis*");

        // When
        Set<File> aFiles = aSpec.resolve(fProject).getFiles();

        // Then
        assertEquals(1, aFiles.size());
        assertTrue(aFiles.contains(aMatchingFile.toFile()));
    }
}
