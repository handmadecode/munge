/*
 * Copyright 2019 Peter Franzen. All rights reserved.
 *
 * Licensed under the Apache License v2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
package org.myire.munge.transform;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static java.util.Objects.requireNonNull;

import groovy.lang.Closure;

import org.gradle.api.Project;
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectories;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.OutputFiles;
import org.gradle.util.ConfigureUtil;


/**
 * A {@code TransformationSet} transforms source files by applying templates loaded from template
 * files. The result is written to output files.
 *<p>
 * Source and template files can be specified either as paths to files or directories, or as a
 * path to a directory with a configuration closure, which operates on a {@code PatternFilterable}.
 * Relative paths are resolved relative to the project directory.
 *<p>
 * The output file for a transformation can be specified in several ways:
 *<ul>
 * <li> The output can be directed to a single file, specified with {@link #setOutputFile(Object)}.
 * If the transformation set contains several transformations, the effect of specifying a single
 * output file is implementation specific.</li>
 * <li>The output files can be created in an specific directory, specified with
 * {@link #setOutputDir(Object)}. The name of the output file will be the same as the name of
 * the transformation's source file.</li>
 * <li>The output file can be specified by a {@code Closure} applied to the source and template file
 * paths of the transformation. The {@code Closure} either returns an object specifying the output
 * file of the transformation or null if there should be no explicit output file. In the former case
 * the returned object is resolved with {@code Project::file()}.</li>
 *</ul>
 * The above variants can be combined. Multiple closures can be added to the output file
 * specification, and they will be called in the order they were added until one returns a non-null
 * value. If all closures return null, the directory or file variant will be used. Should both an
 * output directory and an output file be specified, the former takes precedence.
 *<p>
 * It is possible to pass parameters to the transformations. This is done by calling
 * {@link #parameter(String, Object)}.
 *<p>
 * Example of a configuration closure for a transformation set:
 *<pre>
 *     {
 *         source 'src.xml'
 *         sources ('srcDir')
 *         {
 *           include '*.xml'
 *         }
 *
 *         templates 'a.xsl', 'rsrc/b.xsl'
 *         templates ('templatesDir')
 *         {
 *           exclude 'common.xsl'
 *         }
 *
 *         outputDir = 'dst'
 *         outputMapping {
 *           s, t -&gt;
 *             if (s.name.startsWith('xyz'))
 *               'dst/' + s.name + '-' + t.name
 *             else
 *               null
 *         }
 *
 *         parameters(
 *           'stringParam': 'value',
 *           'intParam': 17
 *         )
 *     }
 *</pre>
 *
 * @author <a href="mailto:peter@myire.org">Peter Franzen</a>
 */
abstract public class TransformationSet
{
    protected final Project fProject;

    private final FilesSpec fSourceFilesSpec = new FilesSpec();
    private final FilesSpec fTemplateFilesSpec = new FilesSpec();

    private File fOutputFile;
    private File fOutputDirectory;
    private final List<Closure<?>> fOutputMappings = new ArrayList<>();

    private final Map<String, Object> fParameters = new HashMap<>();

    private FileCollection fSourceFiles;
    private FileCollection fTemplateFiles;
    private final List<Object> fDynamicOutputDirectories = new ArrayList<>();


    /**
     * Create a new {@code TransformationSet}.
     *
     * @param pProject  The project to resolve file paths with.
     *
     * @throws NullPointerException if {@code pProject} is null.
     */
    protected TransformationSet(Project pProject)
    {
        fProject = requireNonNull(pProject);
    }


    /**
     * Get the source files that each template file will be applied to.
     *
     * @return  The source files, never null.
     */
    @InputFiles
    public FileCollection getSourceFiles()
    {
        if (fSourceFiles == null)
            fSourceFiles = fSourceFilesSpec.resolve(fProject);

        return fSourceFiles;
    }


    /**
     * Get the template files that will be applied to each source.
     *
     * @return  The template files, never null.
     */
    @InputFiles
    public FileCollection getTemplateFiles()
    {
        if (fTemplateFiles == null)
            fTemplateFiles = fTemplateFilesSpec.resolve(fProject);

        return fTemplateFiles;
    }


    @OutputFile
    @Optional
    public File getOutputFile()
    {
        return fOutputFile;
    }


    @OutputDirectory
    @Optional
    public File getOutputDir()
    {
        return fOutputDirectory;
    }


