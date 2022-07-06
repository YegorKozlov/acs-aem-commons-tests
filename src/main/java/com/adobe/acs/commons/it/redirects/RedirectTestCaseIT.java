package com.adobe.acs.commons.it.redirects;

import com.adobe.acs.commons.it.redirects.testing.RedirectTestingClient;
import com.adobe.acs.commons.it.redirects.testing.rules.RedirectConfigurationRule;
import com.adobe.acs.commons.it.redirects.testing.rules.RedirectOSGiConfigurationRule;
import com.adobe.cq.testing.junit.rules.CQAuthorClassRule;
import com.google.common.collect.ImmutableMap;
import org.apache.http.Header;
import org.apache.http.message.BasicHeader;
import org.apache.sling.testing.clients.SlingHttpResponse;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.TimeZone;

import static com.adobe.acs.commons.it.redirects.testing.asserts.RedirectAssert.assertNoRedirect;
import static com.adobe.acs.commons.it.redirects.testing.asserts.RedirectAssert.assertRedirect;
import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

public class RedirectTestCaseIT {
    @ClassRule
    public static CQAuthorClassRule cqBaseClassRule = new CQAuthorClassRule();

    @Rule
    public RedirectOSGiConfigurationRule osgiConfigRule = new RedirectOSGiConfigurationRule(cqBaseClassRule.authorRule);

    @Rule
    public RedirectConfigurationRule redirectConfigurationRule = new RedirectConfigurationRule(cqBaseClassRule.authorRule);

    private String contentRoot;
    protected String confRoot;
    protected String damRoot;

    static RedirectTestingClient adminAuthor;

    @BeforeClass
    public static void beforeClass() {
        adminAuthor = cqBaseClassRule.authorRule.getAdminClient(RedirectTestingClient.class);
    }

    @Before
    public void before() {
        contentRoot = redirectConfigurationRule.getContentRoot();
        confRoot = redirectConfigurationRule.getConfRoot();
        damRoot = redirectConfigurationRule.getDamRoot();
    }

    @Test
    public void testNavigate302() throws Exception {
        redirectConfigurationRule.withRules(confRoot,
                contentRoot + "/en/one", contentRoot + "/en/two", 302
        );

        assertRedirect(adminAuthor, contentRoot + "/en/one.html", contentRoot + "/en/two.html", 302);
    }

    @Test
    public void testNavigate301() throws Exception {
        redirectConfigurationRule.withRules(confRoot,
                contentRoot + "/page1", contentRoot + "/page1target", 301
        );

        assertRedirect(adminAuthor, contentRoot + "/page1.html", contentRoot + "/page1target.html", 301);
    }

    @Test
    public void testNavigateToExternalSite() throws Exception {
        redirectConfigurationRule.withRules(confRoot,
                contentRoot + "/en/one", "https://www.geometrixx.com", 302
        );

        assertRedirect(adminAuthor, contentRoot + "/en/one.html", "https://www.geometrixx.com", 302);
    }

    @Test
    public void testPreserveQueryString() throws Exception {
        redirectConfigurationRule.withRules(confRoot,
                contentRoot + "/en/one", contentRoot + "/en/two", 302,
                contentRoot + "/en/three", "https://www.geometrixx.com", 302
        );

        assertRedirect(adminAuthor, contentRoot + "/en/one.html?a=1&b=2&c=3", contentRoot + "/en/two.html?a=1&b=2&c=3", 302);
        assertRedirect(adminAuthor, contentRoot + "/en/three.html?a=1&b=2&c=3", "https://www.geometrixx.com?a=1&b=2&c=3", 302);
    }

    @Test
    public void testMatchSingleAsset() throws Exception {
        redirectConfigurationRule.withRules(confRoot,
                damRoot + "/en/one.pdf", damRoot + "/en/two.pdf", 302
        );

        assertRedirect(adminAuthor, damRoot + "/en/one.pdf", damRoot + "/en/two.pdf", 302);
    }

