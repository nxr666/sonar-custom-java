package org.sonar.custom.java.xml;

import org.sonar.api.batch.bootstrap.ProjectBuilder;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.bootstrap.ProjectReactor;

import java.io.File;
import java.util.List;

/**
 * Ensures MyBatis mapper XML under src/main/resources is indexed as main sources.
 */
public class MybatisXmlProjectBuilder extends ProjectBuilder {

    private static final String MAIN_RESOURCES = "src/main/resources";

    @Override
    protected void build(ProjectReactor reactor) {
        for (ProjectDefinition project : reactor.getProjects()) {
            File resourcesDir = new File(project.getBaseDir(), MAIN_RESOURCES);
            if (!resourcesDir.isDirectory()) {
                continue;
            }
            if (alreadyContainsResources(project.sources())) {
                continue;
            }
            project.addSources(MAIN_RESOURCES);
        }
    }

    private static boolean alreadyContainsResources(List<String> sources) {
        if (sources == null || sources.isEmpty()) {
            return false;
        }
        for (String source : sources) {
            if (source == null) {
                continue;
            }
            String normalized = source.replace('\\', '/');
            if (MAIN_RESOURCES.equals(normalized) || normalized.endsWith("/" + MAIN_RESOURCES)) {
                return true;
            }
        }
        return false;
    }
}
