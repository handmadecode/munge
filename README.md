# Munge

A Gradle plugin for transforming XML files by applying XSL stylesheets and FreeMarker templates.

*Munge* (and its related term *mung*) has long been jargon for manipulating data in some way, see
for instance the [Jargon file](http://www.catb.org/~esr/jargon/html/M/munge.html) or
[Wikipedia](https://en.wikipedia.org/wiki/Mung_%28computer_term%29). The plugin's name is however
foremost a nod to the old Macintosh Toolbox function *Munger*:

*Munger (which rhymes with "plunger") lets you manipulate bytes [...]*
([Inside Macintosh Volume I](http://www.weihenstephan.org/~michaste/pagetable/mac/Inside_Macintosh.pdf),
page 468)

*The Munger function searches for a sequence of bytes and replaces it with another sequence of bytes
[...]*
([Inside Macintosh: Text](https://developer.apple.com/library/archive/documentation/mac/pdf/Text.pdf),
page 5-21)


## Contents
1. [Release Notes](#release-notes)
1. [Usage](#usage)
1. [The transform task](#the-transform-task)
1. [Transformation sets](#transformation-sets)
1. [Saxon transformations](#saxon-transformations)
1. [FreeMarker transformations](#freemarker-transformations)
1. [External library versions](#external-library-versions)


## Release Notes

### version 1.1

* The Saxon and FreeMarker versions can be specified in the transform task's configuration
  properties.
* The Xalan and/or Jaxen libraries can be made available to FreeMarker for its XPath support.
* Requires Gradle 6.1

### version 1.0

* Initial release.


## Usage

The Munge plugin requires Gradle version 6.1 or newer. It is applied using the plugins DSL:

    plugins {
      id 'org.myire.munge' version '1.1'
    }


## The transform task

The plugin adds a `transform` task to the project. This task is configured with one or more
*transformation sets*. When the task is executed it processes all transformation sets in the order
they are declared in the configuration.

There are two types of transformation sets; `saxon` and `freemarker`. A `saxon` transformation set
applies XSL style sheets to XML files using the [Saxon](https://www.saxonica.com) library. A
`freemarker` transformation set applies [Apache FreeMarker](https://freemarker.apache.org) templates
to XML files.

Example: a task that first should perform the transformations in a `saxon` transformation set and
then the transformations in a `freemarker` transformation set is configured as follows:

    transform {
        saxon {
            ...
        }
        freemarker {
            ...
        }
    }

A task that first should perform the transformations in a `freemarker` transformation set, then the
transformations in a `saxon` set, and finally the transformations in another `freemarker` set is
configured like this:

    transform {
        freemarker {
            ...
        }
        saxon {
            ...
        }
        freemarker {
            ...
        }
    }

By default the task doesn't fail the build if any errors occur in the transformations. This can be
changed by setting the `failOnError` property to true:

    transform {
        failOnError = true // Fail the build if there is an error in a transformation
        ...
    }


## Transformation sets

A transformation set specifies one or more _source_ files that should be transformed by applying one
or more _template_ files to them. The result of each transformation is written to an _output_ file.

Source and template files can be configured in several ways:

* with the path to a file.
* with the path to a directory (to include all files in that directory and in any subdirectories).
* with the path to a directory together with a closure that specifies which of the directory's files
  to include. The closure operates on a standard Gradle `PatternFilterable`, like e.g. the `Copy`
  task.

Relative paths are resolved relative to the project directory in all of the above cases.

### Source files

To configure a transformation set with a single source file you specify its path:

    source 'path/to/file'

If the path specifies a directory, all files in that directory, including files in any
subdirectories, will be added as source files to the transformation set:

    source 'path/to/directory'

Multiple files and/or directories can be added with the `sources` method:

    sources 'path/to/file1', 'path/to/file2', 'path/to/directory'

When adding a directory, the files to include can be controlled with a configuration closure:

    sources ('path/to/directory') {
        include '*.xml'
    }

### Template files

Template files are added to a transformation set in the same way as source files:

    template 'path/to/file1'
    templates 'path/to/file2', 'path/to/directory1'
    templates ('path/to/directory2') {
        exclude 'common.xsl'
    }

### Output files

When a transformation set is processed it will apply each of its template files to each of its
source files, producing an output file for each transformation. The output file for a transformation
can be specified in several ways:

* All output can be directed to a single file, specified in the `outputFile` property. If the
  transformation set contains several transformations, the output from each transformation is
  appended to the file.
* The output files can be created in a specific directory, specified in the `outputDir` property.
  The name of the output file will then be the same as the name of the transformation's source file.
* The output file can be specified by a closure applied to the transformation's source and template
  files (as `java.io.File` instances). The closure either returns an object specifying the output
  file of the transformation or null if there should be no explicit output file. In the former case
  the returned object is resolved with the project method `file()`. Output file closures are added
  with the `outputMapping` method.

The above variants can be combined. Multiple closures can be added to the output file specification,
and they will be called in the order they were added until one returns a non-null value. If all
closures return null (or there are no closures configured), the directory or file variant will be
used. Should both an output directory and an output file be specified, the former takes precedence.

Example of output file configuration:

    outputDir = 'directory1'
    outputMapping {
        s, t ->
            if (t.name.startsWith('xyz'))
                'directory2/' + s.name + '-' + t.name
            else
                null
    }

The above configuration would create all output files in the directory `directory1` except for
transformations where the template file's name starts with "xyz", in which case the output file will
be written to the directory `directory2` and have the template file's name appended to the source
file's name.

Assuming we have a transformation set with two source files, `sourceFile1` and `sourceFile2`, and
two template files, `abcTemplateFile` and `xyzTemplateFile`, the four transformations in the set
would produce the following output files:

    sourceFile1 x abcTemplateFile -> directory1/sourceFile1
    sourceFile1 x xyzTemplateFile -> directory2/sourceFile1-xyzTemplateFile
    sourceFile2 x abcTemplateFile -> directory1/sourceFile2
    sourceFile2 x xyzTemplateFile -> directory2/sourceFile2-xyzTemplateFile

### Dynamic output directories

In some cases the output files are not specified in the transformation set configuration. Instead
they are created from the templates by using e.g. `<xsl:result-document>` or the FreeMarker
directive `<@outputfile>` installed by the plugin (see below).

Since the task doesn't know about these output files, the Gradle up-to-date check of the task will
not be able to detect modifications to them. To remedy this the directories where the templates
create files can be specified as dynamic output directories in the transformation set's
configuration:

    dynamicOutputDirectory "${buildDir}/generated-sources"

Any directory added as a dynamic output directory will be included in the up-to-date check of the
`transform` task.

### Transformation parameters

The transformation set property `parameter` is used to specify parameters that should be passed to
each transformation in the set:

     parameter('baseDirectory', "${buildDir}/generated-sources")

Parameters can also be specified as a map:

    parameters(
        'stringParam': 'value',
        'intParam': 17
    )


## Saxon transformations

In Saxon transformations the source files are XML files and the template files are XSL style sheets.
The transformations are configured in a `saxon` transformation set configuration block within the
`transform` task.

The `saxon` configuration block adds one property to the common transformation set properties; the
`configurationFile` property. This property lets you specify a
[Saxon configuration file](https://www.saxonica.com/html/documentation/configuration/configuration-file)
that will be used in all transformations in that transformation set.

Example:

    saxon {
        configurationFile = 'src/config/saxon.xml'
        sources ('resources/xml') {
            exclude '*.xsd'
        }
        template 'resources/xsl'
        outputDir = 'generated-resources'
    }

If no configuration file is specified the default configuration will be used.


## FreeMarker transformations

In FreeMarker transformations the source files are XML files and the template files are Apache
FreeMarker template files. FreeMarker transformations are configured in a `freemarker`
transformation set configuration block within the `transform` task.

The `freemarker` configuration block adds two properties to the common transformation set
properties:

* `configurationFile` lets you specify a FreeMarker configuration file that will be used in all
  transformations in that transformation set. A FreeMarker configuration file is a standard
  properties file, the valid properties are documented
  [here](https://freemarker.apache.org/docs/api/freemarker/template/Configuration.html#setSetting-java.lang.String-java.lang.String-).
  If no configuration file is specified the default configuration will be used.
* `charset` lets you specify the character set to encode the output files with. The default is
  UTF-8. Note that this property does not affect the character set of files created with the
  `<@outputfile>` directive (see below).

Example:

    freemarker {
        configurationFile = 'src/config/freemarker.properties'
        source 'resources/xml'
        templates ('resources/templates') {
            include '*.ftl', '*.fm'
        }
        outputDir = 'generated-sources'
        charset = 'iso-8859-1'
    }


### Outputfile directive

In some cases it is desirable for a FreeMarker template to generate multiple output files for one
source file. This can however not be configured in the `freemarker` transformation set, since the
logic for which output files to create is stored in the template files.

To accommodate for this the plugin adds an `<@outputfile>` directive to the FreeMarker configuration
used by the transformations. A FreeMarker template can redirect some of its output to a specific
file using this directive:

    This text is written to the transformation's normal output file
    <@outputfile path='path/to/outputfile'>
        This text is written to the specified output file
    </@outputfile>
    This text is written to the transformation's normal output file

The directive requires the file path to be specified in the `path` parameter. The character set of
the file can be specified in the optional parameter `charset`, default is UTF-8.

This makes it possible for a `freemarker` transformation set to not specify any output file,
directory or output mapping closures, and let the templates create all output files with the
`<@outputfile>` directive.

### XPath support

FreeMarker supports XPath expressions in its XML document processing, see the
[manual](https://freemarker.apache.org/docs/xgui_imperative_learn.html#xgui_imperative_learn_xpath).
This support requires either [Xalan](http://xml.apache.org/xalan-j/index.html) or
[Jaxen](http://www.cafeconleche.org/jaxen/) to be present on the runtime classpath.

By default, the Munge plugin adds the Jaxen library to the runtime classpath. To configure the
plugin to use Xalan instead simply specify the version of the Xalan library as described in the
section on [External library versions](#external-library-versions).

If neither Xalan nor Jaxen is available, FreeMarker has an internal fallback to use the XPath
classes in the `com.sun.org.apache.xpath.internal` package. This fallback instantiates the
`com.sun.org.apache.xpath.internal.XPath` class through reflection, and makes the XPath support work
without Xalan or Jaxen if reflective access to internal JDK packages is allowed.

This internal fallback works silently with the JVM default settings up to and including Java 15. In
Java 16 however, the default value of the JVM flag `--illegal-access` was changed from `permit` to
`deny`, which makes the internal fallback for XPath support fail, and you will see the message

    No XPath support is available.

when executing a `transform` task with a `freemarker` transformation set. To enable the internal
fallback to the `com.sun.org.apache.xpath.internal` classes in Java 16, start Gradle with the JVM
argument `--illegal-access=permit`, i.e. by specifying the command line option 
`"-Dorg.gradle.jvmargs=--illegal-access=permit"`.

The JVM flag `--illegal-access` was removed in Java 17, making the use of Xalan or Jaxen the only
way to enable XPath support in FreeMarker transformations. Should this for some reason not be
desirable it may be possible to enable the internal fallback to `com.sun.org.apache.xpath.internal`
by starting Gradle with the JVM options

    --add-opens java.xml/com.sun.org.apache.xpath.internal=ALL-UNNAMED
    --add-opens java.xml/com.sun.org.apache.xml.internal.utils=ALL-UNNAMED

but this hasn't been tested.


## External library versions

The Munge plugin depends on the Saxon and FreeMarker libraries and optionally on the Xalan or Jaxen
library for XPath support in FreeMarker transformations. The version to use of these libraries can
be configured in the `transform` task.

### Saxon

The artifact for the Saxon dependency is `net.sf.saxon:Saxon-HE`. By default, version 9.9.1-8 is
used, this can be changed by setting the `transform` task's configuration property `saxonVersion`:

    transform {
        saxonVersion = '9.9.1-5'
        ...
    }

### FreeMarker

The artifact for the FreeMarker dependency is `org.freemarker:freemarker`. By default,
version 2.3.31 is  used, this can be changed by setting the `transform` task's configuration
property `freeMarkerVersion`:

    transform {
        freeMarkerVersion = '2.3.29'
        ...
    }

### Xalan and Jaxen

FreeMarker supports XPath expressions if either Xalan or Jaxen is available on the classpath.

By default, version 1.2.0 of the Jaxen artifact `jaxen:jaxen` is included by the Munge plugin. If
another version of Jaxen is desired it can be specified in the `transform` task's configuration
property `jaxenVersion`:

    transform {
        jaxenVersion = '1.1.6'
        ...
    }


To use Xalan instead of Jaxen, specify the version of the Xalan artifact `xalan:xalan` to use in
the `transform` task's configuration property `xalanVersion`:

    transform {
	    xalanVersion = '2.7.2' // Use Xalan for FreeMarker XPath support
        ...
    }

If explicit versions for both libraries are specified they will both be included in the runtime
classpath. In this case FreeMarker chooses Xalan over Jaxen.
