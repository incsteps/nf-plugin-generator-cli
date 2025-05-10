package com.incsteps.nextflow;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import io.micronaut.configuration.picocli.PicocliRunner;
import io.micronaut.context.ApplicationContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Command(name = "generator", description = "...",
        mixinStandardHelpOptions = true)
public class GeneratorCommand implements Runnable {

    private final Logger logger = LoggerFactory.getLogger(GeneratorCommand.class);

    @Option(names = {"-v", "--verbose"}, description = "verbose mode")
    boolean verbose;

    @Option(names = {"-p", "--package"}, description = "The java package to use")
    String packageName="nextflow.hellow";

    @Option(names = {"-d", "--dir"}, description = "Output dir")
    String outputDir="/tmp";

    @Option(names = {"--dev"}, description = "Development mode, don't use in production")
    Boolean dev=false;

    @Option(names = {"-n", "--nf-version"}, description = "The nextflow version to use")
    String version ="24.12.0-edge";

    //@Option(names = {"-r", "--repository"}, description = "Plugin generator Git repository")
    String repository ="https://github.com/incsteps/nf-plugin-generator-cli.git";

    //@Option(names = {"-b", "--branch"}, description = "Git branch to use")
    String branch ="main";

    //@Option(names = {"-l", "--lang"}, description = "java or groovy")
    String lang = "groovy";

    @Parameters(paramLabel = "name", description = "the name of your plugin")
    String name;

    public static void main(String[] args) throws Exception {
        PicocliRunner.run(GeneratorCommand.class, args);
    }

    public void run() {
        // business logic here
        if (verbose) {
            LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
            ch.qos.logback.classic.Logger rootLogger = loggerContext.getLogger("com.incsteps.nextflow");
            rootLogger.setLevel(Level.DEBUG);
        }

        var directory = Paths.get(outputDir, name).toFile();
        if( directory.exists() ) {
            logger.error("The directory '{}' already exists.", name);
            System.err.printf("The directory %s already exists", name);
            System.exit(1);
            return;
        }

        if( !directory.mkdirs() ){
            logger.error("Can't create directory '{}'.", name);
            System.err.printf("Can't create directory %s", name);
            System.exit(1);
            return;
        }

        logger.info("Retrieving last version...");
        var git = new GitDownloader();
        var tmpDir = git.pullRepo(repository, branch);

        logger.info("Generating plugin skeleton...");
        var packageDir = packageName.replace('.', File.separatorChar);
        logger.info("Generating {}", packageDir);

        try {
            String basePath = dev ? "." : tmpDir.getAbsolutePath();
            copyResources(basePath + "/skeletons/base", directory.getAbsolutePath());
            copyResources(basePath +"/skeletons/"+lang, directory.getAbsolutePath()+"/src/main/"+lang+"/"+packageDir);
            copyResources(basePath +"/skeletons/test", directory.getAbsolutePath()+"/src/test/"+lang+"/"+packageDir);
            copyResources(basePath +"/skeletons/validation", directory.getAbsolutePath()+"/validation");

            fixPackages(directory.getAbsolutePath()+"/src/main/"+lang+"/"+packageDir, packageName, name);
            fixPackages(directory.getAbsolutePath()+"/src/test/groovy/"+packageDir, packageName, name);

            fixConfiguration(directory.getAbsolutePath(), packageName, name, version);

            logger.info("""
                    Plugin {} created
                    Follow next steps:
                    cd {}
                    ./gradlew installPlugin  # build and install the plugin in local
                    
                    You can validate the installation using the example provided at validation directory:
                    cd validation
                    nextflow run main.nf
                
                    In order to publish your plugin, please remember to change gradle.properties with your user, organization, etc
                    """.stripIndent(), name, directory.getAbsolutePath()+"/"+name);

        }catch (Exception e) {
            logger.error("Error {}", e.getMessage(), e);
            System.err.printf("Error %s", e.getMessage());
            System.exit(1);
        }
    }

    private static void copyResources(String resourcePath, String destinationDir) throws IOException {
        Path destinationPath = Paths.get(destinationDir);
        if (!Files.exists(destinationPath)) {
            Files.createDirectories(destinationPath);
        }

        Path resourcePathObj = Paths.get(resourcePath);
        Files.walkFileTree(resourcePathObj, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (!Files.isDirectory(file)) {
                    // Calcular la ruta relativa del archivo dentro de la carpeta de recursos.
                    Path relativePath = resourcePathObj.relativize(file);
                    // Calcular la ruta de destino para el archivo.
                    Path destinationFilePath = destinationPath.resolve(relativePath);
                    // Asegurar que el directorio padre existe.
                    Files.createDirectories(destinationFilePath.getParent());
                    // Copiar el archivo al directorio de destino.
                    Files.copy(file, destinationFilePath, StandardCopyOption.REPLACE_EXISTING);
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static void fixPackages(String basePath, String packageName, String name) throws IOException {
        Path resourcePathObj = Path.of(basePath);
        Files.walkFileTree(resourcePathObj, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (!Files.isDirectory(file)) {
                    String source = Files.readString(file.toAbsolutePath());
                    source = source.replaceAll("\\{\\{packageName}}", packageName);
                    source = source.replaceAll("\\{\\{pluginName}}", name);
                    Files.writeString(file.toAbsolutePath(), source);
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static void fixConfiguration(String basePath, String packageName, String name, String version) throws IOException {
        configureGradleProperties(basePath, packageName, version);
        configureGradleSettings(basePath, name);
        configureExtensions(basePath, packageName);
        configureValidation(basePath, name);
    }

    private static void configureGradleProperties(String basePath, String packageName, String version) throws IOException {
        var properties = new Properties();
        properties.load(new FileInputStream(basePath+"/gradle.properties"));
        properties.setProperty("nextflow_version", version);
        properties.setProperty("classname", packageName+".Plugin");
        properties.store(new FileOutputStream(basePath+"/gradle.properties"),"Generated by nf-plugin-generator");
    }

    private static void configureGradleSettings(String basePath, String name) throws IOException {
        var build = Files.readString(Path.of(basePath + "/settings.gradle"));
        build = build.replace("{{PROJECT}}", name);
        Files.writeString(Path.of(basePath + "/settings.gradle"), build);
    }

    private static void configureExtensions(String basePath, String packageName) throws IOException {
        var build = Files.readString(Path.of(basePath + "/build.gradle"));
        build = build.replace("{{EXTENSIONS}}", buildExtensions(packageName));
        Files.writeString(Path.of(basePath + "/build.gradle"), build);
    }

    private static void configureValidation(String basePath, String name) throws IOException {
        var config = Files.readString(Path.of(basePath + "/validation/nextflow.config"));
        config = config.replace("{{PROJECT}}", name);
        Files.writeString(Path.of(basePath + "/validation/nextflow.config"), config);

        var main = Files.readString(Path.of(basePath + "/validation/main.nf"));
        main = main.replace("{{PROJECT}}", name);
        Files.writeString(Path.of(basePath + "/validation/main.nf"), main);
    }

    private static String buildExtensions(String packageName){
        return Stream.of(
                "'"+packageName+".FunctionsExtension"+"'"
        ).collect(Collectors.joining(","));
    }
}
