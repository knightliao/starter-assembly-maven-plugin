package org.apache.maven.plugin.assembly.io;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.assembly.AssemblerConfigurationSource;
import org.apache.maven.plugin.assembly.InvalidAssemblerConfigurationException;
import org.apache.maven.plugin.assembly.model.Assembly;
import org.apache.maven.shared.io.location.ClasspathResourceLocatorStrategy;
import org.apache.maven.shared.io.location.FileLocatorStrategy;
import org.apache.maven.shared.io.location.Location;
import org.apache.maven.shared.io.location.Locator;
import org.apache.maven.shared.io.location.LocatorStrategy;
import org.apache.maven.shared.utils.ReaderFactory;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.IOUtil;

/**
 *
 */
public class MyAssemblyReader extends DefaultAssemblyReader {

    protected List<Assembly> assemblies = new ArrayList<Assembly>();

    public List<Assembly> getAssemblies() {
        return assemblies;
    }

    public List<Assembly> readAssemblies(final AssemblerConfigurationSource configSource)
            throws AssemblyReadException, InvalidAssemblerConfigurationException {
        final Locator locator = new Locator();

        final List<LocatorStrategy> strategies = new ArrayList<LocatorStrategy>();
        strategies.add(new RelativeFileLocatorStrategy(configSource.getBasedir()));
        strategies.add(new FileLocatorStrategy());
        strategies.add(new ClasspathResourceLocatorStrategy());

        final List<LocatorStrategy> refStrategies = new ArrayList<LocatorStrategy>();
        refStrategies.add(new PrefixedClasspathLocatorStrategy("/assemblies/"));

        final String descriptor = configSource.getDescriptor();
        final String descriptorId = configSource.getDescriptorId();
        final String[] descriptors = configSource.getDescriptors();
        final String[] descriptorRefs = configSource.getDescriptorReferences();
        final File descriptorSourceDirectory = configSource.getDescriptorSourceDirectory();

        if (descriptor != null) {
            locator.setStrategies(strategies);
            addAssemblyFromDescriptor(descriptor, locator, configSource, assemblies);
        }

        if (descriptorId != null) {
            locator.setStrategies(refStrategies);
            addAssemblyForDescriptorReference(descriptorId, configSource, assemblies);
        }

        if ((descriptors != null) && (descriptors.length > 0)) {
            locator.setStrategies(strategies);
            for (String descriptor1 : descriptors) {
                getLogger().info("Reading assembly descriptor: " + descriptor1);
                addAssemblyFromDescriptor(descriptor1, locator, configSource, assemblies);
            }
        }

        if ((descriptorRefs != null) && (descriptorRefs.length > 0)) {
            locator.setStrategies(refStrategies);
            for (String descriptorRef : descriptorRefs) {
                addAssemblyForDescriptorReference(descriptorRef, configSource, assemblies);
            }
        }

        if ((descriptorSourceDirectory != null) && descriptorSourceDirectory.isDirectory()) {
            RelativeFileLocatorStrategy locatorStrategy = new RelativeFileLocatorStrategy(descriptorSourceDirectory);
            locator.setStrategies(Collections.singletonList(locatorStrategy));

            final DirectoryScanner scanner = new DirectoryScanner();
            scanner.setBasedir(descriptorSourceDirectory);
            scanner.setIncludes(new String[] {"**/*.xml"});
            scanner.addDefaultExcludes();

            scanner.scan();

            final String[] paths = scanner.getIncludedFiles();

            for (String path : paths) {
                addAssemblyFromDescriptor(path, locator, configSource, assemblies);
            }
        }

        if (assemblies.isEmpty()) {
            if (configSource.isIgnoreMissingDescriptor()) {
                getLogger().debug(
                        "Ignoring missing assembly descriptors per configuration. See messages above for specifics.");
            } else {
                throw new AssemblyReadException("No assembly descriptors found.");
            }
        }

        // check unique IDs
        final Set<String> ids = new HashSet<String>();
        for (final Assembly assembly : assemblies) {
            if (!ids.add(assembly.getId())) {
                getLogger().warn("The assembly id " + assembly.getId() + " is used more than once.");
            }

        }
        return assemblies;
    }

    private Assembly addAssemblyFromDescriptor(final String spec,
                                               final Locator locator,
                                               final AssemblerConfigurationSource configSource,
                                               final List<Assembly> assemblies)
            throws AssemblyReadException, InvalidAssemblerConfigurationException {
        final Location location = locator.resolve(spec);

        if (location == null) {
            if (configSource.isIgnoreMissingDescriptor()) {
                getLogger().debug("Ignoring missing assembly descriptor with ID '"
                        + spec
                        + "' per configuration.\nLocator output was:\n\n"
                        + locator.getMessageHolder().render());
                return null;
            } else {
                throw new AssemblyReadException("Error locating assembly descriptor: "
                        + spec
                        + "\n\n"
                        + locator.getMessageHolder().render());
            }
        }

        Reader r = null;
        try {
            r = ReaderFactory.newXmlReader(location.getInputStream());

            File dir = null;
            if (location.getFile() != null) {
                dir = location.getFile().getParentFile();
            }

            final Assembly assembly = readAssembly(r, spec, dir, configSource);

            assemblies.add(assembly);

            return assembly;
        } catch (final IOException e) {
            throw new AssemblyReadException("Error reading assembly descriptor: " + spec, e);
        } finally {
            IOUtil.close(r);
        }

    }

    private Assembly addAssemblyForDescriptorReference(final String ref,
                                                       final AssemblerConfigurationSource configSource,
                                                       final List<Assembly> assemblies)
            throws AssemblyReadException, InvalidAssemblerConfigurationException {
        final InputStream resourceAsStream =
                Thread.currentThread().getContextClassLoader().getResourceAsStream("assemblies/" + ref + ".xml");

        if (resourceAsStream == null) {
            if (configSource.isIgnoreMissingDescriptor()) {
                getLogger().debug("Ignoring missing assembly descriptor with ID '" + ref + "' per configuration.");
                return null;
            } else {
                throw new AssemblyReadException("Descriptor with ID '" + ref + "' not found");
            }
        }

        Reader reader = null;
        try {
            reader = ReaderFactory.newXmlReader(resourceAsStream);
            final Assembly assembly = readAssembly(reader, ref, null, configSource);

            assemblies.add(assembly);
            return assembly;
        } catch (final IOException e) {
            throw new AssemblyReadException("Problem with descriptor with ID '" + ref + "'", e);
        } finally {
            IOUtils.closeQuietly(reader);
        }
    }
}
