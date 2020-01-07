/*
 * Copyright 2019 Peter Franzen. All rights reserved.
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
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import org.gradle.api.Project;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.Optional;

import freemarker.ext.dom.NodeModel;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.utility.NullWriter;

import org.myire.munge.transform.ParsedFile;
import org.myire.munge.transform.TransformationSet;


/**
 * A transformation set that applies FreeMarker template files to XML source files.
 *
 * @author <a href="mailto:peter@myire.org">Peter Franzen</a>
 */
public class FreeMarkerTransformationSet extends TransformationSet
{
    private File fConfigurationFile;
    private Charset fCharset = StandardCharsets.UTF_8;
    private int fNumTransformationErrors;


    /**
     * Create a new {@code FreeMarkerTransformationSet}.
     *
     * @param pProject  The project to resolve file paths with.
     *
     * @throws NullPointerException if {@code pProject} is null.
     */
    public FreeMarkerTransformationSet(Project pProject)
    {
        super(pProject);
    }


    /**
     * Get the file to configure FreeMarker with before executing the transformations.
     *
     * @return  The FreeMarker configuration file, or null to use the default configuration.
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


    /**
     * Get the name of the charset to encode the output files with.
     *
     * @return  The charset, never null
     */
    @Input
    public String getCharset()
    {
        return fCharset.name();
    }


    public void setCharset(String pCharset)
    {
        fCharset = Charset.forName(pCharset);
    }


    @Override
    public int transform()
    {
        fNumTransformationErrors = 0;

        // Create the configuration to use for each transformation.
        Configuration aConfig = createConfiguration();
        if (aConfig == null)
            // Invalid configuration file, cannot continue with the transformations, return one
            // error.
            return 1;

        // Load the source and template files.
        List<ParsedFile<NodeModel>> aSources = loadSources();
        List<ParsedFile<Template>> aTemplates = loadTemplates(aConfig);

        // All output directed to the global output file is appended; it must be deleted before the
        // first transformation.
        deleteOutputFileIfExists();

        // Create the map to use as data model in the transformations and put all transformation
        // parameters into it.
        Map<String, Object> aDataModel = new HashMap<>(getParameters());

        // Transform each source/template file pair.
        for (ParsedFile<NodeModel> aSource : aSources)
        {
            aDataModel.put("sourceName", aSource.getFile().getName());
            aDataModel.put("doc", aSource.getContents());

            for (ParsedFile<Template> aTemplate : aTemplates)
            {
                aDataModel.put("templateName", aTemplate.getFile().getName());

                // Map the source and template file pair to an output file path.
                File aOutputFile = mapToOutputFile(aSource.getFile(), aTemplate.getFile());

                // Execute the transformation.
                logTransformation(aSource.getFile(), aTemplate.getFile(), aOutputFile);
                execute(aTemplate.getContents(), aDataModel, aOutputFile);
            }
        }

        return fNumTransformationErrors;
    }


    /**
     * Create the FreeMarker configuration and populate it from the configuration file if one is
     * specified.
     *
     * @return  A new configuration instance. If loading a configuration file fails then null is
     *          returned.
     */
    private Configuration createConfiguration()
    {
        // Use the latest known version of the configuration.
        Configuration aConfig = new Configuration(Configuration.VERSION_2_3_29);

        // Add the '<@outputfile>' directive to the configuration.
        aConfig.setSharedVariable("outputfile", new OutputFileDirective());

        // Load the configuration file if specified.
        if (fConfigurationFile != null)
        {
            fProject.getLogger().info("Configuring FreeMarker from file '{}'", fConfigurationFile);
            try (InputStream aStream = Files.newInputStream(fConfigurationFile.toPath()))
            {
                aConfig.setSettings(aStream);
            }
            catch (IOException | TemplateException e)
            {
                // Log the error and return a null configuration to signal the load failure.
                fProject.getLogger().error(
                    "Failed to load FreeMarker configuration file '{}', cannot execute transformations ({})",
                    fConfigurationFile.getAbsolutePath(),
                    e.getMessage());
                fProject.getLogger().debug("Configuration file load stacktrace", e);
                aConfig = null;
            }
        }

        return aConfig;
    }


