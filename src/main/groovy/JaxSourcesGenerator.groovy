@Grab(group='com.sun.xml.ws', module='jaxws-tools', version='2.3.2')
import com.sun.tools.ws.WsImport
import java.util.stream.Collectors
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.MessageDigest
@Grab(group='org.jvnet.jaxb2_commons', module='jaxb2-basics', version='1.11.1')

def printClassPath(classLoader) {
    String classpath = ""
    classpath += classLoader.getURLs().stream()
            .map(url -> "${url.getPath().toString().substring(1)}")
            .collect(Collectors.joining(";"))
    if (classLoader.parent) {
        classpath += printClassPath(classLoader.parent)
    }
    return classpath
}

static def getChecksum(file, type) {
    def digest = MessageDigest.getInstance(type)
    def fos = file.newInputStream()
    def buffer = new byte[16384]
    def len

    while ((len = fos.read(buffer)) > 0) {
        digest.update(buffer, 0, len)
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
final File bindingsChecksumFile = new File("${project.build.directory}/jax-cache/bindings.jxb")
final File xsdChecksumFile = new File("${project.build.directory}/jax-cache/qbxmlops130.xsd")

// TODO: *if* bindings.jxb or qbxmlops130.xsd have changed, then call xjc.
def runXjc = true
def cacheDirExists = false
if (cacheDir.exists()) {
    cacheDirExists = true
    if (bindingsChecksumFile.exists()) {
        String cachedBindingsChecksum = bindingsChecksumFile.text
        String latestBindingsChecksum = getChecksum(new File(bindingsFilePath), "SHA-256")
        if (cachedBindingsChecksum.equals(latestBindingsChecksum)) {
            if (xsdChecksumFile.exists()) {
                String cachedXsdChecksum = xsdChecksumFile.text
                String latestXsdChecksum = getChecksum(new File(xsdFilePath), "SHA-256")
                if (cachedXsdChecksum == latestXsdChecksum) {
                    System.out.println("Skipping xjc because bindings.jxb and qbxmlops130.xsd haven't changed")
                    runXjc = false
                }
            }
        }
    }
}
new File("${project.build.directory}").mkdir()
new File("${project.build.directory}/generated-sources").mkdir()

if (runXjc) {
    ProcessBuilder processBuilder = new ProcessBuilder()
    processBuilder.command(System.getenv("JAVA_HOME") + "/bin/java", "-classpath",
            printClassPath(this.class.classLoader), "com.sun.tools.xjc.Driver",
            '-verbose', '-extension', '-XtoString', '-Xinheritance', '-Xsetters-mode=direct',
            '-d', "${project.build.directory}/generated-sources",
            '-b', "${project.basedir}/src/main/resources/bindings/bindings.jxb",
            "${project.basedir}/src/main/resources/xsd/qbxmlops130.xsd")

// TODO: Do we need to add "target/generated-sources" to compile scope?
    try {
        Process process = processBuilder.start()
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
        String latestBindingsChecksum = getChecksum(new File(bindingsFilePath), "SHA-256")
        String latestXsdChecksum = getChecksum(new File(xsdFilePath), "SHA-256")
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
File wsdlChecksumFile = new File("${project.build.directory}/jax-cache/qbwc.wsdl")

if (cacheDirExists) {
    if (wsdlChecksumFile.exists()) {
        String cachedWsdlChecksum = wsdlChecksumFile.text
        String latestWsdlChecksum = getChecksum(new File(wsdlFilePath), "SHA-256")
        if (cachedWsdlChecksum == latestWsdlChecksum) {
            runWsimport = false
            System.out.println("Skipping wsimport because qbwc.wsdl hasn't changed")
        }
    }
}

if (runWsimport) {
    new File("${project.build.directory}/generated-classes").mkdir()
    String[] wsImportArgs = ['-verbose', '-keep',
                             '-Xnocompile',
                             '-d', "${project.build.directory}/generated-classes",
                             '-s', "${project.build.directory}/generated-sources",
                             "${project.basedir}/src/main/resources/wsdl/qbwc.wsdl"]
    int wsImportExitCode = WsImport.doMain(wsImportArgs)
    System.out.println("\nwsimport exited with error code: " + wsImportExitCode)
    if (wsImportExitCode != 0) {
        throw new Exception("wsimport failed with exit code: " + wsImportExitCode + ".")
    }
    // Save checksums of wsdlFilepath in wsdlChecksumFile
    String latestWsdlChecksum = getChecksum(new File(wsdlFilePath), "SHA-256")
    wsdlChecksumFile.withWriter { writer ->
        writer.write(latestWsdlChecksum)
    }
}
