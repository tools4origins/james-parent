package org.apache.james.karaf.features;

import org.apache.james.mailetcontainer.api.MailProcessor;
import org.apache.james.mailetcontainer.api.jmx.MailSpoolerMBean;
import org.apache.mailet.MailetContext;
import org.junit.Test;

public class MailetContainerLiveTest extends KarafLiveTestSupport {

    private String mailetContainerFeature = "james-server-mailet-container-camel";

    @Test
    public void testInstallMailetContainerFeature() throws Exception {
        addJamesFeaturesRepository();
        features.installFeature(mailetContainerFeature);
        assertInstalled(mailetContainerFeature);
        assertBundlesAreActive();
        assertOSGiServiceStartsIn(MailetContext.class, WAIT_30_SECONDS);
        assertOSGiServiceStartsIn(MailProcessor.class, WAIT_30_SECONDS);
        assertOSGiServiceStartsIn(MailSpoolerMBean.class, WAIT_30_SECONDS);
    }
}
