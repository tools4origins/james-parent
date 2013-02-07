package org.apache.james.karaf.karaf;

import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeaturesService;
import static org.apache.karaf.tooling.exam.options.KarafDistributionOption.karafDistributionConfiguration;
import static org.apache.karaf.tooling.exam.options.KarafDistributionOption.logLevel;
import org.apache.karaf.tooling.exam.options.LogLevelOption;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import org.ops4j.pax.exam.MavenUtils;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.options.MavenArtifactUrlReference;
import org.ops4j.pax.exam.spi.reactors.AllConfinedStagedReactorFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.File;
import java.net.URI;

/**
 * Apache James Karaf deployment test.
 */
@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
public class KarafFeatureInstallTest {

  private static final Logger LOG = LoggerFactory.getLogger(KarafFeatureInstallTest.class);
  @Inject
  private FeaturesService features;

  @Inject
  BundleContext bundleContext;

  private String featuresVersion;

  @Configuration
  public static Option[] configuration() throws Exception {
    MavenArtifactUrlReference karafUrl = maven().groupId("org.apache.karaf")
        .artifactId("apache-karaf")
        .version("2.3.0")
        .type("tar.gz");

    String jamesFeaturesVersion = MavenUtils.getArtifactVersion("org.apache.james.karaf", "james-karaf-features");

    return new Option[]{
        karafDistributionConfiguration().frameworkUrl(karafUrl).karafVersion("2.3.0").name("Apache Karaf")
            .unpackDirectory(new File("target/exam")),
        logLevel(LogLevelOption.LogLevel.INFO),
        // use system property to provide project version for tests
        systemProperty("james-karaf-features").value(jamesFeaturesVersion)
    };
  }

  @Before
  public void setUp() {
    featuresVersion = System.getProperty("james-karaf-features");
  }

  @Test
  public void shouldInstallAllFeatures() throws Exception {
    String url = maven("org.apache.james.karaf", "james-karaf-features")
        .version(featuresVersion)
        .classifier("features")
        .type("xml")
        .getURL();

    features.addRepository(new URI(url));
    features.installFeature("apache-james-mime4j");
    features.installFeature("commons-configuration");

    assertInstalled("apache-james-mime4j");
    assertInstalled("commons-configuration");

    for (Bundle bundle : bundleContext.getBundles()) {
      LOG.info("***** bundle {} is {}", bundle.getSymbolicName(), bundle.getState());
      assertEquals("Bundle " + bundle.getSymbolicName() + " is not active",
          Bundle.ACTIVE, bundle.getState());
    }
  }

  private void assertInstalled(String featureName) throws Exception {
    Feature feature = features.getFeature(featureName);
    assertTrue("Feature " + featureName + " should be installed", features.isInstalled(feature));
  }
}
