package util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
/**
 * FileUtils
 */
public class FileUtils {
    private FileUtils() {
        // Utility class
    }   

    private static <T> List<T> listFile(Path directory, Function<Path, T> mapper) throws IOException {
        try (Stream<Path> stream = Files.walk(directory)) {
            return stream
                    .filter(path -> !path.equals(directory))
                    .filter(Files::isRegularFile)
                    .map(mapper)
                    .collect(Collectors.toList());
        }
    }

    private static <T> List<T> listFile(Path directory, Function<Path, T> mapper, int depth) throws IOException {
        try (Stream<Path> stream = Files.walk(directory, depth)) {
            return stream
                    .filter(path -> !path.equals(directory))
                    .filter(Files::isRegularFile)
                    .map(mapper)
                    .collect(Collectors.toList());
        }
    }
    
    private static <T> List<T> listDirectory(Path directory, Function<Path, T> mapper) throws IOException {
        try (Stream<Path> stream = Files.walk(directory)) {
            return stream
                    .filter(path -> !path.equals(directory))
                    .filter(Files::isDirectory)
                    .map(mapper)
                    .collect(Collectors.toList());
        }
    }

    private static <T> List<T> listDirectory(Path directory, Function<Path, T> mapper, int depth) throws IOException {
        try (Stream<Path> stream = Files.walk(directory, depth)) {
            return stream
                    .filter(path -> !path.equals(directory))
                    .filter(Files::isDirectory)
                    .map(mapper)
                    .collect(Collectors.toList());
        }
    }

    public static List<Path> listFilePaths(Path directory) throws IOException {
        return listFile(directory, path -> path);
    }

    public static List<Path> listFilePaths(String directory) throws IOException {
        return listFilePaths(Paths.get(directory));
    }

    public static List<String> listFilePaths(Path directory, int depth) throws IOException {
        return listFile(directory, path -> directory.relativize(path).toString().replace('\\', '/'), depth);
    }

    public static List<String> listFilePaths(String directory, int depth) throws IOException {
        return listFilePaths(Paths.get(directory), depth);
    }

    public static List<String> listFileNames(Path directory) throws IOException {
        return listFile(directory, path -> path.getFileName().toString());
    }

    public static List<File> listFilesAsFileObjects(Path directory) throws IOException {
        return listFile(directory, Path::toFile);
    }

    public static List<String> listDirectories(Path directory) throws IOException{
        return listDirectory(directory, path -> path.getFileName().toString());
    }

    public static List<String> listDirectories(Path directory, int depth) throws IOException{
        return listDirectory(directory, path -> path.getFileName().toString(), depth);
    }
    
}