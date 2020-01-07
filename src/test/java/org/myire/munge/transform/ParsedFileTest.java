/*
 * Copyright 2019 Peter Franzen. All rights reserved.
 *
 * Licensed under the Apache License v2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
package org.myire.munge.transform;

import java.io.File;

import org.junit.Test;
import static org.junit.Assert.assertEquals;


public class ParsedFileTest
{
    @Test(expected = NullPointerException.class)
    public void constructorThrowsForNullFile()
    {
        // When
        new ParsedFile<>(null, "");
    }


    @Test(expected = NullPointerException.class)
    public void constructorThrowsForNullContents()
    {
        // When
        new ParsedFile<>(new File(""), null);
    }


    @Test
    public void getFileReturnsValuePassedToConstructor()
    {
        // Given
        File aFile = new File("xx");
        ParsedFile<String> aParsedFile = new ParsedFile<>(aFile, "");

        // When
        File aReturnedFile = aParsedFile.getFile();

        // Then
        assertEquals(aFile, aReturnedFile);
    }


    @Test
    public void getContentsReturnsValuePassedToConstructor()
    {
        // Given
        String aContents = "meh";
        ParsedFile<String> aParsedFile = new ParsedFile<>(new File("xx"), aContents);

        // When
        String aReturnedContents = aParsedFile.getContents();

        // Then
        assertEquals(aContents, aReturnedContents);
    }
}
