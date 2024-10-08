package com.github.tlbueno.artemis_scenarios.helpers;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Utility class for file related methods
 */
@EqualsAndHashCode()
@Log4j2
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@ToString(callSuper = true)
public class FileHelper {
    private static final String PROJECT_USER_DIR = System.getProperty("user.dir");

    /**
     * Get the project file path relative to the project root
     *
     * @param projectRelativeFile file name (may contain sub directories)
     * @return the file with full path
     */
    public static String getProjectRelativeFile(String projectRelativeFile) {
        return getProjectRelativeFilePath(projectRelativeFile).toString();
    }

    /**
     * Get the project file path relative to the project root
     *
     * @param projectRelativeFile file name (may contain sub directories)
     * @return the resulting path of the file
     */
    public static Path getProjectRelativeFilePath(String projectRelativeFile) {
        LOGGER.debug("Getting project relative file path from project: {}, path: {}",
                PROJECT_USER_DIR, projectRelativeFile);
        return Paths.get(PROJECT_USER_DIR, projectRelativeFile).toAbsolutePath();
    }
}
