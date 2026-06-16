package org.sonar.custom.java;

import org.sonar.custom.java.xml.MybatisMapperXmlSensor;
import org.sonar.custom.java.xml.MybatisXmlProjectBuilder;
import org.sonar.custom.java.xml.MybatisXmlRulesDefinition;
import org.sonar.api.Plugin;

/**
 * Entry point of the SonarQube plugin.
 * This class is referenced in pom.xml as the plugin class.
 */
public class CustomJavaRulesPlugin implements Plugin {

    @Override
    public void define(Context context) {
        context.addExtension(CustomJavaRulesDefinition.class);
        context.addExtension(CustomJavaFileCheckRegistrar.class);
        context.addExtension(MybatisXmlRulesDefinition.class);
        context.addExtension(MybatisXmlProjectBuilder.class);
        context.addExtension(MybatisMapperXmlSensor.class);
    }
}
