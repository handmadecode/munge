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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import groovy.lang.Closure;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.gradle.api.Project;


public class TransformationSetTest extends ProjectDirectoryTest
{
    static private final String SOURCES_DIR = "sources";
    static private final String SOURCE_FILE1 = "src1";
    static private final String SOURCE_FILE2 = "src2";
    static private final String TEMPLATES_DIR = "templates";
    static private final String TEMPLATE_FILE1 = "template1";
    static private final String TEMPLATE_FILE2 = "template2";
    static private final String OUTPUT_DIR = "out";
    static private final String OUTPUT_FILE = "outfile";


    private Path fSourcesDirectory;
    private Path fTemplatesDirectory;


    /**
     * Create a 'sources' and a 'templates' sub-directory in the project directory. Each directory
     * contains two files, 'src1' and 'src2', and 'template1' and 'template2'.
     *
     * @throws IOException  if creating a directory or file fails.
     */
    @Before
    public void createFiles() throws IOException
    {
        fSourcesDirectory = Files.createDirectory(fProjectDirectory.resolve(SOURCES_DIR));
        Files.createFile(fSourcesDirectory.resolve(SOURCE_FILE1));
        Files.createFile(fSourcesDirectory.resolve(SOURCE_FILE2));

        fTemplatesDirectory = Files.createDirectory(fProjectDirectory.resolve(TEMPLATES_DIR));
        Files.createFile(fTemplatesDirectory.resolve(TEMPLATE_FILE1));
        Files.createFile(fTemplatesDirectory.resolve(TEMPLATE_FILE2));
    }


    @Test(expected = NullPointerException.class)
    public void constructorThrowsForNullArgument()
    {
        new TransformationSetImpl(null);
    }


    @Test
    public void getSourceFilesReturnsSingleFile()
    {
        // Given
        TransformationSet aTransformationSet = new TransformationSetImpl(fProject);
        aTransformationSet.source(SOURCES_DIR + File.separator + SOURCE_FILE1);

        // When
        Set<File> aSourceFiles = aTransformationSet.getSourceFiles().getFiles();

        // Then
        assertEquals(1, aSourceFiles.size());
        assertTrue(aSourceFiles.contains(new File(fSourcesDirectory.toFile(), SOURCE_FILE1)));
    }


    @Test
    public void getSourceFilesReturnsAllFilesInDirectory()
    {
        // Given
        TransformationSet aTransformationSet = new TransformationSetImpl(fProject);
        aTransformationSet.source(SOURCES_DIR);

        // When
        Set<File> aSourceFiles = aTransformationSet.getSourceFiles().getFiles();

        // Then
        assertEquals(2, aSourceFiles.size());
        assertTrue(aSourceFiles.contains(new File(fSourcesDirectory.toFile(), SOURCE_FILE1)));
        assertTrue(aSourceFiles.contains(new File(fSourcesDirectory.toFile(), SOURCE_FILE2)));
    }


    @Test
    public void getSourceFilesReturnsMultipleFiles()
    {
        // Given
        TransformationSet aTransformationSet = new TransformationSetImpl(fProject);
        aTransformationSet.sources(
            SOURCES_DIR + File.separator + SOURCE_FILE1,
            SOURCES_DIR + File.separator + SOURCE_FILE2
        );

        // When
        Set<File> aSourceFiles = aTransformationSet.getSourceFiles().getFiles();

        // Then
        assertEquals(2, aSourceFiles.size());
        assertTrue(aSourceFiles.contains(new File(fSourcesDirectory.toFile(), SOURCE_FILE1)));
        assertTrue(aSourceFiles.contains(new File(fSourcesDirectory.toFile(), SOURCE_FILE2)));
    }


    @Test
    public void getSourceFilesReturnsIncludedFilesInDirectory()
    {
        // Given
        TransformationSet aTransformationSet = new TransformationSetImpl(fProject);
        aTransformationSet.sources(SOURCES_DIR, new PatternFilterableClosure("*1", null));

        // When
        Set<File> aSourceFiles = aTransformationSet.getSourceFiles().getFiles();

        // Then
        assertEquals(1, aSourceFiles.size());
        assertTrue(aSourceFiles.contains(new File(fSourcesDirectory.toFile(), SOURCE_FILE1)));
    }


    @Test
    public void getTemplateFilesReturnsSingleFile()
    {
        // Given
        TransformationSet aTransformationSet = new TransformationSetImpl(fProject);
        aTransformationSet.template(TEMPLATES_DIR + File.separator + TEMPLATE_FILE2);

        // When
        Set<File> aTemplateFiles = aTransformationSet.getTemplateFiles().getFiles();

        // Then
        assertEquals(1, aTemplateFiles.size());
        assertTrue(aTemplateFiles.contains(new File(fTemplatesDirectory.toFile(), TEMPLATE_FILE2)));
    }


