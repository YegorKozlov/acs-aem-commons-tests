package com.adobe.acs.commons.it.mcp.cfi;

import com.adobe.acs.commons.it.mcp.cfi.testing.rules.TestContentFragmentRule;
import com.adobe.cq.testing.client.CQClient;
import com.adobe.cq.testing.junit.rules.CQAuthorClassRule;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ContentFragmentImportIT {

    @ClassRule
    public static CQAuthorClassRule cqBaseClassRule = new CQAuthorClassRule();

    @ClassRule
    public static TestContentFragmentRule cfRule = new TestContentFragmentRule(cqBaseClassRule.authorRule);

    static CQClient cqClient;

    @BeforeClass
    public static void beforeClass() {
        cqClient = cqBaseClassRule.authorRule.getAdminClient(CQClient.class);
    }

    @Test
    public void importFromSpreadsheetIT() throws Exception {
        List<String> fragments = cfRule.importFragments();
        for (String path : fragments) {
            JsonNode jcrContent = cqClient.doGetJson(path + "/jcr:content", 1);
            assertEquals(path, true, jcrContent.get("contentFragment").asBoolean());
            assertEquals(path, "CF Import", jcrContent.get("jcr:title").asText());

            assertEquals(path, cfRule.getCfModelPath(), cqClient.doGetJson(path + "/jcr:content/data", 1).get("cq:model").asText());

            JsonNode props = cqClient.doGetJson(path + "/jcr:content/data/master", 1);
            assertEquals(path, true, props.get("boolean").asBoolean());
            assertEquals(path, "Sat Feb 16 1974 00:00:00 GMT+0000", props.get("date") == null ? null : props.get("date").asText());
            assertEquals(path, "Sat Feb 16 1974 13:36:00 GMT+0000", props.get("dateTime") == null ? null : props.get("dateTime").asText());
            assertEquals(path, "<p>Hello, World!</p>", props.get("multiLine").asText());
            assertEquals(path, "single line", props.get("singleLine").asText());
            assertEquals(path, 1974, props.get("numberField").asInt());

            JsonNode stringMultifield = props.get("stringMultifield");
            assertTrue(stringMultifield.isArray());
            List<String> vals = new ArrayList<>();
            for (JsonNode jsonNode : stringMultifield) {
                vals.add(jsonNode.asText());
            }
            assertEquals(path, Arrays.asList("Line 1", "Line 2", "Line 3"), vals);

            JsonNode metadata = cqClient.doGetJson(path + "/jcr:content/metadata", 1);
            assertEquals(path, "Hello, CF Import!", metadata.get("jcr:description").asText());
            List<String> tags = new ArrayList<>();
            for (JsonNode jsonNode : metadata.get("cq:tags")) {
                tags.add(jsonNode.asText());
            }
            assertEquals(path, Arrays.asList("properties:orientation/portrait", "properties:orientation/square"), tags);
        }

    }
}
