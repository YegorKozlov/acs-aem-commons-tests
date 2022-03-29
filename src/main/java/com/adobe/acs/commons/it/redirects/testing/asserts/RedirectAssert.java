package com.adobe.acs.commons.it.redirects.testing.asserts;

import org.apache.http.Header;
import org.apache.http.message.BasicHeader;
import org.apache.sling.testing.clients.SlingClient;
import org.apache.sling.testing.clients.SlingHttpResponse;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public final class RedirectAssert {
    private RedirectAssert() {
    }

    public static void assertRedirect(SlingClient slingClient, String source, String target, int statusCode) throws Exception {
        SlingHttpResponse response = navigate(slingClient, source);
        assertEquals("Unexpected response code: " + source, statusCode, response.getStatusLine().getStatusCode());
        Header locationHeader = response.getFirstHeader("Location");
        assertNotNull("location header not found", locationHeader);
        assertEquals(source + " should redirect to " + target, target, locationHeader.getValue());
    }

    public static void assertNoRedirect(SlingClient slingClient, String source) throws Exception {
        SlingHttpResponse response = navigate(slingClient, source);
        Header locationHeader = response.getFirstHeader("Location");
        assertNull("Location header is not expected", locationHeader);
    }

    private static SlingHttpResponse navigate(SlingClient slingClient, String source) throws Exception {
        return slingClient.doRawRequest("HEAD", source,
                Collections.singletonList(new BasicHeader("Cookie", "wcmmode=disabled")));
    }
}
