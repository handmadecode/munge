/*
 * Copyright 2019, 2021 Peter Franzen. All rights reserved.
 *
 * Licensed under the Apache License v2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
package org.myire.munge.transform.saxon;

import org.gradle.api.Project;

import org.myire.munge.transform.TransformationSet;
import org.myire.munge.transform.Transformer;


/**
 * A transformation set that applies XSLT style sheets to XML source files using the Saxon
 * processor.
 *
 * @author <a href="mailto:peter@myire.org">Peter Franzen</a>
 */
public class SaxonTransformationSet extends TransformationSet
{
    /**
     * Create a new {@code SaxonTransformationSet}.
     *
     * @param pProject  The project to resolve file paths with.
     *
     * @throws NullPointerException if {@code pProject} is null.
     */
    public SaxonTransformationSet(Project pProject)
    {
        super(pProject);
    }


    @Override
    protected Transformer createTransformer(ClassLoader pClassLoader) throws ReflectiveOperationException
    {
        String aClassName = "org.myire.munge.transform.saxon.SaxonTransformer";
        Class<?> aTransformerClass = Class.forName(aClassName, false, pClassLoader);
        return (Transformer) aTransformerClass.newInstance();
    }
}
