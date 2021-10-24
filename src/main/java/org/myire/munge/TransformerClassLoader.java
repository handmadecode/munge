/*
 * Copyright 2021 Peter Franzen. All rights reserved.
 *
 * Licensed under the Apache License v2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
package org.myire.munge;

import java.io.File;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.Objects;
import static java.util.Objects.requireNonNull;

import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.logging.Logging;

import org.myire.munge.transform.Transformer;


/**
 * A class loader for dynamically loading the classes of the external libraries used by
 * {@link Transformer} implementations.
 *<p>
 * This class loader is part of the solution to the following problem: a {@code Transformer}
 * executes transformations by utilizing one or more external libraries. The exact version of those
 * libraries can be specified at runtime through configuration properties. To achieve this, the
 * external library classes must be loaded from a dynamically specified location. This means that
 * the {@code TransformationSet} that uses the external library cannot reference those classes
 * directly, since that would trigger loading those classes when the {@code TransformationSet} class
 * is loaded, which would occur before the {@code TransFormTask} has a chance to specify the
 * location of the external libraries.
 *<p>
 * Instead, the external library classes are referenced only through {@code Transformer}
 * implementation classes. Those implementation classes can be instantiated through reflection at
 * runtime with a {@code ClassLoader} that can load the external library classes from a dynamically
 * specified location (more precisely the {@code munge} Gradle configuration). The
 * {@code Transformer} implementation class will by nature reference the external library classes,
 * and must also be loaded by this special {@code ClassLoader}.
 *<p>
 * Using a special {@code ClassLoader} also allows the optional loading of additional libraries that
 * are used by the external libraries if available. An example of this is FreeMaker's XPath
 * support, which uses Jaxen or Xalan if available and otherwise falls back to use the JDK internal
 * classes in the {@code com.sun.org.apache.xpath} package.
 *<p>
 * To summarize, this class loader should:
 *<ul>
 * <li>Load the external library classes from a dynamically specified location, i.e. the
 *    {@code munge} Gradle configuration</li>
 * <li>Load the {@code Transformer} implementation classes and any helper classes</li>
 * <li>Delegate the loading of all other classes to the class loader of the {@code Transformer}
 *     class (this is especially important for the {@code Transformer} interface itself; the
 *     {@code TransformerSet} instantiating the {@code Transformer} implementation class will
 *     already have referenced and loaded the interface, and should this class loader load it as
 *     part of loading the implementation class, it will not be the same interface and a
 *     {@code ClassCastException} will be thrown)</li>
 *</ul>
 *
 * @author <a href="mailto:peter@myire.org">Peter Franzen</a>
 */
class TransformerClassLoader extends URLClassLoader
{
    // Packages containing Transformer implementations that this instance should load.
    static private final String[] cTransformerImplementationPackages =
    {
        "org.myire.munge.transform.saxon",
        "org.myire.munge.transform.freemarker"
    };


    private final ConfigurationContainer fConfigurations;
    private URLClassLoader fMungeConfigurationClassLoader;


    /**
     * Create a new {@code TransformerClassLoader}.
     *
     * @param pConfigurations   The configuration container to get the {@code munge} configuration
     *                          from. This configuration specifies the file locations where the
     *                          external library classes are located.
     *
     * @throws NullPointerException if {@code pConfigurations} is null.
     */
    TransformerClassLoader(ConfigurationContainer pConfigurations)
    {
        this(pConfigurations, Transformer.class.getClassLoader());
    }


    /**
     * Create a new {@code TransformerClassLoader}.
     *
     * @param pConfigurations   The configuration container to get the {@code munge} configuration
     *                          from.
     * @param pParent           The parent class loader to delegate loading of non-library classes
     *                          to.
     *
     * @throws NullPointerException if {@code pConfigurations} is null.
     */
    private TransformerClassLoader(ConfigurationContainer pConfigurations, ClassLoader pParent)
    {
        super(getUrls(pParent), pParent);
        fConfigurations = requireNonNull(pConfigurations);
    }


