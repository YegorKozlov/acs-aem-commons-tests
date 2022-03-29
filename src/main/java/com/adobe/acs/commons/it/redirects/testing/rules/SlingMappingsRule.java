package com.adobe.acs.commons.it.redirects.testing.rules;

import com.google.common.collect.ImmutableMap;
import org.apache.sling.testing.clients.ClientException;
import org.apache.sling.testing.clients.SlingClient;
import org.apache.sling.testing.clients.osgi.OsgiConsoleClient;
import org.apache.sling.testing.clients.osgi.OsgiInstanceConfig;
import org.apache.sling.testing.clients.util.config.InstanceConfigException;
import org.apache.sling.testing.clients.util.config.impl.InstanceConfigCacheImpl;
import org.apache.sling.testing.junit.rules.instance.Instance;
import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import static org.apache.http.HttpStatus.SC_OK;
import static org.junit.Assert.assertEquals;

public class SlingMappingsRule extends ExternalResource {
    private final Logger logger = LoggerFactory.getLogger(SlingMappingsRule.class);

    private final Instance quickstartRule;
    private SlingClient client;
    private String slingMappings;
    private String domain;
    private List<String> internalRedirects;

    public SlingMappingsRule(Instance quickstartRule, String domain, List<String> internalRedirects) {
        super();
        this.quickstartRule = quickstartRule;
        this.internalRedirects = internalRedirects;
        this.domain = domain;
    }

    void createSlingMappings(String path, List<String> internalRedirects) throws ClientException {
        if (!path.startsWith("/etc/map")) {
            throw new IllegalArgumentException("slingMappings root must start with /etc/map, was: " + path);
        }
        client.createNode("/etc/map/https", "sling:Folder");
        client.createNode(path, "sling:Mapping");
        client.setPropertyStringArray(path, "sling:internalRedirect", internalRedirects);
        logger.info("created {}", path);
    }

    @Override
    protected void before() throws ClientException {
        // Store existing config
        client = this.quickstartRule.getAdminClient(OsgiConsoleClient.class);

        slingMappings = "/etc/map/" + domain.replace("://", "/");
        logger.info("created {}", slingMappings);
        createSlingMappings(slingMappings, internalRedirects);

    }

    @Override
    protected void after() {
        try {
            logger.info("deleting {}", slingMappings);
            client.deletePath(slingMappings);
        } catch (ClientException e) {
            logger.error("Could not delete sling mappings", e);
        }
    }
}
