/*
 * Copyright 2019 Peter Franzen. All rights reserved.
 *
 * Licensed under the Apache License v2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
package org.myire.munge.transform;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import static java.util.Objects.requireNonNull;

import groovy.lang.Closure;

import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.util.PatternFilterable;
import org.gradle.util.ClosureBackedAction;


/**
 * A specification of a collection of files. The files are specified as resolvable objects similarly
 * to {@code org.gradle.api.Project::files()}, and are lazily resolved when
 * {@link #resolve(Project)} is called.
 *<p>
 * Adding a directory to a {@code FilesSpec} will add all of the files in the directory and any
 * subdirectories to the file collection.
 *
 * @author <a href="mailto:peter@myire.org">Peter Franzen</a>
 */
class FilesSpec
{
    private final Set<Object> fPaths = new LinkedHashSet<>();
    private final List<DirectoryFilesSpec> fDirectorySpecs = new ArrayList<>();


    /**
     * Resolve the files added to this specification.
     *
     * @param pProject  The project to resolve the files with.
     *
     * @return  A new {@code FileCollection}, never null.
     *
     * @throws NullPointerException if {@code pProject} is null.
     */
    FileCollection resolve(Project pProject)
    {
        FileCollection aFiles = pProject.files(fPaths.toArray()).getAsFileTree();

        for (DirectoryFilesSpec aDirectorySpec : fDirectorySpecs)
            aFiles = aFiles.plus(aDirectorySpec.resolve(pProject));

        return aFiles;
    }


    /**
     * Add a file or directory to the collection. The path will not be resolved until the next call
     * to {@link #resolve(Project)}.
     *
     * @param pPath An object that resolves to a file or directory. See the documentation of
     *              {@code org.gradle.api.Project::file()} for a list of the types that can be
     *              resolved.
     *
     * @throws NullPointerException if {@code pPath} is null.
     */
    void add(Object pPath)
    {
        fPaths.add(requireNonNull(pPath));
    }


    /**
     * Add multiple files or directories to the collection. The paths will not be resolved until the
     * next call to {@link #resolve(Project)}.
     *
     * @param pPaths    An array of objects that resolve to files and/or directories. See the
     *                  documentation of {@code org.gradle.api.Project::files()} for a list of the
     *                  types that can be resolved.
     *
     * @throws NullPointerException if {@code pPaths} is null.
     */
    void add(Object... pPaths)
    {
        Collections.addAll(fPaths, pPaths);
    }


    /**
     * Add a directory to the collection and specify which of its files to include and/or exclude
     * through a {@code Closure}.
     *
     * @param pDirectoryPath    An object that resolves to a directory. See the documentation of
     *                          {@code org.gradle.api.Project::file()} for a list of the types that
     *                          can be resolved.
     * @param pConfigureClosure A closure that configures a {@code PatternFilterable}.
     *
     * @throws NullPointerException if any of the parameters is null.
     */
    void add(Object pDirectoryPath, Closure<?> pConfigureClosure)
    {
        DirectoryFilesSpec aDirectorySpec = new DirectoryFilesSpec(pDirectoryPath);
        Action<PatternFilterable> aConfigAction = new ClosureBackedAction<>(pConfigureClosure);
        aConfigAction.execute(aDirectorySpec);
        fDirectorySpecs.add(aDirectorySpec);
    }
}
