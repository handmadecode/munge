/*
 * Copyright 2021 Peter Franzen. All rights reserved.
 *
 * Licensed under the Apache License v2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
package org.myire.munge.transform.saxon;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import javax.xml.transform.stream.StreamSource;

import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmAtomicValue;
import net.sf.saxon.s9api.XdmValue;
import net.sf.saxon.s9api.XsltCompiler;
import net.sf.saxon.s9api.XsltExecutable;
import net.sf.saxon.s9api.XsltTransformer;

import org.gradle.api.file.FileCollection;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;

import org.myire.munge.transform.ParsedFile;
import org.myire.munge.transform.Transformer;
import org.myire.munge.transform.OutputFileSpec;


/**
 * A {@code Transformer} that applies transforms XML files by applying XSL stylesheets using the
 * Saxon library.
 */
public class SaxonTransformer implements Transformer
{
    private final Logger fLogger = Logging.getLogger("org.myire.munge");

    // The configured Saxon processor.
    private Processor fProcessor;

    // The XML source files.
    private Collection<File> fSourceFiles;

    // The parsed XSL stylesheet files.
    private final List<ParsedFile<XsltTransformer>> fLoadedStylesheetFiles = new ArrayList<>();

    // Parameters to be used in all transformations
    private final Map<QName, XdmValue> fXsltParameters = new HashMap<>();


    @Override
    public boolean configure(File pConfigurationFile)
    {
        if (pConfigurationFile != null)
        {
            try
            {
                fProcessor = new Processor(new StreamSource(pConfigurationFile));
                return true;
            }
            catch (SaxonApiException e)
            {
                // Log the error and return false to signal the configuration load failure.
                fLogger.error(
                    "Failed to load Saxon configuration file '{}', cannot execute transformations ({})",
                    pConfigurationFile.getAbsolutePath(),
                    e.getMessage());
                fLogger.debug("Configuration file load stacktrace", e);
                return false;
            }
        }
        else
        {
            // No configuration file, use a Processor that doesn't require licensed features.
            fProcessor = new Processor(false);
            return true;
        }
    }


    @Override
    public void setTransformationParameters(Map<String, Object> pParameters)
    {
        for (Map.Entry<String, Object> aParameter : pParameters.entrySet())
            fXsltParameters.put(
                new QName(aParameter.getKey()),
                XdmAtomicValue.makeAtomicValue(aParameter.getValue()));
    }


    @Override
    public void setOutputFileCharset(Charset pCharset)
    {
        // Ignore, the output charset is determined by the XSL transformation.
    }


    @Override
    public int loadSources(FileCollection pSourceFiles)
    {
        // The XML source files are not parsed before the transformation, simply store their
        // specifications.
        fSourceFiles = pSourceFiles.getFiles();
        return 0;
    }


    @Override
    public int loadTemplates(FileCollection pTemplateFiles)
    {
        int aNumErrors = 0;

        // Compile each XSLT file.
        XsltCompiler aCompiler = fProcessor.newXsltCompiler();
        for (File aXsltFile : pTemplateFiles)
        {
            try
            {
                // Create an XsltTransformer from the XSLT file.
                XsltExecutable aXsltExecutable = aCompiler.compile(new StreamSource(aXsltFile));
                XsltTransformer aXsltTransformer = aXsltExecutable.load();

                // Put the transformation set parameters into the XsltTransformer.
                for (Map.Entry<QName, XdmValue> aParameter : fXsltParameters.entrySet())
                    aXsltTransformer.setParameter(aParameter.getKey(), aParameter.getValue());

                // Create an association between the XSL file and the parsed stylesheet.
                fLoadedStylesheetFiles.add(new ParsedFile<>(aXsltFile, aXsltTransformer));
            }
            catch (SaxonApiException e)
            {
                aNumErrors++;
                fLogger.error(
                    "Failed to load XSLT file '{}', skipping ({})",
                    aXsltFile.getAbsolutePath(),
                    e.getMessage());
                fLogger.debug("XSLT file load stacktrace", e);
            }
        }

        return aNumErrors;
    }


    @Override
    public int executeTransformations(BiFunction<File, File, OutputFileSpec> pOutputFileMapping)
    {
        int aNumErrors = 0;

        // Transform each source/template file pair.
        for (File aSourceFile : fSourceFiles)
        {
            for (ParsedFile<XsltTransformer> aStylesheet : fLoadedStylesheetFiles)
            {
                // Map the source and template file pair to an output file path.
                OutputFileSpec aOutputFile = pOutputFileMapping.apply(aSourceFile, aStylesheet.getFile());

                // Log the transformation that will be executed.
                logTransformation(aSourceFile, aStylesheet.getFile(), aOutputFile);

                // Execute the transformation.
                if (!executeTransformation(aStylesheet.getContents(), aSourceFile, aOutputFile))
                    aNumErrors++;
            }
        }

        return aNumErrors;
    }


    /**
     * Execute an XSL transformation.
     *
     * @param pTransformer  The XSLT transformer.
     * @param pSourceFile   The XML source file.
     * @param pOutputFile   The file destination for the transformation's output.
     *
     * @throws NullPointerException if any of the parameters is null.
     */
    private boolean executeTransformation(
        XsltTransformer pTransformer,
        File pSourceFile,
        OutputFileSpec pOutputFile)
    {
        try (OutputStream aOutputStream = createOutputStream(pOutputFile))
        {
            pTransformer.setSource(new StreamSource(pSourceFile));
            pTransformer.setDestination(fProcessor.newSerializer(aOutputStream));
            pTransformer.transform();
            return true;
        }
        catch (IOException | SaxonApiException e)
        {
            fLogger.error(
                "Failed to transform file {} ({})",
                pSourceFile.getAbsolutePath(),
                e.getMessage());
            fLogger.debug("XSLT processing stacktrace", e);
            return false;
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
    static private OutputStream createOutputStream(OutputFileSpec pFile) throws IOException
    {
        if (pFile != null)
            return pFile.open();
        else
            return NullOutputStream.INSTANCE;
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
