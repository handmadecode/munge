/*
 * Copyright 2019 Peter Franzen. All rights reserved.
 *
 * Licensed under the Apache License v2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
package org.myire.munge;

import org.junit.Test;
import static org.junit.Assert.assertTrue;

import org.gradle.testfixtures.ProjectBuilder;

import org.myire.munge.transform.freemarker.FreeMarkerTransformationSet;
import org.myire.munge.transform.saxon.SaxonTransformationSet;


public class TransformTaskTest
{
    @Test
    public void saxonTransformationSetIsAdded()
    {
        // Given
        TransformTask aTask = ProjectBuilder.builder().build().getTasks().create("x", TransformTask.class);

        // When
        aTask.saxon(null);

        // Then
        assertTrue(aTask.getTransformationSets().get(0) instanceof SaxonTransformationSet);
    }


    @Test
    public void freeMarkerTransformationSetIsAdded()
    {
        // Given
        TransformTask aTask = ProjectBuilder.builder().build().getTasks().create("x", TransformTask.class);

        // When
        aTask.freeMarker(null);

        // Then
        assertTrue(aTask.getTransformationSets().get(0) instanceof FreeMarkerTransformationSet);
    }
}
