package com.adobe.acs.commons.it.mcp.cfi.testing.rules;

import com.adobe.acs.commons.it.redirects.testing.rules.SlingMappingsRule;
import com.adobe.cq.testing.client.CQClient;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
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

public class TestContentFragmentRule extends ExternalResource {
    private final Logger logger = LoggerFactory.getLogger(SlingMappingsRule.class);
    private static String CREATE_MODEL_URL = "/mnt/overlay/dam/cfm/models/console/content/createmodelwizard.html/conf/dam/cfm/models/console/content/createmodelwizard/_jcr_content";
    private static String CF_IMPORT_URL = "/apps/acs-commons/content/manage-controlled-processes/jcr:content.start.json";

    private final Instance quickstartRule;
    private CQClient client;
    private String confParentPath;
    private String cfModelPath;
    private String cfDamPath;
    private List<String> cfFragments;
    private File tmp;

    public TestContentFragmentRule(Instance quickstartRule) {
        super();
        this.quickstartRule = quickstartRule;
    }


    void createCFModel(String parentPath) throws ClientException, IOException {
        HttpEntity entity = FormEntityBuilder.create()
                .addParameter(":operation", "cfm:createModel")
                .addParameter("_parentPath_", parentPath)
                .addParameter("modelType", "/libs/settings/dam/cfm/model-types/fragment")
                .addParameter("./jcr:title", "CF Test Model")
                .addParameter("isStatusEnabled", "true")
                .build();
        client.doPost(CREATE_MODEL_URL, entity, SC_CREATED);

        cfModelPath = parentPath + "/settings/dam/cfm/models/cf-test-model";
        String json = IOUtils.toString(getClass().getResource("/mcp/cfi/cf.json"), "utf-8");
        logger.info("importing json into {}", cfModelPath);
        client.importContent(cfModelPath, "json", json);
    }


    public void createConf(String path) throws ClientException {
        if (!path.startsWith("/conf/")) {
            throw new IllegalArgumentException("conf root must start with /conf, was: " + path);
        }
        logger.info("creating cf structures under {}", path);
        client.createNode(path, "sling:Folder");
        client.createNode(path + "/settings", "sling:Folder");
        client.createNode(path + "/settings/dam", "cq:Page");
        client.createNode(path + "/settings/dam/cfm", "cq:Page");
        client.createNode(path + "/settings/dam/cfm/models", "cq:Page");
    }

    public void createDamRoot(String path) throws ClientException {
        if (!path.startsWith("/content/dam/")) {
            throw new IllegalArgumentException("dam root must start with /content/dam, was: " + path);
        }
        client.createNode(path, "sling:Folder");
        client.createNode(path + "/jcr:content", "nt:unstructured");
        logger.info("created {}", path);
    }

    @Override
    protected void before() throws ClientException, IOException {
        client = this.quickstartRule.getAdminClient(CQClient.class);


        String rnd = RandomStringUtils.randomAlphabetic(8);
        confParentPath = "/conf/" + rnd;
        cfDamPath = "/content/dam/" + rnd;
        createConf(confParentPath);
        createCFModel(confParentPath);
        createDamRoot(cfDamPath);
    }

    @Override
    protected void after() {
        try {
            logger.info("deleting {}", confParentPath);
            client.deletePath(confParentPath);

            logger.info("deleting {}", cfDamPath);
            client.deletePath(cfDamPath);
        } catch (ClientException e) {
            logger.error("Could not delete sling mappings", e);
        }
        tmp.delete();
    }

    void prepareImportData(String resourcePath) throws IOException {
        cfFragments = new ArrayList<>();
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            Workbook wb = WorkbookFactory.create(is);
            Sheet sheet = wb.getSheetAt(0);
            for (Row row : sheet) {
                Cell cellA = row.getCell(0);
                if (cellA == null) {
                    continue;
                }
                String path = cellA.getStringCellValue();
                if (path.startsWith("/content/dam/")) {
                    cellA.setCellValue(cfDamPath);

                    String cfPath = cfDamPath + "/" + row.getCell(1).getStringCellValue();
                    cfFragments.add(cfPath);
                }
                Cell cellC = row.getCell(3);
                String template = cellC.getStringCellValue();
                if (template.startsWith("/conf/")) {
                    cellC.setCellValue(cfModelPath);
                }
            }
            tmp = File.createTempFile("cf-import", "xlsx");
            logger.info("{}", tmp);
            FileOutputStream out = new FileOutputStream(tmp);
            wb.write(out);
        }

    }

    public List<String> importFragments() throws Exception {
        prepareImportData("/mcp/cfi/cf-import.xlsx");

        HttpEntity entity = MultipartEntityBuilder.create()
                .addTextBody("definition", "Content Fragment Import")
                .addBinaryBody("importFile", tmp)
                .addTextBody("dryRunMode@DefaultValue", "false")
                .addTextBody("dryRunMode@UseDefaultWhenMissing", "true")
                .build();
        String json = client.doPost(CF_IMPORT_URL, entity).getContent();

        String jobPath = JsonUtils.getJsonNodeFromString(json).get("path").asText();
        waitCompleted(jobPath, 30000L, 1000L);

        return cfFragments;
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

    public String getCfModelPath(){
        return cfModelPath;
    }
}