    @Override
    protected Class<?> loadClass(String pName, boolean pResolve) throws ClassNotFoundException
    {
        synchronized(getClassLoadingLock(pName))
        {
            // First try to load the class from the 'munge' configuration's class locations.
            try
            {
                // Lazily create the class loader for the configuration's class locations to allow
                // its file locations to be resolved as late as possible.
                if (fMungeConfigurationClassLoader == null)
                    fMungeConfigurationClassLoader = createMungeConfigurationClassLoader();

                return fMungeConfigurationClassLoader.loadClass(pName);
            }
            catch (ClassNotFoundException cnfe)
            {
                // Class not found in the 'munge' configuration's class locations, continue.
            }

            if (isTransformerImplementationClass(pName))
            {
                // Transformer implementation classes should be loaded by this instance and not by
                // the parent, first check if it has already been loaded.
                Class<?> aClass = findLoadedClass(pName);
                if (aClass == null)
                    // Load this class, if it can't be loaded a ClassNotFoundException will be
                    // thrown without letting the parent have a chance at loading the class; the
                    // class URLs used by this class loader are the same as used by the parent.
                    aClass = findClass(pName);

                if (pResolve)
                    resolveClass(aClass);

                return aClass;
            }
            else
                // Class not found in the 'munge' configuration, and it is not a Transformer
                // implementation class, go through the normal class loader hierarchy.
                return super.loadClass(pName, pResolve);
        }
    }


    /**
     * Create a class loader that loads from the 'munge' configuration's class locations.
     */
    private URLClassLoader createMungeConfigurationClassLoader()
    {
        Configuration aConfiguration = fConfigurations.findByName("munge");
        if (aConfiguration != null)
            return new URLClassLoader(createUrls(aConfiguration.getFiles()), getPlatformClassLoader());
        else
            return new URLClassLoader(new URL[0], getPlatformClassLoader());
    }


    /**
     * Check if the fully qualified name of a class is in one of the {@code Transformer}
     * implementation packages.
     *
     * @param pFqn The fully qualified name of the class.
     *
     * @return  True if the specified class name is the name of a {@code Transformer} implementation
     *          class, false if not.
     */
    static private boolean isTransformerImplementationClass(String pFqn)
    {
        for (String aPackageName : cTransformerImplementationPackages)
            if (pFqn.startsWith(aPackageName))
                return true;

        return false;
    }


    /**
     * Get the platform class loader, if available.
     *
     * @return The platform class loader, or null if there is none.
     */
    static private ClassLoader getPlatformClassLoader()
    {
        try
        {
            MethodHandle aMethodHandle =
                MethodHandles.lookup().findStatic(
                    ClassLoader.class,
                    "getPlatformClassLoader",
                    MethodType.methodType(ClassLoader.class));

            return (ClassLoader) aMethodHandle.invokeExact();
        }
        catch (Throwable t)
        {
            return null;
        }
    }


    /**
     * Create an array of {@code URL} instances from a collection of {@code File} instances.
     *
     * @param pFiles The files to create {@code URL} instances from.
     *
     * @throws NullPointerException if {@code pFiles} is null.
     */
    static private URL[] createUrls(Collection<File> pFiles)
    {
        return pFiles
            .stream()
            .map(TransformerClassLoader::fileToUrl)
            .filter(Objects::nonNull)
            .toArray(URL[]::new);
    }


    /**
     * Get the {@code URL} instances a {@code ClassLoader} loads classes from.
     *
     * @param pClassLoader The class loader to get the {@code URL} locations from.
     *
     * @return An array of {@code URL} instances, possibly empty, never null.
     */
    static private URL[] getUrls(ClassLoader pClassLoader)
    {
        if (pClassLoader instanceof URLClassLoader)
            return ((URLClassLoader) pClassLoader).getURLs();
        else
            return new URL[0];
    }


    /**
     * Convert a {@code File} to a {@code URL}.
     *
     * @param pFile The file to convert.
     *
     * @return The file converted to a url, or null if the conversion failed.
     */
    static private URL fileToUrl(File pFile)
    {
        try
        {
            return pFile.toURI().toURL();
        }
        catch (MalformedURLException mue)
        {
            Logging.getLogger(TransformerClassLoader.class).debug(
                "Cannot convert file " + pFile + " to an URL, not adding to class loader");
            return null;
        }
    }
}
