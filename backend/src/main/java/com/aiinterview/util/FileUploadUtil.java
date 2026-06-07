package com.aiinterview.util;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class FileUploadUtil {

    private FileUploadUtil() {
    }

    public static void saveMultipart(MultipartFile file, Path target) throws IOException {
        Path absolute = target.toAbsolutePath().normalize();
        Files.createDirectories(absolute.getParent());
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, absolute, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
