/*
 * Copyright 2019 Peter Franzen. All rights reserved.
 *
 * Licensed under the Apache License v2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
package org.myire.munge.transform.saxon;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.transform.stream.StreamSource;

import org.gradle.api.Project;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.Optional;

import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmAtomicValue;
import net.sf.saxon.s9api.XsltCompiler;
import net.sf.saxon.s9api.XsltExecutable;
import net.sf.saxon.s9api.XsltTransformer;

import org.myire.munge.transform.ParsedFile;
import org.myire.munge.transform.TransformationSet;


/**
 * A transformation set that applies XSLT style sheets to XML source files using the Saxon
 * processor.
 *
 * @author <a href="mailto:peter@myire.org">Peter Franzen</a>
 */
public class SaxonTransformationSet extends TransformationSet
{
    private File fConfigurationFile;
    private int fNumTransformationErrors;


    /**
     * Create a new {@code SaxonTransformationSet}.
     *
     * @param pProject  The project to resolve file paths with.
     *
     * @throws NullPointerException if {@code pProject} is null.
     */
    public SaxonTransformationSet(Project pProject)
    {
        super(pProject);
    }


    /**
     * Get the file to configure Saxon with before executing the transformations.
     *
     * @return  The Saxon configuration file, or null to use the default configuration.
     */
    @InputFile
    @Optional
    public File getConfigurationFile()
    {
        return fConfigurationFile;
    }


    public void setConfigurationFile(Object pFile)
    {
        fConfigurationFile = pFile != null ? fProject.file(pFile) : null;
    }


    @Override
    public int transform()
    {
        fNumTransformationErrors = 0;

        Processor aProcessor = createProcessor();
        if (aProcessor == null)
            // Invalid configuration file, cannot continue with the transformations, return one
            // error.
            return 1;

        // Load the stylesheet files
        List<ParsedFile<XsltTransformer>> aStylesheets = loadStylesheets(aProcessor);

        // All output directed to the global output file is appended; it must be deleted before the
        // first transformation.
        deleteOutputFileIfExists();

        // Transform each source/template file pair.
        for (File aSourceFile : getSourceFiles())
        {
            for (ParsedFile<XsltTransformer> aStylesheet : aStylesheets)
            {
                // Map the source and template file pair to an output file path.
                File aOutputFile = mapToOutputFile(aSourceFile, aStylesheet.getFile());

                // Execute the transformation.
                logTransformation(aSourceFile, aStylesheet.getFile(), aOutputFile);
                executeTransformation(aProcessor, aStylesheet.getContents(), aSourceFile, aOutputFile);
            }
        }

        return fNumTransformationErrors;
    }


    /**
     * Create a new {@code Processor} and configure it from the configuration file if one is
     * specified.
     *
     * @return  A new {@code Processor}. If loading a configuration file fails then null is
     *          returned.
     */
    private Processor createProcessor()
    {
        if (fConfigurationFile == null)
            // No configuration file, use a Processor that doesn't require licensed features.
            return new Processor(false);

        try
        {
            return new Processor(new StreamSource(fConfigurationFile));
        }
        catch (SaxonApiException e)
        {
            // Log the error and return a null processor to signal the load failure.
            fProject.getLogger().error(
                "Failed to load Saxon configuration file '{}', cannot execute transformations ({})",
                fConfigurationFile.getAbsolutePath(),
                e.getMessage());
            fProject.getLogger().debug("Configuration file load stacktrace", e);
            return null;
        }
    }


