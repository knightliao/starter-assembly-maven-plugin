package org.apache.maven.plugin.assembly.mojos.utils;

import java.io.File;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.assembly.AssemblerConfigurationSource;
import org.apache.maven.plugin.assembly.archive.DefaultAssemblyArchiver;
import org.apache.maven.plugin.assembly.archive.phase.AssemblyArchiverPhase;
import org.apache.maven.plugin.assembly.archive.phase.DependencySetAssemblyPhase;
import org.apache.maven.plugin.assembly.artifact.DependencyResolutionException;
import org.apache.maven.plugin.assembly.artifact.DependencyResolver;
import org.apache.maven.plugin.assembly.model.Assembly;
import org.apache.maven.plugin.assembly.model.DependencySet;

/**
 *
 */
public class DependencyFinder {

    public DependencyFinder(AssemblerConfigurationSource configSource,
                            DefaultAssemblyArchiver assemblyArchiver,
                            List<Assembly> assemblies) throws MojoExecutionException {

        this.configSource = configSource;
        this.assemblies = assemblies;

        List<AssemblyArchiverPhase> assemblyPhases =
                getField(DefaultAssemblyArchiver.class, assemblyArchiver, "assemblyPhases");
        for (AssemblyArchiverPhase assemblyPhase : assemblyPhases) {
            if (assemblyPhase instanceof DependencySetAssemblyPhase) {
                DependencySetAssemblyPhase phase = (DependencySetAssemblyPhase) assemblyPhase;
                dependencyResolver = getField(DependencySetAssemblyPhase.class, phase, "dependencyResolver");
            }
        }

        if (dependencyResolver == null) {
            throw new MojoExecutionException("can not find dependencyResolver");
        }
    }

    private AssemblerConfigurationSource configSource;
    private List<Assembly> assemblies;
    private DependencyResolver dependencyResolver;

    public Set<File> find() throws MojoExecutionException {
        Set<File> dependencies = new HashSet<File>();

        for (Assembly assembly : assemblies) {
            try {
                Map<DependencySet, Set<Artifact>> dm =
                        dependencyResolver.resolveDependencySets(assembly, configSource, assembly.getDependencySets());
                for (Set<Artifact> artifacts : dm.values()) {
                    for (Artifact artifact : artifacts) {
                        dependencies.add(artifact.getFile());
                    }
                }
            } catch (DependencyResolutionException e) {
                throw new MojoExecutionException("find dependency due to error", e);
            }
        }

        return dependencies;
    }

    public static void setField(Class<?> clazz, Object target, String fieldName, Object value)
            throws MojoExecutionException {
        try {
            Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new MojoExecutionException("", e);
        }
    }

    public static <T> T getField(Class<?> clazz, Object target, String fieldName) throws MojoExecutionException {
        try {
            Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            return (T) field.get(target);
        } catch (Exception e) {
            throw new MojoExecutionException("", e);
        }
    }
}
