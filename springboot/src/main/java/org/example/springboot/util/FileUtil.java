package org.example.springboot.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class FileUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(FileUtil.class);
    public static final String FILE_BASE_PATH = System.getProperty("user.dir") + "/files/";
    private static final Set<String> IMAGE_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png", ".webp", ".gif", ".ico");
    private static final Set<String> VIDEO_EXTENSIONS = Set.of(".mp4", ".webm", ".mov");
    private static final Set<String> COMMON_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png", ".webp", ".gif", ".ico", ".mp4", ".webm", ".mov", ".pdf", ".txt");

    public static Path getProjectRootPath() throws IOException {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources("classpath*:.");
        if (resources.length == 0) {
            throw new IOException("Cannot find project root path.");
        }
        File rootDir = resources[0].getFile();
        return rootDir.toPath();
    }

    public static String saveFile(MultipartFile file, String folderName, String baseDir) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            return null;
        }

        String extension = getExtension(originalFilename);
        if (!isAllowedExtension(baseDir, extension)) {
            LOGGER.warn("Rejected upload by extension: originalFilename={}, baseDir={}", originalFilename, baseDir);
            return null;
        }

        Path projectRootPath = Paths.get(FILE_BASE_PATH).normalize();
        Path fileDirectory = projectRootPath.resolve(baseDir).normalize();
        if (folderName != null && !folderName.isBlank()) {
            fileDirectory = fileDirectory.resolve(Paths.get(folderName).getFileName().toString()).normalize();
        }
        if (!fileDirectory.startsWith(projectRootPath)) {
            LOGGER.warn("Rejected upload directory traversal: baseDir={}, folderName={}", baseDir, folderName);
            return null;
        }

        String storedFileName = System.currentTimeMillis() + "-" + UUID.randomUUID() + extension;
        Path uploadFilePath = fileDirectory.resolve(storedFileName).normalize();
        if (!uploadFilePath.startsWith(projectRootPath)) {
            LOGGER.warn("Rejected upload path traversal: {}", uploadFilePath);
            return null;
        }

        try {
            Files.createDirectories(fileDirectory);
            file.transferTo(uploadFilePath.toFile());
            LOGGER.info("File saved at: {}", uploadFilePath.toAbsolutePath());
        } catch (IOException e) {
            LOGGER.error("Save file failed: originalFilename={}, baseDir={}, folderName={}", originalFilename, baseDir, folderName, e);
            return null;
        }

        return "/" + baseDir + "/" + (folderName != null && !folderName.isBlank() ? Paths.get(folderName).getFileName() + "/" : "") + storedFileName;
    }

    public static String saveImage(MultipartFile file, String folderName) {
        return saveFile(file, folderName, "img");
    }

    public static String saveVideo(MultipartFile file, String folderName) {
        return saveFile(file, folderName, "videos");
    }

    public static boolean deleteFile(String filename) {
        try {
            if (filename.startsWith("/")) {
                filename = filename.substring(1);
            }

            Path basePath = Paths.get(FILE_BASE_PATH).normalize();
            Path filePath = basePath.resolve(filename).normalize();
            if (!filePath.startsWith(basePath)) {
                LOGGER.warn("Rejected delete path traversal: {}", filename);
                return false;
            }

            if (Files.exists(filePath)) {
                Files.delete(filePath);
                LOGGER.info("File deleted: {}", filePath);
                return true;
            }
            LOGGER.warn("File not found: {}", filePath);
            return false;
        } catch (Exception e) {
            LOGGER.error("Error deleting file: {}", filename, e);
            return false;
        }
    }

    public static void writeToFile(String fileName, String content) throws IOException {
        File file = new File(fileName);
        LOGGER.debug("Writing to file: {}", file.getAbsolutePath());
        try (FileWriter fileWriter = new FileWriter(file)) {
            fileWriter.write(content);
        }
    }

    private static String getExtension(String originalFilename) {
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex <= 0 || dotIndex == originalFilename.length() - 1) {
            return "";
        }
        return originalFilename.substring(dotIndex).toLowerCase(Locale.ROOT);
    }

    private static boolean isAllowedExtension(String baseDir, String extension) {
        if (extension == null || extension.isBlank()) {
            return false;
        }
        if ("img".equalsIgnoreCase(baseDir)) {
            return IMAGE_EXTENSIONS.contains(extension);
        }
        if ("videos".equalsIgnoreCase(baseDir)) {
            return VIDEO_EXTENSIONS.contains(extension);
        }
        return COMMON_EXTENSIONS.contains(extension);
    }
}
