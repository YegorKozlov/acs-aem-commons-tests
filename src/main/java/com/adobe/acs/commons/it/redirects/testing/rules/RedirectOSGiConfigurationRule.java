package com.adobe.acs.commons.it.redirects.testing.rules;

import com.google.common.collect.ImmutableMap;
import org.apache.sling.testing.clients.ClientException;
import org.apache.sling.testing.clients.osgi.OsgiConsoleClient;
import org.apache.sling.testing.clients.osgi.OsgiInstanceConfig;
import org.apache.sling.testing.clients.util.config.InstanceConfigException;
import org.apache.sling.testing.clients.util.config.impl.InstanceConfigCacheImpl;
import org.apache.sling.testing.junit.rules.instance.Instance;
import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.TimeoutException;

import static org.apache.http.HttpStatus.SC_OK;
import static org.junit.Assert.assertEquals;

public class RedirectOSGiConfigurationRule extends ExternalResource {
    private final Logger logger = LoggerFactory.getLogger(RedirectOSGiConfigurationRule.class);

    private final Instance quickstartRule;
    private OsgiConsoleClient client;
    private InstanceConfigCacheImpl configs;
    private final String pid = "com.adobe.acs.commons.redirects.filter.RedirectFilter";

    public RedirectOSGiConfigurationRule(Instance quickstartRule) {
        super();
        this.quickstartRule = quickstartRule;
    }

    @Override
    protected void before() throws ClientException, InstanceConfigException, InterruptedException {
        // Store existing config
        client = this.quickstartRule.getAdminClient(OsgiConsoleClient.class);

        this.configs = new InstanceConfigCacheImpl();
        this.configs.add(new OsgiInstanceConfig(this.client, this.pid));

        update(ImmutableMap.of(
                "enabled", "true",
                "mapUrls", "true",
                "preserveQueryString", "true",
                "evaluateSelectors", "false",
                "paths", "/content"
        ));
    }

    /**
     * Restore the original configuration state of the log tracer.
     */
    @Override
    protected void after() {
        try {
            this.configs.restore();
        } catch (InstanceConfigException | InterruptedException e) {
            logger.error("Could not restore OSGi config.", e);
        }
    }
    public void update(Map<String, Object> configProperties) throws ClientException, InterruptedException{
        try {
            client.waitEditConfiguration(10000,
                    pid, null,
                    configProperties);
            ;
            Map<String, Object> config = this.client.waitGetConfiguration(30000, this.pid, SC_OK);
            assertEquals(config.get("enabled"), "true");
        } catch (TimeoutException e) {
            throw new ClientException("Failed editing configuration for " + this.pid, e);
        }

    }

    public void evaluateSelectors(boolean flag) throws ClientException, InterruptedException{
        update(ImmutableMap.of(
                "enabled", "true",
                "mapUrls", "true",
                "preserveQueryString", "true",
                "evaluateSelectors", flag,
                "paths", "/content"
        ));
    }
}
