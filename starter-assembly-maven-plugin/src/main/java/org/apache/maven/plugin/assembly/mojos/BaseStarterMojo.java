package org.apache.maven.plugin.assembly.mojos;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.assembly.io.MyAssemblyReader;
import org.apache.maven.plugin.assembly.mojos.utils.DependencyFinder;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 *
 */
public abstract class BaseStarterMojo extends AbstractAssemblyMojo {

    protected abstract String getFormat();

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    protected MavenProject project;

    @Override
    public MavenProject getProject() {
        return project;
    }

    protected void setAssemblyReader() throws MojoExecutionException {
        DependencyFinder.setField(AbstractAssemblyMojo.class, this, "assemblyReader", new MyAssemblyReader());
    }
}
