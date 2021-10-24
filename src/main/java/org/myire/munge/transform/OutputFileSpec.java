/*
 * Copyright 2021 Peter Franzen. All rights reserved.
 *
 * Licensed under the Apache License v2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
package org.myire.munge.transform;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import static java.util.Objects.requireNonNull;


/**
 * An {@code OutputFileSpec} associates a {@code File} with a set of {@code OpenOption} to use when
 * opening the file for writing.
 */
public class OutputFileSpec
{
    private final File fFile;
    private final OpenOption[] fOpenOptions;


    /**
     * Create a new {@code OutputFileSpec}.
     *
     * @param pFile         The file specification.
     * @param pOpenOptions  The options to specify when opening the file for writing.
     *
     * @throws NullPointerException if any of the parameters is null.
     */
    public OutputFileSpec(File pFile, OpenOption... pOpenOptions)
    {
        fFile = requireNonNull(pFile);
        fOpenOptions = requireNonNull(pOpenOptions);
    }


    /**
     * Get the file that will be opened in calls to {@link #open()}.
     *
     * @return  The underlying {@code File}, never null.
     */
    public File getFile()
    {
        return fFile;
    }


    /**
     * Open the file for writing with the options specified in the constructor. Any parent
     * directories that don't exist will be created first.
     *
     * @return  An {@code OutputStream} for the opened file. The caller is responsible for closing
     *          this stream.
     *
     * @throws IOException  if opening the file or creating a parent directory fails.
     */
    public OutputStream open() throws IOException
    {
        Path aPath = fFile.toPath();
        Files.createDirectories(aPath.getParent());
        return Files.newOutputStream(aPath, fOpenOptions);
    }
}