    /**
     * Load each specified source file and parse its contents into a {@code NodeModel}. If a file
     * cannot be loaded the error is logged and the file is skipped, but the loading continues.
     *
     * @return  The loaded and parsed source files. The returned list may be empty but will never be
     *          null.
     */
    private List<ParsedFile<NodeModel>> loadSources()
    {
        List<ParsedFile<NodeModel>> aParsedFiles = new ArrayList<>();
        for (File aSourceFile : getSourceFiles())
        {
            try
            {
                NodeModel aNodeModel = NodeModel.parse(aSourceFile);
                aParsedFiles.add(new ParsedFile<>(aSourceFile, aNodeModel));
            }
            catch (ParserConfigurationException | SAXException | IOException e)
            {
                fNumTransformationErrors++;
                fProject.getLogger().error("Failed to load source file '{}', skipping ({})",
                                           aSourceFile.getAbsolutePath(),
                                           e.getMessage());
                fProject.getLogger().debug("Source file load stacktrace", e);
            }
        }

        return aParsedFiles;
    }


    /**
     * Load each specified template file as a {@code Template} instance. If a file cannot be loaded
     * the error is logged and the file is skipped, but the loading continues.
     *
     * @param pConfiguration    The configuration instance to load the template files with.
     *
     * @return  The loaded files and their contents. The returned list may be empty but will never
     *          be null.
     *
     * @throws NullPointerException if {@code pConfiguration} is null.
     */
    private List<ParsedFile<Template>> loadTemplates(Configuration pConfiguration)
    {
        List<ParsedFile<Template>> aParsedFiles = new ArrayList<>();
        for (File aTemplateFile : getTemplateFiles())
        {
            try
            {
                pConfiguration.setDirectoryForTemplateLoading(aTemplateFile.getParentFile());
                Template aTemplate = pConfiguration.getTemplate(aTemplateFile.getName());
                aParsedFiles.add(new ParsedFile<>(aTemplateFile, aTemplate));
            }
            catch (IOException e)
            {
                fNumTransformationErrors++;
                fProject.getLogger().error("Failed to load template file '{}', skipping ({})",
                                           aTemplateFile.getAbsolutePath(),
                                           e.getMessage());
                fProject.getLogger().debug("Template file load stacktrace", e);
            }
        }

        return aParsedFiles;
    }


    /**
     * Execute a template with a data model and write the generated output to a file.
     *
     * @param pTemplate     The template to execute.
     * @param pDataModel    The data model.
     * @param pOutputFile   The file to write the output to, or null to let the template direct the
     *                      output.
     *
     * @throws NullPointerException if {@code pTemplate} or {@code pDataModel} is null.
     */
    private void execute(Template pTemplate, Map<String, Object> pDataModel, File pOutputFile)
    {
        try (Writer aWriter = createWriter(pOutputFile))
        {
            pTemplate.process(pDataModel, aWriter);
        }
        catch (IOException | TemplateException e)
        {
            fNumTransformationErrors++;
            fProject.getLogger().error("Freemarker template processing error: {}", e.getMessage());
            fProject.getLogger().debug("Template processing stacktrace", e);
        }
    }


    /**
     * Create a {@code Writer} instance for a file. The writer will use the charset specified by
     * {@link #setCharset(String)} or, if no charset has been specified, the default charset UTF-8.
     *
     * @param pFile The file, or null to create a {@code Writer} that discards everything.
     *
     * @return  A new {@code Writer}, never null.
     *
     * @throws IOException  if creating or opening the file fails.
     */
    private Writer createWriter(File pFile) throws IOException
    {
        if (pFile == null)
            return NullWriter.INSTANCE;

        // Make sure all parent directories exist before any attempt to create the file.
        Path aPath = pFile.toPath();
        Files.createDirectories(aPath.getParent());

        if (pFile.equals(getOutputFile()))
        {
            // Writing to the global output file is always done in append mode.
            return new OutputStreamWriter(
                Files.newOutputStream(aPath, StandardOpenOption.CREATE, StandardOpenOption.APPEND),
                fCharset);
        }
        else
            // Other output files are truncated and overwritten of they exist.
            return new OutputStreamWriter(Files.newOutputStream(aPath), fCharset);
    }
}
