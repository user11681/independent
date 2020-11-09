package user11681.independent;

import java.io.File;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

public class Main {
    private static final int SUCCESS = 0;
    private static final int VALID = 1;
    private static final int INVALID = 2;

    private static boolean recursive;

    public static void main(String[] arguments) throws Throwable {
        final List<String> switches = new ArrayList<>();
        final List<String> options = new ArrayList<>();

        for (final String argument : arguments) {
            if (argument.matches("--[^-]{2,}")) {
                options.add(argument.toLowerCase(Locale.ROOT));
            } else if (argument.matches("-[^-]+")) {
                switches.add(argument.toLowerCase(Locale.ROOT));
            }
        }

        if (switches.contains("-r") || options.contains("--recursive")) {
            recursive = true;
        }

        if (arguments.length == 0) {
            System.out.println("Enter the path of a mod JAR or directory without escaping any whitespace characters.");

            arguments = new String[]{new Scanner(System.in).nextLine()};
        }

        int modificationCount = 0;

        for (final String path : arguments) {
            final File file = new File(path);

            if (file.exists()) {
                modificationCount += processFile(file);
            } else {
                System.err.printf("\"%s\" is not a valid path.\n", file);
            }
        }

        switch (modificationCount) {
            case 0:
                System.out.println("No mods to modify.");

                break;
            case 1:
                System.out.println("Modified 1 mod.");

                break;
            default:
                System.out.println("Modified " + modificationCount + " mods.");

                break;
        }
    }

    @SuppressWarnings("ConstantConditions")
    private static int processFile(final File file) throws Throwable {
        int modificationCount = 0;

        if (file.isDirectory()) {
            for (final File subfile : file.listFiles()) {
                if (recursive) {
                    modificationCount += processFile(subfile);
                }

                if (removeDependencies(subfile) == SUCCESS) {
                    ++modificationCount;
                }
            }
        } else {
            final int status = removeDependencies(file);

            if (status == INVALID) {
                System.err.printf("\"%s\" is not a Fabric mod JAR or directory.\n", file);
            } else if (status == SUCCESS) {
                ++modificationCount;
            }
        }

        return modificationCount;
    }

    private static int removeDependencies(final File file) throws Throwable {
        if (file.getName().endsWith(".jar")) {
            final FileSystem mod = FileSystems.newFileSystem(file.toPath(), Main.class.getClassLoader());

            try {
                final Path metadata = mod.getPath("fabric.mod.json");
                String content = new String(Files.readAllBytes(metadata));
                int dependsIndex = content.indexOf("\"depends\"");

                if (dependsIndex >= 0) {
                    int dependsEnd = content.indexOf('}', dependsIndex);
                    final int commaIndex = content.indexOf(',', dependsEnd);

                    if (commaIndex >= 0) {
                        dependsEnd = commaIndex;
                    } else {
                        final int previousComma = content.substring(0, dependsIndex).lastIndexOf(',');

                        if (previousComma >= 0) {
                            dependsIndex = previousComma;
                        }
                    }

                    content = content.substring(0, dependsIndex) + content.substring(dependsEnd + 1);

                    Files.write(metadata, content.getBytes());

                    mod.close();

                    return SUCCESS;
                }

                return VALID;
            } catch (final Throwable throwable) {
                return INVALID;
            }
        }

        return INVALID;
    }
}
