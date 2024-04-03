package cc.nuym.jnic;

import cc.nuym.jnic.env.SetupManager;
import cc.nuym.jnic.utils.DecryptorClass;
import cc.nuym.jnic.utils.TamperUtils;
import cc.nuym.jnic.xml.Config;
import org.apache.commons.compress.utils.IOUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;
import picocli.CommandLine;

import java.io.*;
import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;


public class Main {
    private File inputFile;
    static File output;


    public static void main(final String[] args) {
        System.out.println("\n");
        System.out.println("JNIC Java to C translator 3.7.1");
        System.out.println(" ~ (c) +Vincent Tang 2020-2024");
        System.out.println("\n");
        System.out.println("License: nuym (Enterprise)");
        SetupManager.init();
        System.exit(new CommandLine(new NativeObfuscatorRunner()).setCaseInsensitiveEnumValuesAllowed(true).execute(args));
    }

    @CommandLine.Command(name = "Jnic", mixinStandardHelpOptions = true, version = {"Jnic Bytecode Translator"}, description = {"将.jar文件翻译成.c文件并生成输出.jar文件"})
    private static class NativeObfuscatorRunner implements Callable<Integer> {
        @CommandLine.Parameters(index = "0", description = "Jar file to transpile")
        private File jarFile;

        @CommandLine.Parameters(index = "1", description = "Output directory")
        private String outputDirectory;

        @CommandLine.Option(names = {"-l", "--libraries"}, description = "Directory for dependent libraries")
        private File librariesDirectory;

        @CommandLine.Option(names = {"-a", "--annotations"}, description = "Use annotations to ignore/include native obfuscation")
        private boolean useAnnotations;
        @CommandLine.Option(names = {"--plain-lib-name"}, description = {"Common library name to be used for the loader"})
        private String libraryName;

        @Override
        public Integer call() throws Exception {
            System.out.println("Reading input jar " + this.jarFile);
            final List<Path> libs = new ArrayList<Path>();
            if (this.librariesDirectory != null) {
                Files.walk(this.librariesDirectory.toPath(), FileVisitOption.FOLLOW_LINKS).filter(f -> f.toString().endsWith(".jar") || f.toString().endsWith(".zip")).forEach(libs::add);
            }
            if (new File(this.outputDirectory).isDirectory()) {
                final File outFile = new File(this.outputDirectory, this.jarFile.getName());
                if (outFile.exists()) {
                    outFile.renameTo(new File(this.outputDirectory, this.jarFile.getName() + ".BACKUP"));
                }
            } else {
                final File outFile = new File(this.outputDirectory);
                if (outFile.exists()) {
                    outFile.renameTo(new File(this.outputDirectory + ".BACKUP"));
                }
            }
            //开始处理
            new NativeObfuscator().process(this.jarFile.toPath(), Paths.get(this.outputDirectory), libs, this.libraryName, this.useAnnotations);
            return 0;
        }
    }
}
