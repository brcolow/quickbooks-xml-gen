@Grab(group='org.glassfish.jaxb', module='jaxb-xjc', version='4.0.4')
import com.sun.tools.ws.WsImport

import java.nio.charset.StandardCharsets
import java.util.stream.Collectors
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.RuntimeException;
import java.security.MessageDigest

def printClassPath(classLoader) {
    String classpath = ""
    classpath += classLoader.getURLs().stream()
            .map(url -> {
                String path = "${url.getPath().toString().substring(1)}"
                // Found weird behavior where if Maven is run without 'clean' goal then ${project.build.directory}/target/classes/
                // is appended (with trailing slash) which for some reason breaks xjc extension finding.
                if (path.endsWith("/")) {
                    return path.substring(0, path.length() - 2)
                } else {
                    return path
                }
            })
            .collect(Collectors.joining(";", "", ";"))
    if (classLoader.parent) {
        classpath += printClassPath(classLoader.parent)
    }
    return classpath
}

static def getChecksum(file, type, version) {
    def digest = MessageDigest.getInstance(type)
    def fos = file.newInputStream()
    def buffer = new byte[16384]
    def len

    while ((len = fos.read(buffer)) > 0) {
        digest.update(buffer, 0, len)
    }
    if (version != null && !version.isEmpty()) {
        digest.update(version.getBytes(StandardCharsets.US_ASCII))
    }
    fos.close()
    def checksum = digest.digest()

    def result = ""
    for (byte b : checksum) {
        result += toHex(b)
    }
    return result
}

private static hexChr(int b) {
    return Integer.toHexString(b & 0xF)
}

private static toHex(int b) {
    return hexChr((b & 0xF0) >> 4) + hexChr(b & 0x0F)
}

System.properties.'com.sun.tools.xjc.Options.findServices' = true
System.properties.'com.sun.tools.internal.xjc.Options.findServices' = true
System.properties.'-DenableExternalEntityProcessing' = true
final File cacheDir = new File("${project.build.directory}/jax-cache")
final String bindingsFilePath = "${project.basedir}/src/main/resources/bindings/bindings.jxb"
final String xsdFilePath = "${project.basedir}/src/main/resources/xsd/qbxmlops130.xsd"
final File bindingsChecksumFile = new File("${project.build.directory}/jax-cache/" + bindingsFilePath.substring(bindingsFilePath.lastIndexOf('/'), bindingsFilePath.length()))
final File xsdChecksumFile = new File("${project.build.directory}/jax-cache/" + xsdFilePath.substring(xsdFilePath.lastIndexOf('/'), xsdFilePath.length()))
if (System.getenv("JAVA_HOME") == null) {
    throw new RuntimeException("JAVA_HOME environment variable must be set!");
}
ProcessBuilder processBuilder = new ProcessBuilder()
processBuilder.command(System.getenv("JAVA_HOME") + "/bin/java", "-classpath",
        printClassPath(this.class.classLoader), "com.sun.tools.xjc.Driver",
        '-version')
String xjcVersion
try {
    Process process = processBuilder.start()
    BufferedReader reader =
            new BufferedReader(new InputStreamReader(process.getInputStream()))

    String line
    String versionStr = ""
    while ((line = reader.readLine()) != null) {
        versionStr += line
    }

    if (!versionStr.startsWith("xjc ")) {
        throw new Exception("expected xjc -version response to start with \"xjc \" but was: \"" + versionStr + "\"")
    }
    xjcVersion = versionStr.substring(4)

    System.out.println("xjc version: \"" + xjcVersion + "\"")
    int exitCode = process.waitFor()
    if (exitCode != -1) {
        throw new Exception("expected xjc -version to return exit code -1 but was: " + exitCode)
    }
} catch (Exception ex) {
    ex.printStackTrace()
}

