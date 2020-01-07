/*
 * Copyright 2019 Peter Franzen. All rights reserved.
 *
 * Licensed under the Apache License v2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
package org.myire.munge;

import java.util.ArrayList;
import java.util.List;

import groovy.lang.Closure;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.TaskAction;

import org.myire.munge.transform.TransformationSet;
import org.myire.munge.transform.freemarker.FreeMarkerTransformationSet;
import org.myire.munge.transform.saxon.SaxonTransformationSet;


/**
 * Gradle task that performs file transformations specified in one or more {@link TransformationSet}
 * instances.
 *
 * @author <a href="mailto:peter@myire.org">Peter Franzen</a>
 */
public class TransformTask extends DefaultTask
{
    static final String TASK_NAME = "transform";


    private final List<TransformationSet> fTransformationSets = new ArrayList<>();
    private boolean fFailOnError;


    /**
     * Get the transformation sets added to this task.
     *
     * @return  The list of transformation sets, never null.
     */
    @Nested
    public List<TransformationSet> getTransformationSets()
    {
        return fTransformationSets;
    }


    /**
     * Add a {@link SaxonTransformationSet} to the task.
     *
     * @param pClosure  A closure that configures the transformation set.
     */
    public void saxon(Closure<?> pClosure)
    {
        fTransformationSets.add(new SaxonTransformationSet(getProject()).configure(pClosure));
    }


    /**
     * Add a {@link FreeMarkerTransformationSet} to the task.
     *
     * @param pClosure  A closure that configures the transformation set.
     */
    public void freeMarker(Closure<?> pClosure)
    {
        fTransformationSets.add(new FreeMarkerTransformationSet(getProject()).configure(pClosure));
    }


    /**
     * Get the <i>fail on error property</i>. If this property is true, the {@link #transform()}
     * method will throw a {@code GradleException} if any transformation set reports a
     * transformation error. If this property is false, errors will only be logged. The default
     * value is false.
     *
     * @return  The <i>fail on error property</i>.
     */
    @Input
    public boolean isFailOnError()
    {
        return fFailOnError;
    }


    public void setFailOnError(boolean pFailOnError)
    {
        fFailOnError = pFailOnError;
    }


    /**
     * Execute the transformations specified in all transformation sets added to the task. The
     * transformations will be executed sequentially in the order they were added.
     *
     * @throws GradleException  if at least one of the transformation sets reported an error and
     *                          {@link #isFailOnError()} is true.
     */
    @TaskAction
    public void transform()
    {
        int aNumErrors = 0;
        for (TransformationSet aTransformationSet : fTransformationSets)
            aNumErrors += aTransformationSet.transform();

        if (aNumErrors > 0 && isFailOnError())
            throw new GradleException("There were " + aNumErrors + " transformation errors");
    }
}