    /**
     * Load and compile the specified stylesheet files.
     *
     * @param pProcessor    The processor to create the XSLT compiler with.
     *
     * @return  The loaded and compiled stylesheet files. The returned list may be empty but will
     *          never be null.
     *
     * @throws NullPointerException if {@code pProcessor} is null.
     */
    private List<ParsedFile<XsltTransformer>> loadStylesheets(Processor pProcessor)
    {
        List<ParsedFile<XsltTransformer>> aLoadedFiles = new ArrayList<>();

        XsltCompiler aCompiler = pProcessor.newXsltCompiler();
        for (File aXsltFile : getTemplateFiles())
        {
            try
            {
                // Create an XsltTransformer from the XSLT file.
                XsltExecutable aXsltExecutable = aCompiler.compile(new StreamSource(aXsltFile));
                XsltTransformer aXsltTransformer = aXsltExecutable.load();

                // Put the transformation set parameters into the XsltTransformer.
                setParameters(aXsltTransformer);

                aLoadedFiles.add(new ParsedFile<>(aXsltFile, aXsltTransformer));
            }
            catch (SaxonApiException e)
            {
                fNumTransformationErrors++;
                fProject.getLogger().error("Failed to load XSLT file '{}', skipping ({})",
                                           aXsltFile.getAbsolutePath(),
                                           e.getMessage());
                fProject.getLogger().debug("XSLT file load stacktrace", e);
            }
        }

        return aLoadedFiles;
    }


    /**
     * Put the transformation set parameters into an {@code XsltTransformer}.
     *
     * @param pXsltTransformer  The transformer.
     *
     * @throws NullPointerException if {@code pXsltTransformer } is null.
     */
    private void setParameters(XsltTransformer pXsltTransformer)
    {
        for (Map.Entry<String, Object> aParameter : getParameters().entrySet())
        {
            pXsltTransformer.setParameter(
                new QName(aParameter.getKey()),
                XdmAtomicValue.makeAtomicValue(aParameter.getValue()));
        }
    }

    /**
     * Execute an XSL transformation.
     *
     * @param pProcessor    The processor to create the destination with..
     * @param pTransformer  The transformer.
     * @param pSourceFile   The XML source file.
     * @param pOutputFile   The file destination for the transformation's output.
     *
     * @throws NullPointerException if any of the parameters is null.
     */
    private void executeTransformation(
        Processor pProcessor,
        XsltTransformer pTransformer,
        File pSourceFile,
        File pOutputFile)
    {
        try (OutputStream aOutputStream = createOutputStream(pOutputFile))
        {
            pTransformer.setSource(new StreamSource(pSourceFile));
            pTransformer.setDestination(pProcessor.newSerializer(aOutputStream));
            pTransformer.transform();
        }
        catch (IOException | SaxonApiException e)
        {
            fNumTransformationErrors++;
            fProject.getLogger().error(
                "Failed to transform file {} ({})",
                pSourceFile.getAbsolutePath(),
                e.getMessage());
            fProject.getLogger().debug("XSLT processing stacktrace", e);
        }
    }


    /**
     * Create an {@code OutputStream} instance for a file.
     *
     * @param pFile The file, or null to create a {@code OutputStream} that discards everything.
     *
     * @return  A new {@code OutputStream}, never null.
     *
     * @throws IOException  if creating or opening the file fails.
     */
    private OutputStream createOutputStream(File pFile) throws IOException
    {
        if (pFile == null)
            return NullOutputStream.INSTANCE;

        // Make sure all parent directories exist before any attempt to create the file.
        Path aPath = pFile.toPath();
        Files.createDirectories(aPath.getParent());

        if (pFile.equals(getOutputFile()))
            // Writing to the global output file is always done in append mode.
            return Files.newOutputStream(aPath, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        else
            // Other output files are truncated and overwritten of they exist.
            return Files.newOutputStream(aPath);
    }


    /**
     * An {@code OutputStream} that discards everything written to it.
     */
    static private class NullOutputStream extends OutputStream
    {
        static final NullOutputStream INSTANCE = new NullOutputStream();

        @Override
        public void write(int b)
        {
            // Discard.
        }

        @Override
        public void write(byte[] b)
        {
            // Discard.
        }

        @Override
        public void write(byte[] b, int off, int len)
        {
            // Discard.
        }
    }
}
