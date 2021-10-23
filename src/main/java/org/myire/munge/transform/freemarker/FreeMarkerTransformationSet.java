/*
 * Copyright 2019, 2021 Peter Franzen. All rights reserved.
 *
 * Licensed under the Apache License v2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
package org.myire.munge.transform.freemarker;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.gradle.api.Project;
import org.gradle.api.tasks.Input;

import org.myire.munge.transform.TransformationSet;
import org.myire.munge.transform.Transformer;


/**
 * A transformation set that applies FreeMarker template files to XML source files.
 *
 * @author <a href="mailto:peter@myire.org">Peter Franzen</a>
 */
public class FreeMarkerTransformationSet extends TransformationSet
{
    private Charset fCharset = StandardCharsets.UTF_8;


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
    protected Transformer createTransformer(ClassLoader pClassLoader) throws ReflectiveOperationException
    {
        String aClassName = "org.myire.munge.transform.freemarker.FreeMarkerTransformer";
        Class<?> aTransformerClass = Class.forName(aClassName, false, pClassLoader);
        Transformer aTransformer = (Transformer) aTransformerClass.newInstance();
        aTransformer.setOutputFileCharset(fCharset);
        return aTransformer;
    }
}