    @Test
    public void getTemplateFilesReturnsAllFilesInDirectory()
    {
        // Given
        TransformationSet aTransformationSet = new TransformationSetImpl(fProject);
        aTransformationSet.template(TEMPLATES_DIR);

        // When
        Set<File> aTemplateFiles = aTransformationSet.getTemplateFiles().getFiles();

        // Then
        assertEquals(2, aTemplateFiles.size());
        assertTrue(aTemplateFiles.contains(new File(fTemplatesDirectory.toFile(), TEMPLATE_FILE1)));
        assertTrue(aTemplateFiles.contains(new File(fTemplatesDirectory.toFile(), TEMPLATE_FILE2)));
    }


    @Test
    public void getTemplateFilesReturnsMultipleFiles()
    {
        // Given
        TransformationSet aTransformationSet = new TransformationSetImpl(fProject);
        aTransformationSet.templates(
            TEMPLATES_DIR + File.separator + TEMPLATE_FILE2,
            TEMPLATES_DIR + File.separator + TEMPLATE_FILE1
        );

        // When
        Set<File> aTemplateFiles = aTransformationSet.getTemplateFiles().getFiles();

        // Then
        assertEquals(2, aTemplateFiles.size());
        assertTrue(aTemplateFiles.contains(new File(fTemplatesDirectory.toFile(), TEMPLATE_FILE1)));
        assertTrue(aTemplateFiles.contains(new File(fTemplatesDirectory.toFile(), TEMPLATE_FILE2)));
    }


    @Test
    public void getTemplateFilesDoesNotReturnExcludedFilesInDirectory()
    {
        // Given
        TransformationSet aTransformationSet = new TransformationSetImpl(fProject);
        aTransformationSet.templates(TEMPLATES_DIR, new PatternFilterableClosure(null, "*1"));

        // When
        Set<File> aTemplateFiles = aTransformationSet.getTemplateFiles().getFiles();

        // Then
        assertEquals(1, aTemplateFiles.size());
        assertTrue(aTemplateFiles.contains(new File(fTemplatesDirectory.toFile(), TEMPLATE_FILE2)));
    }


    @Test
    public void getOutputFileReturnsValueFromSetter()
    {
        // Given
        TransformationSet aTransformationSet = new TransformationSetImpl(fProject);
        aTransformationSet.setOutputFile(OUTPUT_DIR + File.separator + OUTPUT_FILE);

        // When
        File aFile = aTransformationSet.getOutputFile();

        // Then
        File aOutputDir = new File(fProjectDirectory.toFile(), OUTPUT_DIR);
        assertEquals(new File(aOutputDir, OUTPUT_FILE), aFile);

        // When
        aTransformationSet.setOutputFile(null);

        // Then
        assertNull(aTransformationSet.getOutputFile());
    }


    @Test
    public void getOutputDirectoryReturnsValueFromSetter()
    {
        // Given
        TransformationSet aTransformationSet = new TransformationSetImpl(fProject);
        aTransformationSet.setOutputDir(OUTPUT_DIR);

        // When
        File aDirectory = aTransformationSet.getOutputDir();

        // Then
        assertEquals(new File(fProjectDirectory.toFile(), OUTPUT_DIR), aDirectory);

        // When
        aTransformationSet.setOutputDir(null);

        // Then
        assertNull(aTransformationSet.getOutputDir());
    }


    @Test
    public void getMappedOutputFilesReturnsEmptyListWhenNothingIsSpecified()
    {
        // Given
        TransformationSet aTransformationSet = new TransformationSetImpl(fProject);
        aTransformationSet.source(SOURCES_DIR + File.separator + SOURCE_FILE1);
        aTransformationSet.source(SOURCES_DIR + File.separator + SOURCE_FILE2);
        aTransformationSet.template(TEMPLATES_DIR + File.separator + TEMPLATE_FILE1);

        // When
        assertTrue(aTransformationSet.getMappedOutputFiles().isEmpty());
    }


    @Test
    public void getMappedOutputFilesReturnsFileInOutputDirectoryWhenSpecified()
    {
        // Given
        TransformationSet aTransformationSet = new TransformationSetImpl(fProject);
        aTransformationSet.source(SOURCES_DIR + File.separator + SOURCE_FILE1);
        aTransformationSet.source(SOURCES_DIR + File.separator + SOURCE_FILE2);
        aTransformationSet.template(TEMPLATES_DIR + File.separator + TEMPLATE_FILE1);
        aTransformationSet.setOutputDir(OUTPUT_DIR);
        File aOutputDir = new File(fProjectDirectory.toFile(), OUTPUT_DIR);

        // When
        List<File> aFiles = aTransformationSet.getMappedOutputFiles();

        // Then
        assertEquals(2, aFiles.size());
        assertEquals(new File(aOutputDir, SOURCE_FILE1), aFiles.get(0));
        assertEquals(new File(aOutputDir, SOURCE_FILE2), aFiles.get(1));
    }


