package com.adobe.acs.commons.it.redirects.testing;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.sling.testing.clients.ClientException;
import org.apache.sling.testing.clients.SlingClient;
import org.apache.sling.testing.clients.SlingClientConfig;

import java.net.URI;

/**
 * Augmented SlingClient with disabled redirect handling
 */
public class RedirectTestingClient extends SlingClient {
    public RedirectTestingClient(CloseableHttpClient http, SlingClientConfig config) throws ClientException {
        super(http, config);
    }

    public RedirectTestingClient(URI url, String user, String password) throws ClientException {
        super(url, user, password);
    }

    public static abstract class InternalBuilder<T extends RedirectTestingClient> extends SlingClient.InternalBuilder<T> {

        protected InternalBuilder(URI url, String user, String password) {
            super(url, user, password);
        }
    }

    public final static class Builder extends RedirectTestingClient.InternalBuilder<RedirectTestingClient> {

        private Builder(URI url, String user, String password) {
            super(url, user, password);
        }

        @Override
        public RedirectTestingClient build() throws ClientException {
            return new RedirectTestingClient(buildHttpClient(), buildSlingClientConfig());
        }

        public static RedirectTestingClient.Builder create(URI url, String user, String password) {
            RedirectTestingClient.Builder builder = new RedirectTestingClient.Builder(url, user, password);
            builder.disableRedirectHandling();
            return builder;
        }
    }

}
