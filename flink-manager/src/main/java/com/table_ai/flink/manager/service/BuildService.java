package com.table_ai.flink.manager.service;

import org.springframework.stereotype.Service;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

@Service
public class BuildService {

    public File buildJar(Path projectDir, File sourceFile) throws IOException, InterruptedException {
        // 1. Copy Template POM
        try (java.io.InputStream in = getClass().getResourceAsStream("/job-template/pom.xml")) {
            if (in == null) {
                throw new IOException("Template pom.xml not found in resources!");
            }
            java.nio.file.Files.copy(in, projectDir.resolve("pom.xml"),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }

        // 2. Execute Maven Build
        String fileName = sourceFile.getName();
        String className = fileName.substring(0, fileName.lastIndexOf('.'));
        String fullClassName = "com.table_ai.flink.generated." + className;

        System.out.println("Running Maven build for: " + className);
        ProcessBuilder pb = new ProcessBuilder("mvn", "clean", "package", "-DskipTests",
                "-Djob.main.class=" + fullClassName);
        pb.directory(projectDir.toFile());
        pb.redirectErrorStream(true);
        Process process = pb.start();

        // Capture output for debugging
        try (java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("[MVN] " + line);
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IOException("Maven build failed with exit code: " + exitCode);
        }

        // 3. Locate Generated JAR (Target directory)
        File targetDir = new File(projectDir.toFile(), "target");
        // Find the shaded jar (usually original-*.jar is the thin one, we want the
        // other one, or configured explicitly)
        // With shade plugin, the main jar is the shaded one.
        File[] jars = targetDir.listFiles((dir, name) -> name.endsWith(".jar") && !name.startsWith("original-"));

        if (jars == null || jars.length == 0) {
            throw new IOException("No JAR file found in target directory after build!");
        }
        File jarFile = jars[0];

        // 4. Wrap the JAR inside a ZIP file (AWS requirement: ZIP containing JAR)
        File zipFile = new File(targetDir, "package.zip");
        try (java.io.FileOutputStream fos = new java.io.FileOutputStream(zipFile);
                java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(fos);
                java.io.FileInputStream fis = new java.io.FileInputStream(jarFile)) {

            // Add JAR as entry in ZIP
            java.util.zip.ZipEntry entry = new java.util.zip.ZipEntry(jarFile.getName());
            zos.putNextEntry(entry);

            byte[] buffer = new byte[8192];
            int length;
            while ((length = fis.read(buffer)) >= 0) {
                zos.write(buffer, 0, length);
            }
            zos.closeEntry();
        }

        return zipFile;
    }
}
