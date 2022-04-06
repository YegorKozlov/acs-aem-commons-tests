package com.adobe.acs.commons.it.sorter;

import com.adobe.cq.testing.client.CQClient;
import com.adobe.cq.testing.junit.rules.CQAuthorClassRule;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.http.HttpEntity;
import org.apache.sling.testing.clients.ClientException;
import org.apache.sling.testing.clients.util.FormEntityBuilder;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class NodeNameSorterIT {
    private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @ClassRule
    public static CQAuthorClassRule cqBaseClassRule = new CQAuthorClassRule();


    static CQClient cqClient;
    private String contentRoot;

    @BeforeClass
    public static void beforeClass() {
        cqClient = cqBaseClassRule.authorRule.getAdminClient(CQClient.class);
    }


    @Before
    public void before() throws ClientException {
        String site = RandomStringUtils.randomAlphabetic(8);
        contentRoot = "/content/" + site;
        cqClient.createNode(contentRoot, "cq:Page");
        cqClient.createNode(contentRoot + "/jcr:content", "cq:PageContent");

        Map<String, String> sortablePages = new LinkedHashMap<>();
        sortablePages.put("aaa", "aaa");
        sortablePages.put("bbb", "Bbb");
        sortablePages.put("Zorro", "Zorro");
        sortablePages.put("1", "1");
        sortablePages.put("20", "20");
        sortablePages.put("200", "200");
        sortablePages.put("101", "101");
        sortablePages.put("11", "11");
        sortablePages.put("22", "22");
        sortablePages.put("Two", "Two");

        List<String> nodeNames = new ArrayList<>(sortablePages.keySet());
        Collections.shuffle(nodeNames);
        for(String nodeName : nodeNames){
            String title = sortablePages.get(nodeName);
            createPage(nodeName, title);
        }
        log.info("nodes to sort: {}", nodeNames);
    }

    private void createPage(String name, String title) throws ClientException{
        String pagePath = contentRoot + "/" + name;
        log.info(pagePath);
        cqClient.createNode(pagePath, "cq:Page");
        cqClient.createNode(pagePath + "/jcr:content", "cq:PageContent");
        if(title != null) {
            cqClient.setPageProperty(pagePath, "jcr:title", title);
        }
    }

    @Before
    public void after() throws ClientException {
        cqClient.deletePath(contentRoot);
    }

    /**
     * curl -F":operation=acs-commons:sortNodes" \
     * -F":sorterName=byName" \
     * -F":caseSensitive=false" \
     * http://localhost:4502/content/someFolder
     */
    @Test
    public void byNodeNameCaseInsensitive() throws ClientException {

        List<String> expectedOrder = Arrays.asList("jcr:content", "1", "101", "11", "20", "200", "22", "aaa", "bbb", "Two", "Zorro");
        HttpEntity entity = FormEntityBuilder.create()
                .addParameter(":operation", "acs-commons:sortNodes")
                .addParameter(":sorterName", "byName")
                .addParameter(":caseSensitive", "false")
                .build();
        cqClient.doPost(contentRoot, entity);
        List<String> sortedNames = getChildNodes(contentRoot);
        assertEquals(expectedOrder, sortedNames);

    }

    /**
     * curl -F":operation=acs-commons:sortNodes" \
     * -F":sorterName=byName" \
     * -F":caseSensitive=true" \
     * http://localhost:4502/content/someFolder
     */
    @Test
    public void byNodeNameCaseSensitive() throws ClientException {

        List<String> expectedOrder = Arrays.asList("jcr:content", "1", "101", "11", "20", "200", "22", "Two", "Zorro", "aaa", "bbb");
        HttpEntity entity = FormEntityBuilder.create()
                .addParameter(":operation", "acs-commons:sortNodes")
                .addParameter(":sorterName", "byName")
                .addParameter(":caseSensitive", "true")
                .build();
        cqClient.doPost(contentRoot, entity);
        List<String> sortedNames = getChildNodes(contentRoot);
        assertEquals(expectedOrder, sortedNames);

    }

    /**
     * curl -F":operation=acs-commons:sortNodes" \
     * -F":sorterName=byTitle" \
     * -F":caseSensitive=true" \
     * http://localhost:4502/content/someFolder
     */
    @Test
    public void byNodeTitleCaseSensitive() throws ClientException {

        List<String> expectedOrder = Arrays.asList("jcr:content", "1", "101", "11", "20", "200", "22", "Bbb", "Two", "Zorro", "aaa");
        HttpEntity entity = FormEntityBuilder.create()
                .addParameter(":operation", "acs-commons:sortNodes")
                .addParameter(":sorterName", "byTitle")
                .addParameter(":caseSensitive", "true")
                .build();
        cqClient.doPost(contentRoot, entity);
        List<String> sortedTitles = getChildNodesTitles(contentRoot);
        assertEquals(expectedOrder, sortedTitles);

    }

    /**
     * curl -F":operation=acs-commons:sortNodes" \
     * -F":sorterName=byTitle" \
     * -F":caseSensitive=false" \
     * http://localhost:4502/content/someFolder
     */
    @Test
    public void byNodeTitleCaseInsensitive() throws ClientException {

        List<String> expectedOrder = Arrays.asList("jcr:content", "1", "101", "11", "20", "200", "22", "aaa", "Bbb", "Two", "Zorro");
        HttpEntity entity = FormEntityBuilder.create()
                .addParameter(":operation", "acs-commons:sortNodes")
                .addParameter(":sorterName", "byTitle")
                .addParameter(":caseSensitive", "false")
                .build();
        cqClient.doPost(contentRoot, entity);
        List<String> sortedTitles = getChildNodesTitles(contentRoot);
        assertEquals(expectedOrder, sortedTitles);

    }

    /**
     * curl -F":operation=acs-commons:sortNodes" \
     * -F":sorterName=byName" \
     * -F":caseSensitive=false" \
     * -F":respectNumbers=true" \
     * http://localhost:4502/content/someFolder
     */
    @Test
    public void byNodeNameCaseInsensitiveRespectNumbers() throws ClientException {

        List<String> expectedOrder = Arrays.asList("jcr:content", "1", "11", "20", "22", "101", "200", "aaa", "bbb", "Two", "Zorro");
        HttpEntity entity = FormEntityBuilder.create()
                .addParameter(":operation", "acs-commons:sortNodes")
                .addParameter(":sorterName", "byName")
                .addParameter(":caseSensitive", "false")
                .addParameter(":respectNumbers", "true")
                .build();
        cqClient.doPost(contentRoot, entity);
        List<String> sortedNames = getChildNodes(contentRoot);
        assertEquals(expectedOrder, sortedNames);

    }

    List<String> getChildNodes(String path) throws ClientException {
        List<String> nodes = new ArrayList<>();
        ObjectNode json = (ObjectNode)cqClient.doGetJson(path, 1);

        Iterator<Map.Entry<String, JsonNode>> it = json.fields();
        while(it.hasNext()){
            Map.Entry<String, JsonNode> e = it.next();
            if(e.getValue().isObject()) {
                nodes.add(e.getKey());
            }
        }
        return nodes;
    }

    List<String> getChildNodesTitles(String path) throws ClientException {
        List<String> nodes = new ArrayList<>();
        ObjectNode json = (ObjectNode)cqClient.doGetJson(path, 2);

        Iterator<Map.Entry<String, JsonNode>> it = json.fields();
        while(it.hasNext()){
            Map.Entry<String, JsonNode> e = it.next();
            if(e.getValue().isObject()) {
                JsonNode value = e.getValue();
                JsonNode title = value.get("jcr:title");
                if(title == null) {
                    title = value.at("/jcr:content/jcr:title");
                }
                nodes.add(title == null ? e.getKey() : title.asText(e.getKey()));
            }
        }
        return nodes;
    }
}
