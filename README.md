# Quickbooks SDK XML Generator

Add the two Groovy scripts (`JaxSourcesGenerator` and `JaxServiceGenerator`), the `gmaven-plus-plugin` config from `pom.xml` and
the XSD/WSDL resource files inside `src/main/resources`) to your project. `bindings.jxb` is an optional resource I included
on this repository just as a reference for anyone wanting to know some tweaks to make the generated XML-SOAP classes nicer to work
with. If using `bindings.jxb` then also include `src/main/java/

Then when building your project (via `mvn package`, for example) the Quickbooks SDK XML-Soap classes
will be added to the target build classes.