package com.knightliao.plugin.starter.assembly.utils;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;

/*

 */
public class MavenFileUtils {

    public static void copyFile(MavenProject project, String format, URL url, String targetName)
            throws MojoExecutionException {
        if (StringUtils.isBlank(targetName)) {
            return;
        }

        File file = new File(project.getBuild().getDirectory() + "/" + format + "/" + targetName);

        if (!file.getParentFile().exists()) {
            mkParent(file.getParentFile());
        }

        try {
            FileUtils.copyURLToFile(url, file);
        } catch (IOException e) {
            throw new MojoExecutionException(String.format("copy url:[%s] due to error", targetName), e);
        }
    }

    public static void copyFile(MavenProject project, String format, File source, String targetName)
            throws MojoExecutionException {
        if (source.isDirectory()) {
            return;
        }

        try {
            copyFile(project, format, source.toURI().toURL(), targetName);
        } catch (Exception e) {
            throw new MojoExecutionException(String.format("copy file:[%s] due to error", targetName), e);
        }
    }

    public static void mkParent(File parentFile) {
        if (!parentFile.isDirectory()) {
            return;
        }

        if (!parentFile.getParentFile().exists()) {
            mkParent(parentFile.getParentFile());
        }

        parentFile.mkdir();
    }

}
