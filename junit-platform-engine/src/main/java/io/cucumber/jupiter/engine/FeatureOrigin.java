package io.cucumber.jupiter.engine;

import gherkin.ast.Examples;
import gherkin.ast.Location;
import gherkin.ast.Node;
import gherkin.ast.ScenarioOutline;
import gherkin.ast.TableRow;
import io.cucumber.core.feature.CucumberFeature;
import io.cucumber.core.feature.CucumberPickle;
import io.cucumber.core.io.Classpath;
import org.junit.platform.engine.TestSource;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.ClasspathResourceSource;
import org.junit.platform.engine.support.descriptor.FilePosition;
import org.junit.platform.engine.support.descriptor.FileSource;
import org.junit.platform.engine.support.descriptor.UriSource;

import java.net.URI;

import static io.cucumber.core.io.Classpath.CLASSPATH_SCHEME_PREFIX;

abstract class FeatureOrigin {

    static final String FEATURE_SEGMENT_TYPE = "feature";
    private static final String SCENARIO_SEGMENT_TYPE = "scenario";
    private static final String OUTLINE_SEGMENT_TYPE = "outline";
    private static final String EXAMPLES_SEGMENT_TYPE = "examples";
    private static final String EXAMPLE_SEGMENT_TYPE = "example";

    private static FilePosition getPickleLocation(CucumberPickle location) {
        return FilePosition.from(location.getLine(), location.getColumn());
    }

    private static FilePosition createFilePosition(Location location) {
        return FilePosition.from(location.getLine(), location.getColumn());
    }

    static FeatureOrigin fromUri(URI uri) {
        if (ClasspathResourceSource.CLASSPATH_SCHEME.equals(uri.getScheme())) {
            if(!uri.getSchemeSpecificPart().startsWith("/")){
                // ClasspathResourceSource.from expects all resources to start with /
                uri = URI.create(CLASSPATH_SCHEME_PREFIX + "/" + uri.getSchemeSpecificPart());
            }
            ClasspathResourceSource source = ClasspathResourceSource.from(uri);
            return new ClasspathFeatureOrigin(source);
        }

        UriSource source = UriSource.from(uri);
        if (source instanceof FileSource) {
            return new FileFeatureOrigin((FileSource) source);
        }

        return new UriFeatureOrigin(source);

    }

    static boolean isClassPath(URI uri) {
        return ClasspathResourceSource.CLASSPATH_SCHEME.equals(uri.getScheme());
    }

    static boolean isFeatureSegment(UniqueId.Segment segment) {
        return FEATURE_SEGMENT_TYPE.equals(segment.getType());
    }

    abstract TestSource featureSource();

    abstract TestSource nodeSource(Node scenarioDefinition);

    abstract UniqueId featureSegment(UniqueId parent, CucumberFeature feature);

    UniqueId scenarioSegment(UniqueId parent, Node scenarioDefinition) {
        return parent.append(SCENARIO_SEGMENT_TYPE, String.valueOf(scenarioDefinition.getLocation().getLine()));
    }

    UniqueId outlineSegment(UniqueId parent, ScenarioOutline scenarioOutline) {
        return parent.append(OUTLINE_SEGMENT_TYPE, String.valueOf(scenarioOutline.getLocation().getLine()));
    }

    UniqueId examplesSegment(UniqueId parent, Examples examples) {
        return parent.append(EXAMPLES_SEGMENT_TYPE, String.valueOf(examples.getLocation().getLine()));
    }

    UniqueId exampleSegment(UniqueId parent, TableRow tableRow) {
        return parent.append(EXAMPLE_SEGMENT_TYPE, String.valueOf(tableRow.getLocation().getLine()));
    }

    private static class FileFeatureOrigin extends FeatureOrigin {

        private final FileSource source;

        FileFeatureOrigin(FileSource source) {
            this.source = source;
        }

        @Override
        TestSource featureSource() {
            return source;
        }

        @Override
        TestSource nodeSource(Node node) {
            return FileSource.from(source.getFile(), createFilePosition(node.getLocation()));
        }

        @Override
        UniqueId featureSegment(UniqueId parent, CucumberFeature feature) {
            return parent.append(FEATURE_SEGMENT_TYPE, source.getUri().toString());
        }
    }

    private static class UriFeatureOrigin extends FeatureOrigin {

        private final UriSource source;

        UriFeatureOrigin(UriSource source) {
            this.source = source;
        }

        @Override
        TestSource featureSource() {
            return source;
        }

        @Override
        TestSource nodeSource(Node node) {
            return source;
        }

        @Override
        UniqueId featureSegment(UniqueId parent, CucumberFeature feature) {
            return parent.append(FEATURE_SEGMENT_TYPE, source.getUri().toString());
        }
    }

    private static class ClasspathFeatureOrigin extends FeatureOrigin {

        private final ClasspathResourceSource source;

        ClasspathFeatureOrigin(ClasspathResourceSource source) {
            this.source = source;
        }

        @Override
        TestSource featureSource() {
            return source;
        }

        @Override
        TestSource nodeSource(Node node) {
            return ClasspathResourceSource.from(source.getClasspathResourceName(), createFilePosition(node.getLocation()));
        }

        @Override
        UniqueId featureSegment(UniqueId parent, CucumberFeature feature) {
            return parent.append(FEATURE_SEGMENT_TYPE, feature.getUri().toString());
        }
    }

}