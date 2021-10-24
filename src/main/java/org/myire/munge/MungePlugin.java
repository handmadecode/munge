/*
 * Copyright 2019, 2021 Peter Franzen. All rights reserved.
 *
 * Licensed under the Apache License v2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
package org.myire.munge;

import org.gradle.api.Project;
import org.gradle.api.Plugin;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.DependencySet;
import org.gradle.api.artifacts.dsl.DependencyHandler;


/**
 * Gradle plugin for adding a file transformation task to its project.
 */
public class MungePlugin implements Plugin<Project>
{
    static private final String CONFIGURATION_NAME = "munge";

    static private final String SAXON_GROUP_ARTIFACT_ID = "net.sf.saxon:Saxon-HE";
    static private final String FREEMARKER_GROUP_ARTIFACT_ID = "org.freemarker:freemarker";
    static private final String XALAN_GROUP_ARTIFACT_ID = "xalan:xalan";
    static private final String JAXEN_GROUP_ARTIFACT_ID = "jaxen:jaxen";

    private Project fProject;
    private Configuration fConfiguration;
    private TransformTask fTask;


    public void apply(Project pProject)
    {
        fProject = pProject;

        // Create the munge configuration and add it to the project. The munge classpath is
        // specified through this configuration's dependencies.
        fConfiguration = createConfiguration();

        // Create the task.
        fTask = pProject.getTasks().create(TransformTask.TASK_NAME, TransformTask.class);
        fTask.setDescription("Performs file transformations");
    }


    /**
     * Create the Munge configuration if not already present in the project and define it to depend
     * on the default artifacts unless explicit dependencies have been defined.
     *
     * @return  The Munge configuration.
     */
    private Configuration createConfiguration()
    {
        Configuration aConfiguration = fProject.getConfigurations().maybeCreate(CONFIGURATION_NAME);

        aConfiguration.setVisible(false);
        aConfiguration.setTransitive(true);
        aConfiguration.setDescription("The external libraries used by the transform task");

        // Add an action that adds a dependency on the external library artifacts before the
        // configuration's dependencies are resolved.
        aConfiguration.getIncoming().beforeResolve(ignore -> this.setConfigurationDependencies());

        return aConfiguration;
    }


    /**
     * Add dependencies on the Saxon and FreeMarker artifacts to the {@code munge} configuration if
     * it has no explicit dependencies. The artifacts' versions will be taken from the
     * {@code transform} task's configuration properties.
     *<p>
     * If the {@code transform} task is configured with non-null versions for the Xalan and Jaxen
     * artifacts, dependencies will be added for those artifacts too.
     */
    private void setConfigurationDependencies()
    {
        if (fConfiguration.getDependencies().isEmpty())
        {
            DependencySet aDependencies = fConfiguration.getDependencies();
            DependencyHandler aDependencyHandler = fProject.getDependencies();

            addDependency(aDependencies, aDependencyHandler, SAXON_GROUP_ARTIFACT_ID, fTask.getSaxonVersion());
            addDependency(aDependencies, aDependencyHandler, FREEMARKER_GROUP_ARTIFACT_ID, fTask.getFreeMarkerVersion());
            addDependency(aDependencies, aDependencyHandler, XALAN_GROUP_ARTIFACT_ID, fTask.getXalanVersion());
            addDependency(aDependencies, aDependencyHandler, JAXEN_GROUP_ARTIFACT_ID, fTask.getJaxenVersion());
        }
    }


    /**
     * Add a dependency on an artifact to a {@code DependencySet} if its version is non-null.
     *
     * @param pDependencies         The dependency set to add the dependency to.
     * @param pDependencyHandler    The instance to create the dependency with.
     * @param pGroupAndArtifactID   The group and artifact ID string.
     * @param pVersion              The version string.
     *
     * @throws NullPointerException if {@code DependencySet} or {@code pDependencyHandler} is null.
     */
    static private void addDependency(
        DependencySet pDependencies,
        DependencyHandler pDependencyHandler,
        String pGroupAndArtifactID,
        String pVersion)
    {
        if (pVersion != null)
            pDependencies.add(pDependencyHandler.create(pGroupAndArtifactID + ':' + pVersion));
    }
}