def runXjc = true
def cacheDirExists = false
if (cacheDir.exists()) {
    cacheDirExists = true
    if (bindingsChecksumFile.exists()) {
        String cachedBindingsChecksum = bindingsChecksumFile.text
        String latestBindingsChecksum = getChecksum(new File(bindingsFilePath), "SHA-256", xjcVersion)
        if (cachedBindingsChecksum.equals(latestBindingsChecksum)) {
            if (xsdChecksumFile.exists()) {
                String cachedXsdChecksum = xsdChecksumFile.text
                String latestXsdChecksum = getChecksum(new File(xsdFilePath), "SHA-256", xjcVersion)
                if (cachedXsdChecksum == latestXsdChecksum) {
                    System.out.println("Skipping xjc because the bindings file and XSD file have not changed " +
                            "nor has the version of xjc been changed.")
                    runXjc = false
                }
            }
        }
    }
}
new File("${project.build.directory}").mkdir()
new File("${project.build.directory}/generated-sources").mkdir()

if (runXjc) {
    processBuilder = new ProcessBuilder()
    processBuilder.command(System.getenv("JAVA_HOME") + "/bin/java", "-classpath",
            printClassPath(this.class.classLoader), "com.sun.tools.xjc.Driver",
            '-verbose', '-extension', '-Xtostring', '-Xinherit', '-Xsetters',
            '-d', "${project.build.directory}/generated-sources",
            '-b', bindingsFilePath,
            xsdFilePath)

    try {
        Process process = processBuilder.inheritIO().start()
        BufferedReader reader =
                new BufferedReader(new InputStreamReader(process.getInputStream()))

        String line
        while ((line = reader.readLine()) != null) {
            System.out.println(line)
        }

        int exitCode = process.waitFor()
        System.out.println("\nxjc exited with error code: " + exitCode)
        if (exitCode != 0) {
            throw new Exception("xjc failed with exit code: " + exitCode + ".")
        }

        if (!cacheDirExists) {
            cacheDir.mkdir()
        }
        // Save checksums of bindingsFilePath and xsdFilePath in bindingsChecksumFile and xsdChecksumFile respec.
        String latestBindingsChecksum = getChecksum(new File(bindingsFilePath), "SHA-256", xjcVersion)
        String latestXsdChecksum = getChecksum(new File(xsdFilePath), "SHA-256", xjcVersion)
        bindingsChecksumFile.withWriter { writer ->
            writer.write(latestBindingsChecksum)
        }
        xsdChecksumFile.withWriter { writer ->
            writer.write(latestXsdChecksum)
        }
    } catch (Exception ex) {
        ex.printStackTrace()
    }
}

// TODO: *if* qbwc.wsdl has changed, then call wsimport.
boolean runWsimport = true
String wsdlFilePath = "${project.basedir}/src/main/resources/wsdl/qbwc.wsdl"
File wsdlChecksumFile = new File("${project.build.directory}/jax-cache/" + wsdlFilePath.substring(wsdlFilePath.lastIndexOf('/'), wsdlFilePath.length()))
System.out.println("Cache dir exists: " + cacheDirExists)

if (cacheDirExists) {
    if (wsdlChecksumFile.exists()) {
        String cachedWsdlChecksum = wsdlChecksumFile.text
        String latestWsdlChecksum = getChecksum(new File(wsdlFilePath), "SHA-256", null)
        if (cachedWsdlChecksum == latestWsdlChecksum) {
            runWsimport = false
            System.out.println("Skipping wsimport because WSDL file hasn't changed")
        }
    }
}

if (runWsimport) {
    new File("${project.build.directory}/generated-classes").mkdir()
    String[] wsImportArgs = ['-verbose', '-keep',
                             '-Xnocompile',
                             '-d', "${project.build.directory}/generated-classes",
                             '-s', "${project.build.directory}/generated-sources",
                             wsdlFilePath]
    int wsImportExitCode = WsImport.doMain(wsImportArgs)
    System.out.println("\nwsimport exited with error code: " + wsImportExitCode)
    if (wsImportExitCode != 0) {
        throw new Exception("wsimport failed with exit code: " + wsImportExitCode + ".")
    }
    // Save checksums of wsdlFilepath in wsdlChecksumFile
    String latestWsdlChecksum = getChecksum(new File(wsdlFilePath), "SHA-256", null)
    wsdlChecksumFile.withWriter { writer ->
        writer.write(latestWsdlChecksum)
    }
}