    // users can append optional .html extension in the rules
    @Test
    public void testMatchWithHtmlExtension() throws Exception {
        redirectConfigurationRule.withRules(confRoot,
                contentRoot + "/en/one.html", contentRoot + "/en/two.html", 302
        );

        assertRedirect(adminAuthor, contentRoot + "/en/one.html", contentRoot + "/en/two.html", 302);
    }

    @Test
    public void testMatchRegexAsset() throws Exception {
        redirectConfigurationRule.withRules(confRoot,
                damRoot + "/en/events/(.*?).pdf", damRoot + "/en/welcome.pdf", 302
        );
        assertRedirect(adminAuthor, damRoot + "/en/events/one.pdf", damRoot + "/en/welcome.pdf", 302);
    }

    @Test
    public void testNotMatchRegexAsset() throws Exception {
        redirectConfigurationRule.withRules(confRoot,
                damRoot + "/en/events/(.*?).pdf", damRoot + "/en/welcome.pdf", 302
        );
        assertNoRedirect(adminAuthor, contentRoot + "/en/events/one.txt");
    }

    @Test
    public void testLeadingSpaces() throws Exception {
        redirectConfigurationRule.withRules(confRoot,
                " " + contentRoot + "/en/one", " " + contentRoot + "/en/two", 302
        );

        assertRedirect(adminAuthor, contentRoot + "/en/one.html", contentRoot + "/en/two.html", 302);
    }

    @Test
    public void testTrailingSpaces() throws Exception {
        redirectConfigurationRule.withRules(confRoot,
                contentRoot + "/en/one  ", contentRoot + "/en/two  ", 302
        );

        assertRedirect(adminAuthor, contentRoot + "/en/one.html", contentRoot + "/en/two.html", 302);
    }

    @Test
    public void testUnsupportedExtension() throws Exception {
        redirectConfigurationRule.withRules(confRoot,
                contentRoot + "/en/one", contentRoot + "/en/two", 302
        );

        assertNoRedirect(adminAuthor, contentRoot + "/en/one.json");
    }

    @Test
    public void testUnsupportedContentRoot() throws Exception {
        redirectConfigurationRule.withRules(confRoot,
                "/etc/en/one", "/lib/en/two", 302
        );

        assertNoRedirect(adminAuthor, "/etc/en/one.html");
    }

    @Test
    public void testUnsupportedMethod() throws Exception {
        redirectConfigurationRule.withRules(confRoot,
                contentRoot + "/en/one", contentRoot + "/en/two", 302
        );

        SlingHttpResponse response = adminAuthor.doRawRequest("POST", contentRoot + "/en/one",
                Collections.singletonList(new BasicHeader("Cookie", "wcmmode=disabled")), 404, 409, 500);
        Header locationHeader = response.getFirstHeader("Location");
        assertNull("location header not found", locationHeader);

    }

    @Test
    public void testAuthorEditWCMMode() throws Exception {
        redirectConfigurationRule.withRules(confRoot,
                contentRoot + "/en/one", contentRoot + "/en/two", 302
        );

        adminAuthor.doGet(contentRoot + "/en/one", null,
                Collections.singletonList(new BasicHeader("Cookie", "wcmmode=edit")), 404);
    }

    @Test
    public void testNoopRewrite() throws Exception {
        redirectConfigurationRule.withRules(confRoot, "(.*)", "$1", 302);
        assertRedirect(adminAuthor, contentRoot + "/about/contact-us.html", contentRoot + "/about/contact-us.html", 302);
    }

    @Test
    public void testPathRewrite1() throws Exception {
        redirectConfigurationRule.withRules(confRoot, contentRoot + "/(.+)/contact-us", contentRoot + "/about/about-us.html", 302);
        assertRedirect(adminAuthor, contentRoot + "/en/contact-us.html", contentRoot + "/about/about-us.html", 302);
    }

