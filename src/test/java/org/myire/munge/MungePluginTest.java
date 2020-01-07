/*
 * Copyright 2019 Peter Franzen. All rights reserved.
 *
 * Licensed under the Apache License v2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
package org.myire.munge;

import org.junit.Test;
import static org.junit.Assert.assertNotNull;

import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;


public class MungePluginTest
{
    @Test
    public void applyingPluginCreatesTask()
    {
        // Given
        Project aProject = ProjectBuilder.builder().build();

        // When
        aProject.getPlugins().apply(org.myire.munge.MungePlugin.class);

        // Then
        assertNotNull(aProject.getTasks().findByName("transform"));
    }
}
