/*
 * Copyright 2019 Peter Franzen. All rights reserved.
 *
 * Licensed under the Apache License v2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
package org.myire.munge.transform;

import java.util.Set;
import static java.util.Objects.requireNonNull;

import groovy.lang.Closure;

import org.gradle.api.Project;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.FileTreeElement;
import org.gradle.api.specs.Spec;
import org.gradle.api.tasks.util.PatternFilterable;
import org.gradle.api.tasks.util.PatternSet;


/**
 * A specification of a collection of files sharing a common base directory. By default all files in
 * the base directory and its subdirectories will be part of the collection. This can be changed by
 * adding include and/or exclude patterns to the specification.
 *<p>
 * The base directory is lazily resolved when {@link #resolve(Project)} is called, and the include
 * and exclude patterns are applied during that method call.
 *
 * @author <a href="mailto:peter@myire.org">Peter Franzen</a>
 */
class DirectoryFilesSpec implements PatternFilterable
{
    private final Object fDirectoryPath;
    private final PatternSet fPatternSet = new PatternSet();


    /**
     * Create a new {@code DirectoryFilesSpec}.
     *
     * @param pDirectoryPath    An object that resolves to a directory when passed to
     *                          {@code org.gradle.api.Project::fileTree()}.
     *
     * @throws NullPointerException if {@code pDirectoryPath} is null.
     */
    DirectoryFilesSpec(Object pDirectoryPath)
    {
        fDirectoryPath = requireNonNull(pDirectoryPath);
    }


    /**
     * Resolve the directory passed to the constructor, apply the include and exclude patterns and
     * return the resulting file collection.
     *
     * @param pProject  The project to resolve the directory with.
     *
     * @return  A new {@code FileCollection}, never null.
     *
     * @throws NullPointerException if {@code pProject} is null.
     */
    FileCollection resolve(Project pProject)
    {
        return pProject.fileTree(fDirectoryPath).matching(fPatternSet);
    }


    @Override
    public Set<String> getIncludes()
    {
        return fPatternSet.getIncludes();
    }


    @Override
    public Set<String> getExcludes()
    {
        return fPatternSet.getExcludes();
    }


    @Override
    public PatternFilterable setIncludes(Iterable<String> pIncludes)
    {
        return fPatternSet.setIncludes(pIncludes);
    }


    @Override
    public PatternFilterable setExcludes(Iterable<String> pExcludes)
    {
        return fPatternSet.setExcludes(pExcludes);
    }


    @Override
    public PatternFilterable include(String... pIncludes)
    {
        return fPatternSet.include(pIncludes);
    }


    @Override
    public PatternFilterable include(Iterable<String> pIncludes)
    {
        return fPatternSet.include(pIncludes);
    }


    @Override
    public PatternFilterable include(Spec<FileTreeElement> pIncludeSpec)
    {
        return fPatternSet.include(pIncludeSpec);
    }


    @Override
    public PatternFilterable include(Closure pIncludeSpec)
    {
        return fPatternSet.include(pIncludeSpec);
    }


    @Override
    public PatternFilterable exclude(String... pExcludes)
    {
        return fPatternSet.exclude(pExcludes);
    }


    @Override
    public PatternFilterable exclude(Iterable<String> pExcludes)
    {
        return fPatternSet.exclude(pExcludes);
    }


    @Override
    public PatternFilterable exclude(Spec<FileTreeElement> pExcludeSpec)
    {
        return fPatternSet.exclude(pExcludeSpec);
    }


    @Override
    public PatternFilterable exclude(Closure pExcludeSpec)
    {
        return fPatternSet.exclude(pExcludeSpec);
    }
}
