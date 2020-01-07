/*
 * Copyright 2019 Peter Franzen. All rights reserved.
 *
 * Licensed under the Apache License v2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
package org.myire.munge.transform.freemarker;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Objects;

import freemarker.core.Environment;
import freemarker.template.TemplateDirectiveBody;
import freemarker.template.TemplateDirectiveModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;


/**
 * FreeMarker template directive that renders its body to a file specified as parameter.
 *
 * @author <a href="mailto:peter@myire.org">Peter Franzen</a>
 */
class OutputFileDirective implements TemplateDirectiveModel
{
    static private final String PARAMETER_PATH = "path";
    static private final String PARAMETER_CHARSET = "charset";


    @Override
    public void execute(
        Environment pEnvironment,
        Map pParameters,
        TemplateModel[] pLoopVariables,
        TemplateDirectiveBody pBody) throws TemplateException, IOException
    {
        if (pBody == null)
            // Nothing to write for an empty body.
            return;

        // The path (relative or absolute) to the output file is specified in the mandatory
        // parameter 'path'.
        String aPath = Objects.toString(pParameters.get(PARAMETER_PATH), null);
        if (aPath == null)
            throw new TemplateModelException("Parameter '" + PARAMETER_PATH + "' not specified");

        // Ensure the entire path to the output file exists.
        Path aOutputFile = Paths.get(aPath);
        Files.createDirectories(aOutputFile.getParent());

        // Get the optional charset parameter, the default charset UTF-8 will be used if not
        // specified.
        Charset aCharset = toCharset(Objects.toString(pParameters.get(PARAMETER_CHARSET), null));

        // Render the directive's body to the output file.
        try (OutputStreamWriter aWriter =
                 new OutputStreamWriter(Files.newOutputStream(aOutputFile), aCharset))
        {
            pBody.render(aWriter);
        }
    }


    /**
     * Get the {@code Charset} instance for a charset name.
     *
     * @param pName The name of the charset, or null to get the default {@code Charset}.
     *
     * @return  A {@code Charset}, never null.
     *
     * @throws TemplateModelException   if {@code pName} is an invalid or unsupported charset name.
     */
    static private Charset toCharset(String pName) throws TemplateModelException
    {
        try
        {
            if (pName != null)
                return Charset.forName(pName);
            else
                return StandardCharsets.UTF_8;
        }
        catch (IllegalCharsetNameException | UnsupportedCharsetException e)
        {
            throw new TemplateModelException(e);
        }
    }
}
