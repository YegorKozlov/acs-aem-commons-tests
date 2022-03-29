package com.adobe.acs.commons.it.redirects;

import com.adobe.acs.commons.it.redirects.testing.RedirectTestingClient;
import com.adobe.acs.commons.it.redirects.testing.rules.RedirectConfigurationRule;
import com.adobe.acs.commons.it.redirects.testing.rules.RedirectOSGiConfigurationRule;
import com.adobe.cq.testing.junit.rules.CQAuthorClassRule;
import com.google.common.collect.ImmutableMap;
import org.apache.sling.testing.clients.ClientException;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import static com.adobe.acs.commons.it.redirects.testing.asserts.RedirectAssert.assertRedirect;


public class ContextPrefixRedirectIT {

    @ClassRule
    public static CQAuthorClassRule cqBaseClassRule = new CQAuthorClassRule();

    @Rule
    public RedirectOSGiConfigurationRule osgiConfigRule = new RedirectOSGiConfigurationRule(cqBaseClassRule.authorRule);

    @Rule
    public RedirectConfigurationRule redirectConfigurationRule = new RedirectConfigurationRule(cqBaseClassRule.authorRule);

    private String contentRoot;
    protected String confRoot;

    static RedirectTestingClient adminAuthor;

    @BeforeClass
    public static void beforeClass() {
        adminAuthor = cqBaseClassRule.authorRule.getAdminClient(RedirectTestingClient.class);
    }
    
    @Before
    public void before() throws ClientException {
        contentRoot = redirectConfigurationRule.getContentRoot(); // /content/<random-string>
        confRoot = redirectConfigurationRule.getConfRoot(); // /conf/<random-string>

        // /conf/<random-string>/settings/redirects@contextPrefix=contentRoot
        adminAuthor.setPropertyString(confRoot + "/settings/redirects",
                "contextPrefix", contentRoot, 200);
    }

    @Test
    public void testContextPrefix() throws Exception {
        redirectConfigurationRule.withRules(confRoot,
                "/en/one", "/en/two", 302
        );

        assertRedirect(adminAuthor, contentRoot + "/en/one.html", contentRoot + "/en/two.html", 302);
    }

    @Test
    public void testContextPrefixFullPathRedirectRule() throws Exception {
        redirectConfigurationRule.withRules(confRoot,
                contentRoot + "/en/one", contentRoot + "/en/two", 302
        );

        assertRedirect(adminAuthor, contentRoot + "/en/one.html", contentRoot + "/en/two.html", 302);
    }

    @Test
    public void testContextPrefixMixedRedirectRules() throws Exception {
        redirectConfigurationRule.withRules(confRoot,
                contentRoot + "/en/one", contentRoot + "/en/two", 302,
                "/en/three", "/en/four", 302
        );

        assertRedirect(adminAuthor, contentRoot + "/en/one.html", contentRoot + "/en/two.html", 302);
        assertRedirect(adminAuthor, contentRoot + "/en/three.html", contentRoot + "/en/four.html", 302);
    }

    @Test
    public void testContextPrefixMixedRedirects() throws Exception {
        redirectConfigurationRule.withRules(confRoot,
                contentRoot + "/en/one", contentRoot + "/en/two", 302,
                "/en/three", contentRoot + "/en/four", 302
        );

        assertRedirect(adminAuthor, contentRoot + "/en/one.html", contentRoot + "/en/two.html", 302);
        assertRedirect(adminAuthor, contentRoot + "/en/three.html", contentRoot + "/en/four.html", 302);
    }

    @Test
    public void testContextPrefixWithAbsoluteUrl() throws Exception {

        redirectConfigurationRule.withRules(confRoot,
                "/page1", "//page2target.com/page1", 302,
                "/page2", "http://localhost:4502/page3", 302,
                "/en/one", "https://adobe-consulting-services.github.io/acs-aem-commons/", 302
        );

        assertRedirect(adminAuthor, contentRoot + "/page1.html", "//page2target.com/page1", 302);
        assertRedirect(adminAuthor, contentRoot + "/page2.html", "http://localhost:4502/page3", 302);
        assertRedirect(adminAuthor, contentRoot + "/en/one.html", "https://adobe-consulting-services.github.io/acs-aem-commons/", 302);
    }

    @Test
    public void testIgnorePrefixedRedirects() throws Exception {
        redirectConfigurationRule.createRule(
                redirectConfigurationRule.getConfRoot(),
                ImmutableMap.of(
                        "source", "/en/one",
                        "target", "/content/escapedsite/en/one",
                        "statusCode", "302",
                        "contextPrefixIgnored", "true"));

        assertRedirect(adminAuthor, contentRoot + "/en/one.html", "/content/escapedsite/en/one.html", 302);
    }

    @Test
    public void testContextPrefixWithPatternRule() throws Exception {

        redirectConfigurationRule.createRule(
                redirectConfigurationRule.getConfRoot(),
                ImmutableMap.of(
                        "source", "/en/one(.*)",
                        "target", "/en/two",
                        "statusCode", "302"));
        redirectConfigurationRule.createRule(
                redirectConfigurationRule.getConfRoot(),
                ImmutableMap.of(
                        "source", "/en/three(.*)",
                        "target", "/content/escaped/en/four",
                        "statusCode", "302",
                        "contextPrefixIgnored", "true"));
        redirectConfigurationRule.createRule(
                redirectConfigurationRule.getConfRoot(),
                ImmutableMap.of(
                        "source", "/(.*)",
                        "target", contentRoot + "/en/six",
                        "statusCode", "302"));

        assertRedirect(adminAuthor, contentRoot + "/en/one1.html", contentRoot + "/en/two.html", 302);
        assertRedirect(adminAuthor, contentRoot + "/en/three3.html", "/content/escaped/en/four.html", 302);
        assertRedirect(adminAuthor, contentRoot + "/en/five.html", contentRoot + "/en/six.html", 302);
    }
}
