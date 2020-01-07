/*
 * Copyright 2019 Peter Franzen. All rights reserved.
 *
 * Licensed under the Apache License v2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
package org.myire.munge;

import org.gradle.api.Project;
import org.gradle.api.Plugin;


/**
 * Gradle plugin for adding a file transformation task to its project.
 */
public class MungePlugin implements Plugin<Project>
{
    public void apply(Project pProject)
    {
        TransformTask aTask = pProject.getTasks().create(TransformTask.TASK_NAME, TransformTask.class);
        aTask.setDescription("Performs file transformations");
    }
}