    @Test
    public void testPathRewrite2() throws Exception {
        redirectConfigurationRule.withRules(confRoot, contentRoot + "/(.+)/contact-us", contentRoot + "/us/$1/contact-us", 302);
        assertRedirect(adminAuthor, contentRoot + "/1/contact-us.html", contentRoot + "/us/1/contact-us.html", 302);
        assertRedirect(adminAuthor, contentRoot + "/1/2/contact-us.html", contentRoot + "/us/1/2/contact-us.html", 302);
    }

    @Test
    public void testPathRewrite3() throws Exception {
        redirectConfigurationRule.withRules(confRoot, contentRoot + "/(en)/(.*?/?)contact-us", contentRoot + "/us/$2contact-us", 302);

        assertRedirect(adminAuthor, contentRoot + "/en/contact-us.html", contentRoot + "/us/contact-us.html", 302);
        assertRedirect(adminAuthor, contentRoot + "/en/1/contact-us.html", contentRoot + "/us/1/contact-us.html", 302);
        assertRedirect(adminAuthor, contentRoot + "/en/1/2/contact-us.html", contentRoot + "/us/1/2/contact-us.html", 302);

    }

    @Test
    public void testPathRewrite4() throws Exception {
        redirectConfigurationRule.withRules(confRoot, contentRoot + "/(en)/(.+)/contact-us", contentRoot + "/us/$2/contact-us#section", 302);
        assertRedirect(adminAuthor, contentRoot + "/en/1/contact-us", contentRoot + "/us/1/contact-us#section", 302);
    }

    @Test
    public void testPathRewrite5() throws Exception {
        redirectConfigurationRule.withRules(confRoot, contentRoot + "/en/research/(.*)", contentRoot + "/en/search?keywords=talent-management", 302);
        assertRedirect(adminAuthor, contentRoot + "/en/research/doc", contentRoot + "/en/search?keywords=talent-management", 302);
    }

//    @Test
//    public void testPathRewrite6() throws Exception {
//        redirectConfigurationRule.withRules(confRoot, contentRoot + "/(.+)/contact-us", contentRoot + "/$1/contact-us#updated", 302);
//        assertRedirect(adminAuthor, contentRoot + "/en/contact-us", contentRoot + "/en/contact-us#updated", 302);
//    }

    @Test
    public void testInvalidRules() throws Exception {
        redirectConfigurationRule.withRules(confRoot,
                contentRoot + "/(.+", contentRoot + "/$a", 302,
                contentRoot + "/(.+", contentRoot + "/$", 302);
        assertNoRedirect(adminAuthor, contentRoot + "/en/research/doc");
    }

    @Test
    public void testUntilDateRedirectExpired() throws Exception {
        ZonedDateTime dateInPast = ZonedDateTime.now().minusDays(1);
        redirectConfigurationRule.createRule(confRoot, ImmutableMap.of(
                "source", contentRoot + "/en/contact-us",
                "target", contentRoot + "/en/contact-them",
                "statusCode", String.valueOf(302),
                "untilDate", DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(dateInPast)
        ));

        assertNoRedirect(adminAuthor, contentRoot + "/en/research/doc");
    }

    @Test
    public void testUntilDateInFuture() throws Exception {
        ZonedDateTime dateInFuture = ZonedDateTime.now().plusDays(1);
        redirectConfigurationRule.createRule(confRoot, ImmutableMap.of(
                "source", contentRoot + "/en/contact-us",
                "target", contentRoot + "/en/contact-them",
                "statusCode", String.valueOf(302),
                "untilDate", DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(dateInFuture)
        ));

        assertRedirect(adminAuthor, contentRoot + "/en/contact-us", contentRoot + "/en/contact-them", 302);
    }

