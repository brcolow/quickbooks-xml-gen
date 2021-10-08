# Quickbooks SDK XML Generator

This package is not meant to be used directly, as it is not published anywhere. It is more of a reference for how I went
about generating Quickbooks Desktop SDK XML-SOAP files without using older plugins that had issues with JPMS as well as
no support for caching (that is, only re-generate the Quickbooks SDK classes if the XSD, WSDL, or bindings have changed).
If you want to use my work here you will need to:

* Copy the two Groovy scripts (`JaxSourcesGenerator` and `JaxServiceGenerator`) in `src/main/groovy`.
* Copy the `gmaven-plus-plugin` config from `pom.xml` to your `pom.xml`.
* Copy the XSD/WSDL resource files in `src/main/resources`, the xjc plugins in `src/main/java/com/brcolow/xjc`.
* Optionally copy `bindings.jxb` in `src/main/resources/bindings.jxb` which is a reference for anyone wanting to see some
tweaks to make the generated XML-SOAP classes nicer to work I personally use to make the SDK easier to deal with. If
using `bindings.jxb` then also include classes in `src/main/java/com/brcolow/quickbooks`.

Then (after copying the necessary files and configuration) when building your project (via `mvn package`, for example)
the Quickbooks SDK XML-Soap sources will be auto-generated and their class files added to your target's built classes.