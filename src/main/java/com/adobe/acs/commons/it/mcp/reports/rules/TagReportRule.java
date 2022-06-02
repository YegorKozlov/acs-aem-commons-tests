package com.adobe.acs.commons.it.mcp.reports.rules;

import com.adobe.acs.commons.it.redirects.testing.rules.SlingMappingsRule;
import com.adobe.cq.testing.client.CQClient;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.entity.mime.FormBodyPartBuilder;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.poi.ss.usermodel.*;
import org.apache.sling.testing.clients.ClientException;
import org.apache.sling.testing.clients.util.FormEntityBuilder;
import org.apache.sling.testing.clients.util.JsonUtils;
import org.apache.sling.testing.clients.util.poller.Polling;
import org.apache.sling.testing.junit.rules.instance.Instance;
import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

import static org.apache.http.HttpStatus.SC_CREATED;

public class TagReportRule extends ExternalResource {
    private final Logger logger = LoggerFactory.getLogger(SlingMappingsRule.class);
    private static String START_URL = "/apps/acs-commons/content/manage-controlled-processes/jcr:content.start.json";

    private final Instance quickstartRule;
    private CQClient client;

    List<String> createdReports;

    public TagReportRule(Instance quickstartRule) {
        super();
        this.quickstartRule = quickstartRule;
    }


    @Override
    protected void before() {
        client = this.quickstartRule.getAdminClient(CQClient.class);
        createdReports = new ArrayList<>();
    }

    @Override
    protected void after() {
        for(String path : createdReports){
            try {
                client.deletePath(path);
            } catch (ClientException e) {
                throw new RuntimeException(e);
            }
        }
    }


   public String createTagReport() throws Exception {
        HttpEntity entity = FormEntityBuilder.create()
                .addParameter("definition", "Tag Report")
                .addParameter("rootSearchPath", "/content")
                .addParameter("tagPath", "/content/cq:tags")
                .addParameter("includeReferences@Delete", "")
                .addParameter("includeReferences@UseDefaultWhenMissing", "true")
                .addParameter("referencesCharacterLimit", "4096")
                .addParameter("referenceMethod@Delete", "")
                .addParameter("referenceMethod@Delete", "")
                .build();
        String json = client.doPost(START_URL, entity).getContent();

        String jobPath = JsonUtils.getJsonNodeFromString(json).get("path").asText();
        waitCompleted(jobPath, 30000L, 1000L);

        return jobPath;
    }

    public void waitCompleted(final String path, final long timeout, final long delay)
            throws TimeoutException, InterruptedException {

        Polling p = new Polling() {
            @Override
            public Boolean call() throws Exception {
                String status = client.doGetJson(path + "/jcr:content", -1).get("status").asText();
                return "Completed".equals(status);
            }

            @Override
            protected String message() {
                return "Path " + path + " does not exist after %1$d ms";
            }
        };

        p.poll(timeout, delay);
    }

}
