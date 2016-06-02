package org.apache.maven.plugin.assembly.mojos;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.BuildBase;
import org.apache.maven.model.Profile;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import com.knightliao.plugin.starter.assembly.utils.MavenFileUtils;

/**
 *
 */
@Mojo(name = StarterBin.FORMAT, aggregator = true, inheritByDefault = false, requiresDependencyResolution =
        ResolutionScope.COMPILE)
@Execute(phase = LifecyclePhase.PACKAGE)
public class StarterBin extends BaseStarterMojo {

    public static final String FORMAT = "bin";

    @Override
    protected String getFormat() {
        return FORMAT;
    }

    @Parameter
    private File startShell;

    @Parameter
    private File stopShell;

    private String folderName = "starter-run";

    @Override
    public String[] getDescriptors() {
        return new String[] {"META-INF/starter.bin.xml"};
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        setAssemblyReader();

        String shellFolder = folderName;

        //
        // start shell
        if (startShell != null) {
            MavenFileUtils.copyFile(getProject(), shellFolder, startShell, "start.sh");
        } else {
            getLog().info("use default start shell");
            MavenFileUtils.copyFile(getProject(),
                    shellFolder,
                    StarterBin.class.getResource("/META-INF/start.sh"),
                    "start.sh");
        }

        //
        // stop shell
        if (stopShell != null) {
            MavenFileUtils.copyFile(getProject(), shellFolder, stopShell, "stop.sh");
        } else {
            getLog().info("use default stop shell");
            MavenFileUtils.copyFile(getProject(),
                    shellFolder,
                    StarterBin.class.getResource("/META-INF/stop.sh"),
                    "stop.sh");
        }

        //
        // profile resources
        //

        String resourceFolder = folderName + File.separator + "conf";

        List<Resource> resources = new ArrayList<Resource>();

        List<Profile> activeProfiles = project.getActiveProfiles();
        for (Profile profile : activeProfiles) {
            getLog().info(String.format("add resource for profile %s", profile));
            BuildBase buildBase = profile.getBuild();
            if (buildBase != null) {
                List<Resource> curResources = buildBase.getResources();
                resources.addAll(curResources);
            }
        }

        for (Resource resource : resources) {

            String directory = resource.getDirectory();
            if (new File(directory).isDirectory()) {
                Collection<File> files = FileUtils.listFilesAndDirs(new File(directory),
                        FileFilterUtils.trueFileFilter(),
                        FileFilterUtils.trueFileFilter());
                for (File file : files) {
                    getLog().info(String.format("add file:[%s] as conf", file.getAbsolutePath()));
                    MavenFileUtils.copyFile(project, resourceFolder, file, file.getAbsolutePath().replace
                            (directory, ""));
                }
            }
        }

        super.execute();

        //
        // dependency
        //

        String libFolder = folderName + File.separator + "lib";

        Set<Artifact> artifacts = project.getArtifacts();
        for (Artifact artifact : artifacts) {

            getLog().info(String.format("add file:[%s] as lib", artifact.getFile().getName()));
            MavenFileUtils.copyFile(project, libFolder, artifact.getFile(), artifact.getFile().getName());
        }
        MavenFileUtils
                .copyFile(project, libFolder, project.getArtifact().getFile(), project.getArtifact().getFile().getName
                        ());
    }

}
