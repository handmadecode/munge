/*
 * Copyright 2021 Peter Franzen. All rights reserved.
 *
 * Licensed under the Apache License v2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
package org.myire.munge.transform.freemarker;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;

import freemarker.ext.dom.NodeModel;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.utility.NullWriter;

import org.gradle.api.file.FileCollection;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;

import org.myire.munge.transform.ParsedFile;
import org.myire.munge.transform.Transformer;
import org.myire.munge.transform.OutputFileSpec;


/**
 * A {@code Transformer} that transforms XML files by applying FreeMarker templates.
 */
public class FreeMarkerTransformer implements Transformer
{
    private final Logger fLogger = Logging.getLogger("org.myire.munge");

    // The data model that will hold all transformation parameters and the contents of the XML
    // document to transform.
    private final Map<String, Object> fDataModel = new HashMap<>();

    // The FreeMarker configuration used in all transformations.
    private Configuration fConfiguration;

    // The output file charset.
    private Charset fCharset = StandardCharsets.UTF_8;

    // Parsed XML source files and FreeMarker template files.
    private final List<ParsedFile<NodeModel>> fParsedSourceFiles = new ArrayList<>();
    private final List<ParsedFile<Template>> fParsedTemplateFiles = new ArrayList<>();


    @Override
    public boolean configure(File pConfigurationFile)
    {
        // Use version 2.3.0 as the base configuration version to allow all 2.3.x FreeMarker
        // versions to be used.
        fConfiguration = new Configuration(Configuration.VERSION_2_3_0);

        // Add the '<@outputfile>' directive to the configuration.
        fConfiguration.setSharedVariable("outputfile", new OutputFileDirective());

        // Load the configuration file if specified.
        if (pConfigurationFile != null)
        {
            fLogger.info("Configuring FreeMarker from file '{}'", pConfigurationFile);
            try (InputStream aStream = Files.newInputStream(pConfigurationFile.toPath()))
            {
                fConfiguration.setSettings(aStream);
                return true;
            }
            catch (IOException | TemplateException e)
            {
                // Log the error and return false to signal the load failure.
                fLogger.error(
                    "Failed to load FreeMarker configuration file '{}', cannot execute transformations ({})",
                    pConfigurationFile.getAbsolutePath(),
                    e.getMessage());
                fLogger.debug("Configuration file load stacktrace", e);
                return false;
            }
        }
        else
            // No configuration file, stick with the default configuration.
            return true;
    }


    @Override
    public void setTransformationParameters(Map<String, Object> pParameters)
    {
        // Put all transformation parameters into the data model used for all transformations.
        fDataModel.putAll(pParameters);
    }


    @Override
    public void setOutputFileCharset(Charset pCharset)
    {
        if (pCharset != null)
            fCharset = pCharset;
    }


    @Override
    public int loadSources(FileCollection pSourceFiles)
    {
        int aNumErrors = 0;
        for (File aSourceFile : pSourceFiles)
        {
            try
            {
                // Parse the XML file into a NodeModel and create an association between the file
                // and the parse result.
                NodeModel aNodeModel = NodeModel.parse(aSourceFile);
                fParsedSourceFiles.add(new ParsedFile<>(aSourceFile, aNodeModel));
            }
            catch (ParserConfigurationException | SAXException | IOException e)
            {
                aNumErrors++;
                fLogger.error(
                    "Failed to load source file '{}', skipping ({})",
                    aSourceFile.getAbsolutePath(),
                    e.getMessage());
                fLogger.debug("Source file load stacktrace", e);
            }
        }

        return aNumErrors;
    }


    @Override
    public int loadTemplates(FileCollection pTemplateFiles)
    {
        int aNumErrors = 0;
        for (File aTemplateFile : pTemplateFiles)
        {
            try
            {
                // Load and parse the template file and create an association between the file and
                // the parsed template.
                fConfiguration.setDirectoryForTemplateLoading(aTemplateFile.getParentFile());
                Template aTemplate = fConfiguration.getTemplate(aTemplateFile.getName());
                fParsedTemplateFiles.add(new ParsedFile<>(aTemplateFile, aTemplate));
            }
            catch (IOException e)
            {
                aNumErrors++;
                fLogger.error(
                    "Failed to load FreeMarker template file '{}', skipping ({})",
                    aTemplateFile.getAbsolutePath(),
                    e.getMessage());
                fLogger.debug("FreeMarker template file load stacktrace", e);
            }
        }

        return aNumErrors;
    }


    @Override
    public int executeTransformations(BiFunction<File, File, OutputFileSpec> pOutputFileMapping)
    {
        int aNumFailures = 0;

        // Transform each source/template file pair.
        for (ParsedFile<NodeModel> aSource : fParsedSourceFiles)
        {
            // Put the name of the source file and its parsed contents into the data model (which
            // already contains the transformation parameters).
            fDataModel.put("sourceName", aSource.getFile().getName());
            fDataModel.put("doc", aSource.getContents());

            // Apply each template to the source file.
            for (ParsedFile<Template> aTemplate : fParsedTemplateFiles)
            {
                // Put the name of the template file into the data model.
                fDataModel.put("templateName", aTemplate.getFile().getName());

                // Map the source and template file pair to an output file.
                OutputFileSpec aOutputFile = pOutputFileMapping.apply(aSource.getFile(), aTemplate.getFile());

                // Log the transformation that will be executed.
                logTransformation(aSource.getFile(), aTemplate.getFile(), aOutputFile);

                // Let the parsed template process the data model and write the result to the output
                // file.
                if (!processDataModel(aTemplate.getContents(), aOutputFile))
                    aNumFailures++;
            }
        }

        return aNumFailures;
    }


    /**
     * Process this transformer's data model with a template and write the generated output to a
     * file.
     *
     * @param pTemplate     The template to process the data model with.
     * @param pOutputFile   The file to write the output to, or null to let the template direct the
     *                      output.
     *
     * @throws NullPointerException if {@code pTemplate} is null.
     */
    private boolean processDataModel(Template pTemplate, OutputFileSpec pOutputFile)
    {
        try (Writer aWriter = createWriter(pOutputFile))
        {
            pTemplate.process(fDataModel, aWriter);
            return true;
        }
        catch (IOException | TemplateException e)
        {
            fLogger.error("Freemarker template processing error: {}", e.getMessage());
            fLogger.debug("Template processing stacktrace", e);
            return false;
        }
    }


    /**
     * Create a {@code Writer} instance for a file. The writer will use the charset specified by
     * {@link #setOutputFileCharset(Charset)} or, if no charset has been specified, the default
     * charset UTF-8.
     *
     * @param pFile The file, or null to create a {@code Writer} that discards everything.
     *
     * @return  A new {@code Writer}, never null.
     *
     * @throws IOException  if creating or opening the file fails.
     */
    private Writer createWriter(OutputFileSpec pFile) throws IOException
    {
        if (pFile != null)
            return new OutputStreamWriter(pFile.open(), fCharset);
        else
            return NullWriter.INSTANCE;
    }
}
