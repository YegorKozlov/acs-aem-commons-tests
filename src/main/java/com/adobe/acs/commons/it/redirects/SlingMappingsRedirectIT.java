package com.adobe.acs.commons.it.redirects;

import com.adobe.acs.commons.it.redirects.testing.RedirectTestingClient;
import com.adobe.acs.commons.it.redirects.testing.rules.RedirectConfigurationRule;
import com.adobe.acs.commons.it.redirects.testing.rules.RedirectOSGiConfigurationRule;
import com.adobe.cq.testing.junit.rules.CQAuthorClassRule;
import org.apache.sling.testing.clients.ClientException;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.Arrays;

import static com.adobe.acs.commons.it.redirects.testing.asserts.RedirectAssert.assertNoRedirect;
import static com.adobe.acs.commons.it.redirects.testing.asserts.RedirectAssert.assertRedirect;


public class SlingMappingsRedirectIT {
    private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @ClassRule
    public static CQAuthorClassRule cqBaseClassRule = new CQAuthorClassRule();

    @Rule
    public RedirectOSGiConfigurationRule osgiConfigRule = new RedirectOSGiConfigurationRule(cqBaseClassRule.authorRule);

    @Rule
    public RedirectConfigurationRule redirectConfigurationRule = new RedirectConfigurationRule(cqBaseClassRule.authorRule);

    private String contentRoot;
    protected String confRoot;
    protected String damRoot;
    protected String slingMappings;
    protected String site = "https://www.acs-redirect-manager.github.io";

    static RedirectTestingClient adminAuthor;

    @BeforeClass
    public static void beforeClass() {
        adminAuthor = cqBaseClassRule.authorRule.getAdminClient(RedirectTestingClient.class);
    }

    @Before
    public void before() throws ClientException {
        contentRoot = redirectConfigurationRule.getContentRoot();
        confRoot = redirectConfigurationRule.getConfRoot();
        damRoot = redirectConfigurationRule.getDamRoot();

        slingMappings = "/etc/map/" + site.replace("://", "/");
        log.info("created {}", slingMappings);
        createSlingMappings(slingMappings, contentRoot, damRoot);
    }

    @After
    public void after() throws ClientException {
        log.info("deleting {}", slingMappings);
        adminAuthor.deletePath(slingMappings);
    }

    /**
     *
     * @param path
     * @param internalRedirect
     * @throws ClientException
     */
    void createSlingMappings(String path, String... internalRedirect) throws ClientException {
        if (!path.startsWith("/etc/map")) {
            throw new IllegalArgumentException("slingMappings root must start with /etc/map, was: " + path);
        }
        adminAuthor.createNode("/etc/map/https", "sling:Folder");
        adminAuthor.createNode(path, "sling:Mapping");
        adminAuthor.setPropertyStringArray(path, "sling:internalRedirect", Arrays.asList(internalRedirect));
        log.info("created {}", path);
    }


    @Test
    public void testSlingMappings() throws Exception {

        redirectConfigurationRule.withRules(confRoot,
                contentRoot + "/page1", contentRoot + "/page1target", 302,
                "/page2", contentRoot + "/page2target", 302,
                "/page3", "/page3target", 301,
                damRoot + "/asset1.txt", damRoot + "/asset1target.txt", 302
        );
        redirectConfigurationRule.withRules("/conf/global",
                contentRoot + "/page1", contentRoot + "/page1global", 302,
                "/page2", contentRoot + "/page2global", 302,
                "/page3", "/page3global", 301,
                damRoot + "/asset1.txt", damRoot + "/asset1global.txt", 302,
                "/asset2.txt", "https://www.my-site.com/assets/asset2global.txt", 302
        );

        assertRedirect(adminAuthor, contentRoot + "/page1.html", site + "/page1target.html", 302);
        assertRedirect(adminAuthor, contentRoot + "/page2.html", site + "/page2target.html", 302);
        assertRedirect(adminAuthor, contentRoot + "/page3.html", "/page3target.html", 301);
        assertRedirect(adminAuthor, damRoot + "/asset1.txt", site + "/asset1target.txt", 302);
        // redirect on /asset2.txt is set in the global configuration. Not visible from the current context
        assertNoRedirect(adminAuthor, damRoot + "/asset2.txt");

        // with query string
        assertRedirect(adminAuthor, contentRoot + "/page1.html?a=1&b=2", site + "/page1target.html?a=1&b=2", 302);
        assertRedirect(adminAuthor, damRoot + "/asset1.txt?a=1&b=2", site + "/asset1target.txt?a=1&b=2", 302);

        // remove the cq:conf property. the redirect should start using the /conf/global configuration
        redirectConfigurationRule.linkConf(damRoot, "");
        redirectConfigurationRule.linkConf(contentRoot, "");
        assertRedirect(adminAuthor, contentRoot + "/page1.html", site + "/page1global.html", 302);
        assertRedirect(adminAuthor, contentRoot + "/page2.html", site + "/page2global.html", 302);
        assertRedirect(adminAuthor, contentRoot + "/page3.html", "/page3global.html", 301);

        assertRedirect(adminAuthor, damRoot + "/asset1.txt", site + "/asset1global.txt", 302);
        assertRedirect(adminAuthor, damRoot + "/asset2.txt", "https://www.my-site.com/assets/asset2global.txt", 302);

    }
}