    /**
     * Get the output file specifications produced by applying all output mappings to the pairs of
     * source and template files.
     *<p>
     * This property is read-only and intended to be used by the Gradle up-to-date check.
     *
     * @return  The file specifications from the output mappings, possibly empty, never null.
     */
    @OutputFiles
    public List<File> getMappedOutputFiles()
    {
        List<File> aFiles = new ArrayList<>();
        for (File aSourceFile : getSourceFiles())
        {
            for (File aTemplateFile : getTemplateFiles())
            {
                File aOutputFile = mapToOutputFile(aSourceFile, aTemplateFile);
                if (aOutputFile != null)
                    aFiles.add(aOutputFile);
            }
        }

        return aFiles;
    }


    /**
     * Get the directories specified as dynamic output directory. A dynamic output directory is a
     * directory where a template creates files without an explicit configuration in the
     * transformation set. These directories need to be marked as output directories for the
     * up-to-date checks to work properly.
     *
     * @return  The dynamic output directories, possibly empty, never null.
     */
    @OutputDirectories
    public FileCollection getDynamicOutputDirectories()
    {
        return fProject.files(fDynamicOutputDirectories.toArray());
    }


    /**
     * Get the parameters specified for the transformations.
     *
     * @return  The parameters, possibly empty, never null.
     */
    @Input
    public Map<String, Object> getParameters()
    {
        return fParameters;
    }


    /**
     * Add a source file or directory to the transformation set. If the path specifies a directory,
     * all of its files, including files in any subdirectories, will be added as source files.
     *
     * @param pPath An object that resolves to a file or directory. See the documentation of
     *              {@code org.gradle.api.Project::file()} for the possible types that can be
     *              resolved.
     *
     * @throws NullPointerException if {@code pPath} is null.
     */
    public void source(Object pPath)
    {
        fSourceFilesSpec.add(pPath);
    }


    /**
     * Add multiple source files and/or directories to the transformation set.
     *
     * @param pPaths    An array of objects that resolve to files and/or directories. See the
     *                  documentation of {@code org.gradle.api.Project::files()} for the possible
     *                  types that can be resolved.
     *
     * @throws NullPointerException if {@code pPaths} is null.
     */
    public void sources(Object... pPaths)
    {
        fSourceFilesSpec.add(pPaths);
    }


    /**
     * Add a source file directory to the transformation set and specify which of its files to
     * include and/or exclude through a {@code Closure}.
     *
     * @param pDirectoryPath    An object that resolves to a directory. See the documentation of
     *                          {@code org.gradle.api.Project::file()} for the possible types that
     *                          can be resolved.
     * @param pConfigClosure    A closure that configures a {@code PatternFilterable}.
     *
     * @throws NullPointerException if any of the parameters is null.
     */
    public void sources(Object pDirectoryPath, Closure<?> pConfigClosure)
    {
        fSourceFilesSpec.add(pDirectoryPath, pConfigClosure);
    }


    /**
     * Add a template file or directory to the transformation set. If the path specifies a
     * directory, all of its files, including files in any subdirectories, will be added as template
     * files.
     *
     * @param pPath An object that resolves to a file or directory. See the documentation of
     *              {@code org.gradle.api.Project::file()} for the possible types that can be
     *              resolved.
     *
     * @throws NullPointerException if {@code pPath} is null.
     */
    public void template(Object pPath)
    {
        fTemplateFilesSpec.add(pPath);
    }


    /**
     * Add multiple template files to the transformation set.
     *
     * @param pPaths    An array of objects that resolve to files and/or directories. See the
     *                  documentation of {@code org.gradle.api.Project::files()} for the possible
     *                  types that can be resolved.
     *
     * @throws NullPointerException if {@code pPaths} is null.
     */
    public void templates(Object... pPaths)
    {
        fTemplateFilesSpec.add(pPaths);
    }


    /**
     * Add a template file directory to the transformation set and specify which of its files to
     * include and/or exclude through a {@code Closure}.
     *
     * @param pDirectoryPath    An object that resolves to a directory. See the documentation of
     *                          {@code org.gradle.api.Project::file()} for the possible types that
     *                          can be resolved.
     * @param pConfigClosure    A closure that configures a {@code PatternFilterable}.
     *
     * @throws NullPointerException if any of the parameters is null.
     */
    public void templates(Object pDirectoryPath, Closure<?> pConfigClosure)
    {
        fTemplateFilesSpec.add(pDirectoryPath, pConfigClosure);
    }


    /**
     * Specify a single file to write all output to for transformations that don't have an explicit
     * output mapping.
     *
     * @param pFilePath An object that resolves to a file. See the documentation of
     *                  {@code org.gradle.api.Project::file()} for the possible types that can be
     *                  resolved. Null means no output file.
     */
    public void setOutputFile(Object pFilePath)
    {
        fOutputFile = pFilePath != null ? fProject.file(pFilePath) : null;
    }


