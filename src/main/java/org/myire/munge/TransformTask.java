/*
 * Copyright 2019, 2021 Peter Franzen. All rights reserved.
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
import org.gradle.api.tasks.Optional;
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

    // External library versions
    private String fSaxonVersion = "9.9.1-8";
    private String fFreeMarkerVersion = "2.3.31";
    private String fXalanVersion;
    private String fJaxenVersion ="1.2.0";
    private boolean fXalanVersionExplicitlySet;
    private boolean fJaxenVersionExplicitlySet;

    // Lazily created class loader for external classes used by the transformation sets.
    private TransformerClassLoader fTransformerClassLoader;



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
     * Get the <i>fail on error</i> property. If this property is true, the {@link #transform()}
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
     * Get the version of the Saxon library to use. The default value is &quot;9.9.1-8&quot;.
     *
     * @return  The version of the Saxon library to use. Null means to disable the Saxon library.
     */
    @Input
    @Optional
    public String getSaxonVersion()
    {
        return fSaxonVersion;
    }


    public void setSaxonVersion(String pSaxonVersion)
    {
        fSaxonVersion = pSaxonVersion;
    }


    /**
     * Get the version of the FreeMarker library to use. The default value is &quot;2.3.31&quot;.
     *
     * @return  The version of the FreeMarker library to use. Null means to disable the FreeMarker
     *          library.
     */
    @Input
    @Optional
    public String getFreeMarkerVersion()
    {
        return fFreeMarkerVersion;
    }


    public void setFreeMarkerVersion(String pFreeMarkerVersion)
    {
        fFreeMarkerVersion = pFreeMarkerVersion;

        if (pFreeMarkerVersion == null && !fJaxenVersionExplicitlySet)
            fJaxenVersion = null;
    }


    /**
     * Get the version of the Xalan library to use. The default value is {@code null}.
     *
     * @return  The version of the Xalan library to use. Null means to disable the Xalan library.
     */
    @Input
    @Optional
    public String getXalanVersion()
    {
        return fXalanVersion;
    }


    public void setXalanVersion(String pXalanVersion)
    {
        fXalanVersion = pXalanVersion;
        fXalanVersionExplicitlySet = true;

        if (!fJaxenVersionExplicitlySet)
            fJaxenVersion = null;
    }


    /**
     * Get the version of the Jaxen library to use. The default value is &quot;1.2.0&quot;.
     *
     * @return  The version of the Jaxen library to use. Null means to disable the Jaxen library.
     */
    @Input
    @Optional
    public String getJaxenVersion()
    {
        return fJaxenVersion;
    }


    public void setJaxenVersion(String pJaxenVersion)
    {
        fJaxenVersion = pJaxenVersion;
        fJaxenVersionExplicitlySet = true;

        if (!fXalanVersionExplicitlySet)
            fXalanVersion = null;
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
            aNumErrors += aTransformationSet.transform(getTransformerClassLoader());

        if (aNumErrors > 0 && isFailOnError())
            throw new GradleException("There were " + aNumErrors + " transformation errors");
    }


    /**
     * Get the class loader to load external transformation classes with, possibly creating the
     * instance first.
     *
     * @return  The transformer class loader, never null.
     */
    private ClassLoader getTransformerClassLoader()
    {
        if (fTransformerClassLoader == null)
        {
            fTransformerClassLoader =
                new TransformerClassLoader(getProject().getConfigurations());
        }

        return fTransformerClassLoader;
    }
}
