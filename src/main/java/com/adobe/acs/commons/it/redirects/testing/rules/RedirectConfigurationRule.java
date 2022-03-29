package com.adobe.acs.commons.it.redirects.testing.rules;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.apache.sling.testing.clients.ClientException;
import org.apache.sling.testing.clients.SlingClient;
import org.apache.sling.testing.junit.rules.instance.Instance;
import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RedirectConfigurationRule extends ExternalResource {
    private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final Instance quickstartRule;
    protected String contentRoot;
    protected String confRoot;
    protected String damRoot;
    protected List<String> testRules = new ArrayList<>();
    protected SlingClient sling;

    public RedirectConfigurationRule(Instance quickstartRule) {
        super();
        this.quickstartRule = quickstartRule;
    }

    @Override
    protected void before() throws ClientException {
        sling = quickstartRule.getAdminClient();

        String site = RandomStringUtils.randomAlphabetic(8);
        contentRoot = "/content/" + site;
        confRoot = "/conf/" + site;
        damRoot = "/content/dam/" + site;
        createSite(contentRoot);
        createConf(confRoot);
        createDamRoot(damRoot);
        linkConf(contentRoot, confRoot);
        linkConf(damRoot, confRoot);

    }

    /**
     * Restore the original configuration state of the log tracer.
     */
    @Override
    protected void after() {
        try {
            for (String path : testRules) {
                log.info("deleting {}", path);
                sling.deletePath(path);
            }
            log.info("deleting {}", confRoot);
            sling.deletePath(confRoot);
            log.info("deleting {}", contentRoot);
            sling.deletePath(contentRoot);
            log.info("deleting {}", damRoot);
            sling.deletePath(damRoot);

        } catch (ClientException e) {
            log.error("failed to restore state", e);
        }
    }

    public void createConf(String path) throws ClientException {
        if (!path.startsWith("/conf/")) {
            throw new IllegalArgumentException("conf root must start with /conf, was: " + path);
        }
        if (sling.exists(path + "/settings/redirects")) {
            log.info("already exists: {}", path);
            return;
        }
        sling.createNode(path + "/settings", "sling:Folder");
        sling.createNode(path + "/settings/redirects", "nt:unstructured");
        sling.setPropertyString(path + "/settings/redirects",
                "sling:resourceType", "acs-commons/components/utilities/manage-redirects/redirects", 200);
        log.info("created {}", path + "/settings/redirects");
    }

    public void createSite(String path) throws ClientException {
        if (!path.startsWith("/content/")) {
            throw new IllegalArgumentException("content root must start with /content, was: " + path);
        }
        sling.createNode(path, "cq:Page");
        sling.createNode(path + "/jcr:content", "cq:PageContent");
        log.info("created {}", path);
    }

    public void createDamRoot(String path) throws ClientException {
        if (!path.startsWith("/content/dam/")) {
            throw new IllegalArgumentException("dam root must start with /content/dam, was: " + path);
        }
        sling.createNode(path, "sling:Folder");
        sling.createNode(path + "/jcr:content", "nt:unstructured");
        log.info("created {}", path);
    }

    public void linkConf(String root, String confPath) throws ClientException {
        sling.setPropertyString(root + "/jcr:content", "cq:conf", confPath, 200);
    }

    public void withRules(String confRoot, Object... args) throws ClientException {
        if (args.length % 3 != 0)
            throw new IllegalArgumentException("the number of arguments should be multiple of 3");

        for (int i = 0; i < args.length; i += 3) {
            String source = (String) args[i];
            String target = (String) args[i + 1];
            int statusCode = (int) args[i + 2];

            createRule(confRoot, ImmutableMap.of("source", source,
                    "target", target,
                    "statusCode", String.valueOf(statusCode)));
        }
    }

    public void createRule(String confRoot, Map<String, String> props) throws ClientException {
        String source = props.get("source");
        if (source == null)
            throw new IllegalArgumentException("source is required, was: " + props);

        String storagePath = confRoot + "/settings/redirects";
        String rulePath = storagePath + "/rule-it-" + source.hashCode();
        sling.createNode(rulePath, "nt:unstructured");
        List<NameValuePair> redirectProps = new ArrayList<>();
        redirectProps.add(
                new BasicNameValuePair("sling:resourceType", "acs-commons/components/utilities/manage-redirects/redirect-row")
        );
        for (String key : props.keySet()) {
            redirectProps.add(new BasicNameValuePair(key, props.get(key)));
        }
        sling.setPropertiesString(rulePath, redirectProps, 200);
        testRules.add(rulePath);

    }

    public String getDamRoot() {
        return damRoot;

    }

    public String getContentRoot() {
        return contentRoot;
    }

    public String getConfRoot() {
        return confRoot;
    }

}
