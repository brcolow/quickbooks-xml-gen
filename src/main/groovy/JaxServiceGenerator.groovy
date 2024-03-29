@Grab(group='org.glassfish.jaxb', module='jaxb-jxc', version='4.0.4')
import com.sun.tools.ws.WsGen

import java.lang.RuntimeException;
import java.util.stream.Collectors

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

def getWsGenInvocation() {
    return [System.getenv("JAVA_HOME") + "/bin/java", "-DenableExternalEntityProcessing=true",
            "-classpath",
            printClassPath(this.class.classLoader), "com.sun.tools.ws.WsGen",
            '-verbose', '-keep',
            '-d', "${project.build.directory}/generated-classes",
            '-s', "${project.build.directory}/generated-sources",
            '-r', "${project.build.directory}/generated-resources",
            'com.dow.quickbooks.QuickbooksWebConnector']
}

new File("${project.build.directory}/generated-resources").mkdir()

System.out.println("Running WsGen.main....")
if (System.getenv("JAVA_HOME") == null) {
    throw new RuntimeException("JAVA_HOME environment variable must be set!");
}
ProcessBuilder processBuilder = new ProcessBuilder()
System.out.println("Running:\n" + String.join(" ", getWsGenInvocation()))
processBuilder.command(getWsGenInvocation())

try {
    Process process = processBuilder.start()
    BufferedReader reader =
            new BufferedReader(new InputStreamReader(process.getInputStream()))

    String line
    while ((line = reader.readLine()) != null) {
        System.out.println(line)
    }

    int exitCode = process.waitFor()
    System.out.println("\nwsgen exited with error code : " + exitCode)
    if (exitCode != 0) {
        throw new Exception("wsgen failed with exit code: " + exitCode + ".")
    }
} catch (Exception ex) {
    ex.printStackTrace()
}