    /**
     * Specify a directory to create output files in for transformations that don't have an explicit
     * output mapping.
     *
     * @param pDirectoryPath    An object that resolves to a directory. See the documentation of
     *                          {@code org.gradle.api.Project::file()} for the possible types that
     *                          can be resolved. Null means no output directory.
     */
    public void setOutputDir(Object pDirectoryPath)
    {
        fOutputDirectory = pDirectoryPath != null ? fProject.file(pDirectoryPath) : null;
    }


    /**
     * Add a closure that maps a source file and a template file to an output file.
     *
     * @param pClosure  The mapping closure.
     *
     * @throws NullPointerException if {@code pClosure } is null.
     */
    public void outputMapping(Closure<?> pClosure)
    {
        fOutputMappings.add(pClosure);
    }


    /**
     * Add a parameter that will be passed to each template when it is applied to a source file.
     *
     * @param pName     The parameter's name.
     * @param pValue    The parameter's value.
     *
     * @throws NullPointerException if {@code pName} is null.
     */
    public void parameter(String pName, Object pValue)
    {
        fParameters.put(requireNonNull(pName), pValue);
    }


    /**
     * Add parameters that will be passed to each template when it is applied to a source file.
     *
     * @param pParameters   The parameters to add.
     *
     * @throws NullPointerException if {@code pParameters} is null.
     */
    public void parameters(Map<String, Object> pParameters)
    {
        fParameters.putAll(pParameters);
    }


    /**
     * Specify a directory where the transformations are known to generate dynamic output.
     *
     * @param pDirectoryPath    An object that resolves to a directory. See the documentation of
     *                          {@code org.gradle.api.Project::file()} for the possible types that
     *                          can be resolved.
     *
     * @throws NullPointerException if {@code pDirectoryPath} is null.
     */
    public void dynamicOutputDirectory(Object pDirectoryPath)
    {
        fDynamicOutputDirectories.add(requireNonNull(pDirectoryPath));
    }


    /**
     * Configure this transformation set through a {@code Closure}.
     *
     * @param pClosure  The configuration closure.
     *
     * @return  This instance.
     */
    public TransformationSet configure(Closure<?> pClosure)
    {
        return ConfigureUtil.configureSelf(pClosure, this);
    }


    /**
     * Execute all transformations in this set. Each template file will be loaded and applied to
     * each source file, and the result will be written to the output file specified by the output
     * mappings or the output directory/file.
     *
     * @return  The number of errors encountered during the transformations.
     */
    abstract public int transform();


    /**
     * Delete the global output file if it has been specified and exists.
     */
    protected void deleteOutputFileIfExists()
    {
        if (fOutputFile != null)
        {
            try
            {
                Files.deleteIfExists(fOutputFile.toPath());
            }
            catch (IOException e)
            {
                fProject.getLogger().warn("Could not delete existing output file '{}'",
                                          fOutputFile.getAbsolutePath(),
                                          e);
            }
        }
    }


    /**
     * Map a source and template file pair to an output file.
     *
     * @param pSourceFile   The source file.
     * @param pTemplateFile The template file.
     *
     * @return  The result from the first output mapping that returns a non-null result. If all
     *          mappings return null (or there are no mappings), a file with the same name as the
     *          source file will be returned, but located in the output directory. Should no
     *          output directory be specified, the output file (which may be null) is returned.
     *
     * @throws NullPointerException if any of the parameters is null.
     */
    protected File mapToOutputFile(File pSourceFile, File pTemplateFile)
    {
        for (Closure<?> aMapping : fOutputMappings)
        {
            Object aResult = aMapping.call(pSourceFile, pTemplateFile);
            if (aResult != null)
                return fProject.file(aResult);
        }

        if (fOutputDirectory != null)
            return new File(fOutputDirectory, pSourceFile.getName());

        return fOutputFile;
    }


    /**
     * Log that a template file is applied to a source file to produce an output file.
     *
     * @param pSourceFile   The source file.
     * @param pTemplateFile The template file.
     * @param pOutputFile   The output file, or null if no explicit output file is used in the
     *                      transformation.
     *
     * @throws NullPointerException if {@code pSourceFile} or {@code pTemplateFile} is null.
     */
    protected void logTransformation(File pSourceFile, File pTemplateFile, File pOutputFile)
    {
        if (pOutputFile != null)
        {
            fProject.getLogger().info(
                "Transforming '{}' into '{}' with '{}'",
                pSourceFile.getAbsolutePath(),
                pOutputFile.getAbsolutePath(),
                pTemplateFile.getAbsolutePath());
        }
        else
        {
            fProject.getLogger().info(
                "Transforming '{}' with '{}'",
                pSourceFile.getAbsolutePath(),
                pTemplateFile.getAbsolutePath());
        }
    }
}
