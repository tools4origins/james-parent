package org.apache.james.karaf.karaf;

import com.google.common.base.Stopwatch;
import org.apache.james.dnsservice.api.DNSService;
import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.tooling.exam.options.KarafDistributionConfigurationFilePutOption;
import static org.apache.karaf.tooling.exam.options.KarafDistributionOption.karafDistributionConfiguration;
import static org.apache.karaf.tooling.exam.options.KarafDistributionOption.keepRuntimeFolder;
import static org.apache.karaf.tooling.exam.options.KarafDistributionOption.logLevel;
import org.apache.karaf.tooling.exam.options.LogLevelOption;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
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
import org.ops4j.pax.exam.options.MavenArtifactProvisionOption;
import org.ops4j.pax.exam.options.MavenArtifactUrlReference;
import org.ops4j.pax.exam.spi.reactors.AllConfinedStagedReactorFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.File;
import java.net.URI;
import java.util.concurrent.TimeUnit;

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
                new KarafDistributionConfigurationFilePutOption("etc/custom.properties",
                        "org.osgi.framework.system.packages.extra",
                        "sun.net.spi.nameservice"),
                keepRuntimeFolder(),
                new MavenArtifactProvisionOption().groupId("com.google.guava").artifactId("guava").versionAsInProject(),
                // use system property to provide project version for tests
                systemProperty("james-karaf-features").value(jamesFeaturesVersion)
        };
    }

    @Before
    public void setUp() {
        featuresVersion = System.getProperty("james-karaf-features");
    }

    @Test
    public void testInstallCommonsConfigurationFeature() throws Exception {
        addJamesFeaturesRepository();
        features.installFeature("commons-configuration");
        assertInstalled("commons-configuration");
        assertBundlesAreActive();
    }

    @Test
    public void testInstallApacheMime4jFeature() throws Exception {
        addJamesFeaturesRepository();
        features.installFeature("apache-james-mime4j");
        assertInstalled("apache-james-mime4j");
        assertBundlesAreActive();
    }

    @Test
    public void testInstallJamesDnsServiceDnsJava() throws Exception {
        addJamesFeaturesRepository();
        features.installFeature("james-server-dnsservice-dnsjava");
        assertInstalled("james-server-dnsservice-dnsjava");
        assertBundlesAreActive();
        assertOSGiServiceStartsIn(DNSService.class, 30000);
    }

    private void assertInstalled(String featureName) throws Exception {
        Feature feature = features.getFeature(featureName);
        assertTrue("Feature " + featureName + " should be installed", features.isInstalled(feature));
    }

    private void assertBundlesAreActive() {
        for (Bundle bundle : bundleContext.getBundles()) {
            LOG.info("***** bundle {} is {}", bundle.getSymbolicName(), bundle.getState());
            assertEquals("Bundle " + bundle.getSymbolicName() + " is not active",
                    Bundle.ACTIVE, bundle.getState());
        }
    }

    private void addJamesFeaturesRepository() throws Exception {
        String url = maven("org.apache.james.karaf", "james-karaf-features")
                .version(featuresVersion)
                .classifier("features")
                .type("xml")
                .getURL();

        features.addRepository(new URI(url));
        features.installFeature("spring-dm");
        features.installFeature("war");
    }

    private void assertOSGiServiceStartsIn(Class clazz, int timeoutInMilliseconds) throws InterruptedException {
        final ServiceTracker tracker = new ServiceTracker(bundleContext, clazz, null);
        tracker.open(true);
        try {
            final Stopwatch stopwatch = new Stopwatch().start();
            final int expectedCount = 1;

            while (true) {
                Object[] services = tracker.getServices();
                if (services == null || services.length < expectedCount) {
                    final int actualCount = (services == null) ? 0 : services.length;
                    if (stopwatch.elapsedMillis() > timeoutInMilliseconds) {
                        fail(String.format("Expected to find %d services of type %s. Found only %d in %d milliseconds",
                                expectedCount, clazz.getCanonicalName(), actualCount, timeoutInMilliseconds));
                    }

                    LOG.info(String.format("Found %d services implementing %s. Trying again in 1s.",
                            actualCount, clazz.getCanonicalName()));
                    TimeUnit.SECONDS.sleep(1);

                } else if (services.length > expectedCount) {
                    fail(String.format("Expected to find %d services implementing %s. Found %d (more than expected).",
                            expectedCount, clazz.getCanonicalName(), services.length));

                } else if (services.length == expectedCount) {
                    break;  /* done - the test was successful */
                }
            }

        } finally {
            tracker.close();
        }
    }
}
