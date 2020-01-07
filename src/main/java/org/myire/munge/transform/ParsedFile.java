/*
 * Copyright 2019 Peter Franzen. All rights reserved.
 *
 * Licensed under the Apache License v2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
package org.myire.munge.transform;

import java.io.File;
import static java.util.Objects.requireNonNull;


/**
 * Data class mapping a file to its parsed contents.
 *
 * @param <T>   The type representing the parsed contents.

 * @author <a href="mailto:peter@myire.org">Peter Franzen</a>
 */
public class ParsedFile<T>
{
    private final File fFile;
    private final T fContents;


    /**
     * Create a new {@code ParsedFile}.
     *
     * @param pFile     The file.
     * @param pContents The file's contents.
     *
     * @throws NullPointerException if any of the parameters is null.
     */
    public ParsedFile(File pFile, T pContents)
    {
        fFile = requireNonNull(pFile);
        fContents = requireNonNull(pContents);
    }


    public File getFile()
    {
        return fFile;
    }


    public T getContents()
    {
        return fContents;
    }
}
