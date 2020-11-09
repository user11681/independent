package user11681.independent;

import java.io.File;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class Main {
    private static final int SUCCESS = 0;
    private static final int VALID = 1;
    private static final int INVALID = 2;

    private static boolean recursive;

    public static void main(String[] argumentArray) throws Throwable {
        final List<String> arguments = new ArrayList<>(Arrays.asList(argumentArray));
        final List<String> switches = new ArrayList<>();
        final List<String> options = new ArrayList<>();

        for (final String argument : argumentArray) {
            if (argument.matches("--[^-]{2,}")) {
                options.add(argument.toLowerCase(Locale.ROOT));
                arguments.remove(argument);
            } else if (argument.matches("-[^-]+")) {
                switches.add(argument.toLowerCase(Locale.ROOT));
                arguments.remove(argument);
            }
        }

        if (switches.contains("-r") || options.contains("--recursive")) {
            recursive = true;
        }

        if (arguments.size() == 0) {
            final String path = System.getProperty("user.dir");

            System.out.printf("Using the current directory (%s) because arguments were not given.\n", path);

            arguments.add(path);
        }

        int modificationCount = 0;

        for (final String argument : arguments) {
            final File path = new File(argument);

            if (path.exists()) {
                modificationCount += processPath(path);
            } else {
                System.err.printf("\"%s\" is not a valid path.\n", path);
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

    private static int processPath(final File file) throws Throwable {
        int modificationCount = 0;

        if (file.isDirectory()) {
            modificationCount += processDirectory(file);
        } else {
            final int status = removeDependencies(file.toPath());

            if (status == INVALID) {
                System.err.printf("\"%s\" is not a Fabric mod JAR or directory.\n", file);
            } else if (status == SUCCESS) {
                ++modificationCount;
            }
        }

        return modificationCount;
    }

    @SuppressWarnings("ConstantConditions")
    private static int processDirectory(final File file) throws Throwable {
        int modificationCount = 0;

        for (final File subfile : file.listFiles()) {
            if (recursive && subfile.isDirectory()) {
                modificationCount += processDirectory(subfile);
            }

            if (removeDependencies(subfile.toPath()) == SUCCESS) {
                ++modificationCount;
            }
        }

        return modificationCount;
    }

    private static int removeDependencies(Path modPath) throws Throwable {
        modPath = modPath.toRealPath();

        if (modPath.toString().endsWith(".jar")) {
            final FileSystem mod = FileSystems.newFileSystem(modPath, Main.class.getClassLoader());

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