    @Test
    public void getMappedOutputFilesReturnsOutputFileWhenSpecified()
    {
        // Given
        TransformationSet aTransformationSet = new TransformationSetImpl(fProject);
        aTransformationSet.source(SOURCES_DIR + File.separator + SOURCE_FILE1);
        aTransformationSet.source(SOURCES_DIR + File.separator + SOURCE_FILE2);
        aTransformationSet.template(TEMPLATES_DIR + File.separator + TEMPLATE_FILE1);
        aTransformationSet.setOutputFile(OUTPUT_FILE);
        File aOutputFile = new File(fProjectDirectory.toFile(), OUTPUT_FILE);

        // When
        List<File> aFiles = aTransformationSet.getMappedOutputFiles();

        // Then
        assertEquals(2, aFiles.size());
        assertEquals(aOutputFile, aFiles.get(0));
        assertEquals(aOutputFile, aFiles.get(1));
    }


    @Test
    public void getMappedOutputFilesReturnsFilesFromMapping()
    {
        // Given
        TransformationSet aTransformationSet = new TransformationSetImpl(fProject);
        aTransformationSet.source(SOURCES_DIR + File.separator + SOURCE_FILE1);
        aTransformationSet.source(SOURCES_DIR + File.separator + SOURCE_FILE2);
        aTransformationSet.template(TEMPLATES_DIR + File.separator + TEMPLATE_FILE1);
        aTransformationSet.outputMapping(new OutputMapper());

        // When
        List<File> aFiles = aTransformationSet.getMappedOutputFiles();

        // Then
        assertEquals(2, aFiles.size());
        assertEquals(
            new File(fProjectDirectory.toFile(), OutputMapper.map(SOURCE_FILE1, TEMPLATE_FILE1)),
            aFiles.get(0));
        assertEquals(
            new File(fProjectDirectory.toFile(), OutputMapper.map(SOURCE_FILE2, TEMPLATE_FILE1)),
            aFiles.get(1));
    }


    @Test
    public void getParametersReturnsAddedParameters()
    {
        // Given
        String aKey1 = "k1", aKey2 = "k2";
        String aValue1 = "k2", aValue2 = "v2";
        TransformationSet aTransformationSet = new TransformationSetImpl(fProject);

        // Then
        assertTrue(aTransformationSet.getParameters().isEmpty());

        // When
        aTransformationSet.parameter(aKey1, aKey2);
        aTransformationSet.parameters(Collections.singletonMap(aKey2, aValue2));

        // Then
        Map<String, Object> aParams = aTransformationSet.getParameters();
        assertEquals(2, aParams.size());
        assertEquals(aValue1, aParams.get(aKey1));
        assertEquals(aValue2, aParams.get(aKey2));
    }


    @Test
    public void getDynamicOutputDirectoriesReturnsAddedDirectories()
    {
        // Given
        String aDir1 = "dyndir1", aDir2 = "dyndir2";
        TransformationSet aTransformationSet = new TransformationSetImpl(fProject);

        // Then
        assertTrue(aTransformationSet.getDynamicOutputDirectories().isEmpty());

        // When
        aTransformationSet.dynamicOutputDirectory(aDir1);
        aTransformationSet.dynamicOutputDirectory(aDir2);

        // Then
        Set<File> aDirectories = aTransformationSet.getDynamicOutputDirectories().getFiles();
        assertEquals(2, aDirectories.size());
        assertTrue(aDirectories.contains(new File(fProjectDirectory.toFile(), aDir1)));
        assertTrue(aDirectories.contains(new File(fProjectDirectory.toFile(), aDir2)));
    }


    static private class TransformationSetImpl extends TransformationSet
    {
        TransformationSetImpl(Project pProject)
        {
            super(pProject);
        }

        @Override
        public int transform()
        {
            return 0;
        }
    }


    static private class OutputMapper extends Closure<String>
    {
        OutputMapper()
        {
            super(null);
        }

        @Override
        public String call(Object... args)
        {
            if (args.length >= 2)
                return map(((File) args[0]).getName(), ((File) args[1]).getName());
            else
                return null;
        }

        static String map(String pInputFileName, String pTemplateFileName)
        {
            return "out-" + pInputFileName + "-" + pTemplateFileName;
        }
    }
}
