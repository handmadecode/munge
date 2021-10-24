/*
 * Copyright 2021 Peter Franzen. All rights reserved.
 *
 * Licensed under the Apache License v2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
package org.myire.munge.transform;

import java.io.File;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.function.BiFunction;

import org.gradle.api.file.FileCollection;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;


/**
 * A {@code Transformer} executes transformations by applying templates to source files and writing
 * the result to output files.
 */
public interface Transformer
{
    /**
     * Configure this transformer with the values in a configuration file.
     *
     * @param pConfigurationFile    The configuration file. If this parameter is null, a default
     *                              configuration will be used.
     *
     * @return  True if the transformer was successfully configured, false if a failure occurred. In
     *          the latter case an error has been logged.
     */
    boolean configure(File pConfigurationFile);

    /**
     * Set the parameters to be used in all transformations.
     *
     * @param pParameters   The transformation parameters.
     *
     * @throws NullPointerException if {@code pParameters} is null.
     */
    void setTransformationParameters(Map<String, Object> pParameters);

    /**
     * Set the character set to encode the output files with.
     *<p>
     * Note that all implementations may not honor this setting.
     *
     * @param pCharset  The output file character set.
     */
    void setOutputFileCharset(Charset pCharset);

    /**
     * Load the source files to transform in the next call to
     * {@link #executeTransformations(BiFunction)}.
     *
     * @param pSourceFiles  The source files to load.
     *
     * @return  The number of source files that couldn't be loaded and for which an error was
     *          logged.
     */
    int loadSources(FileCollection pSourceFiles);

    /**
     * Load the template files to transform source files with in the next call to
     * {@link #executeTransformations(BiFunction)}.
     *
     * @param pTemplateFiles    The template files to load.
     *
     * @return  The number of template files that couldn't be loaded and for which an error was
     *          logged.
     */
    int loadTemplates(FileCollection pTemplateFiles);

    /**
     * Apply all successfully loaded template files to all successfully loaded source files.
     *
     * @param pOutputFileMapping    A function that returns the output file for a source/template
     *                              file combination.
     *
     * @return  The number of errors encountered during the transformations.
     *
     * @throws NullPointerException if {@code pOutputFileMapping} is null.
     */
    int executeTransformations(BiFunction<File, File, OutputFileSpec> pOutputFileMapping);


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
    default void logTransformation(File pSourceFile, File pTemplateFile, OutputFileSpec pOutputFile)
    {
        Logger aLogger = Logging.getLogger("org.myire.munge");

        if (pOutputFile != null)
        {
            aLogger.info(
                "Transforming '{}' into '{}' with template '{}'",
                pSourceFile.getAbsolutePath(),
                pOutputFile.getFile().getAbsolutePath(),
                pTemplateFile.getAbsolutePath());
        }
        else
        {
            aLogger.info(
                "Applying template '{}' to '{}'",
                pTemplateFile.getAbsolutePath(),
                pSourceFile.getAbsolutePath());
        }
    }
}
