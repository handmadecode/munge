/*
 * Copyright 2019 Peter Franzen. All rights reserved.
 *
 * Licensed under the Apache License v2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
package org.myire.munge.transform;

import groovy.lang.Closure;

import org.gradle.api.tasks.util.PatternFilterable;


class PatternFilterableClosure extends Closure<Object>
{
    private final String fIncludePattern;
    private final String fExcludePattern;


    PatternFilterableClosure(String pIncludePattern, String pExcludePattern)
    {
        super(null);
        fIncludePattern = pIncludePattern;
        fExcludePattern = pExcludePattern;
    }


    @Override
    public Object call(Object... args)
    {
        Object aDelegate = args.length > 0 ? args[0] : getDelegate();
        if (aDelegate instanceof PatternFilterable)
        {
            if (fIncludePattern != null)
                ((PatternFilterable) aDelegate).include(fIncludePattern);
            if (fExcludePattern != null)
                ((PatternFilterable) aDelegate).exclude(fExcludePattern);
        }

        return this;
    }
}
