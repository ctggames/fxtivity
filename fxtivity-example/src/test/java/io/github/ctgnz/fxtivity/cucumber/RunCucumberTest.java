package io.github.ctgnz.fxtivity.cucumber;

import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

/**
 * The JUnit Platform Suite entrypoint Surefire discovers, running every {@code .feature} file under {@code src/test/resources/features} against the step definitions in this
 * package.
 * <p>
 * The scenarios are the library's specification, written against Fowler's own example - people, companies and the employments between them - extended with the cases his pattern
 * leaves to others: a name that changes over time, an office with one holder at a time, a board whose seats each have their own history, and departments related to people at both
 * ends.
 * <p>
 * The {@code html} plugin writes {@code target/cucumber/scenarios.html}, published to GitHub Pages so the scenarios are readable without cloning the repository.
 * <p>
 * {@link SelectClasspathResource} rather than {@code @SelectPackage("features")}: discovery logs a warning suggesting the latter, but against this project's classpath layout it
 * finds nothing at all.
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "io.github.ctgnz.fxtivity.cucumber")
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME, value = "html:target/cucumber/scenarios.html")
public class RunCucumberTest {
}