    @Test
    public void testCaconfigRedirects() throws Exception {

        redirectConfigurationRule.withRules(confRoot,
                contentRoot + "/page1", contentRoot + "/page1target", 302,
                contentRoot + "/page2", contentRoot + "/page2target", 302,
                contentRoot + "/page3", contentRoot + "/page3target", 301,
                damRoot + "/asset1.txt", damRoot + "/asset1target.txt", 302
        );
        redirectConfigurationRule.withRules("/conf/global",
                contentRoot + "/page1", contentRoot + "/page1global", 302,
                contentRoot + "/page2", contentRoot + "/page2global", 302,
                contentRoot + "/page3", contentRoot + "/page3global", 301,
                damRoot + "/asset1.txt", damRoot + "/asset1global.txt", 302
        );

        assertRedirect(adminAuthor, contentRoot + "/page1.html", contentRoot + "/page1target.html", 302);
        assertRedirect(adminAuthor, contentRoot + "/page2.html", contentRoot + "/page2target.html", 302);
        assertRedirect(adminAuthor, contentRoot + "/page3.html", contentRoot + "/page3target.html", 301);
        assertRedirect(adminAuthor, damRoot + "/asset1.txt", damRoot + "/asset1target.txt", 302);

        // remove the cq:conf property. the redirect filter should start using the /conf/global configuration
        redirectConfigurationRule.linkConf(contentRoot, "");
        redirectConfigurationRule.linkConf(damRoot, "");
        assertRedirect(adminAuthor, contentRoot + "/page1.html", contentRoot + "/page1global.html", 302);
        assertRedirect(adminAuthor, contentRoot + "/page2.html", contentRoot + "/page2global.html", 302);
        assertRedirect(adminAuthor, contentRoot + "/page3.html", contentRoot + "/page3global.html", 301);
        assertRedirect(adminAuthor, damRoot + "/asset1.txt", damRoot + "/asset1global.txt", 302);
    }


    @Test
    public void testMatchSelectors() throws Exception {
        osgiConfigRule.evaluateSelectors(true);

        redirectConfigurationRule.withRules(confRoot,
                contentRoot + "/page1.mobile", contentRoot + "/page1target", 302
        );
        assertRedirect(adminAuthor, contentRoot + "/page1.mobile.html", contentRoot + "/page1target.html", 302);
        assertNoRedirect(adminAuthor, contentRoot + "/page1.html");
        assertNoRedirect(adminAuthor, contentRoot + "/page1.desktop.html");

    }

    @Test
    public void testMatchSelectorsRegex() throws Exception {
        osgiConfigRule.evaluateSelectors(true);

        redirectConfigurationRule.withRules(confRoot,
                contentRoot + "/page1\\.(mobile|desktop)", contentRoot + "/page1target", 302
        );
        assertRedirect(adminAuthor, contentRoot + "/page1.mobile.html", contentRoot + "/page1target.html", 302);
        assertRedirect(adminAuthor, contentRoot + "/page1.desktop.html", contentRoot + "/page1target.html", 302);
        assertNoRedirect(adminAuthor, contentRoot + "/page1.unknowqn.html");
        assertNoRedirect(adminAuthor, contentRoot + "/page1.html");
    }


    @Test
    public void testSelectorsDisabled() throws Exception {
        osgiConfigRule.evaluateSelectors(false);

        redirectConfigurationRule.withRules(confRoot,
                contentRoot + "/page1", contentRoot + "/page1target", 302,
                contentRoot + "/page1.desktop", contentRoot + "/page1target", 302,
                contentRoot + "/page1.mobile", contentRoot + "/page1target", 302
                );
        assertRedirect(adminAuthor, contentRoot + "/page1.mobile.html", contentRoot + "/page1target.html", 302);
        assertRedirect(adminAuthor, contentRoot + "/page1.html", contentRoot + "/page1target.html", 302);
        assertRedirect(adminAuthor, contentRoot + "/page1.desktop.html", contentRoot + "/page1target.html", 302);

    }
}
