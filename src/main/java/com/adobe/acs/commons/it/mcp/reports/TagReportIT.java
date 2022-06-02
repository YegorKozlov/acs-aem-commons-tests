package com.adobe.acs.commons.it.mcp.reports;

import com.adobe.acs.commons.it.mcp.cfi.testing.rules.TestContentFragmentRule;
import com.adobe.acs.commons.it.mcp.reports.rules.TagReportRule;
import com.adobe.cq.testing.client.CQClient;
import com.adobe.cq.testing.junit.rules.CQAuthorClassRule;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.primitives.UnsignedBytes;
import org.apache.http.util.EntityUtils;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.sling.testing.clients.SlingHttpResponse;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TagReportIT {

    @ClassRule
    public static CQAuthorClassRule cqBaseClassRule = new CQAuthorClassRule();

    @ClassRule
    public static TagReportRule cfRule = new TagReportRule(cqBaseClassRule.authorRule);

    static CQClient cqClient;


    private static final DateTimeFormatter jsonDateTime = DateTimeFormatter.ofPattern("EEE MMM dd yyyy HH:mm:ss 'GMT'Z");

    @BeforeClass
    public static void beforeClass() {
        cqClient = cqBaseClassRule.authorRule.getAdminClient(CQClient.class);
    }

    /**
     * create a tag report, get the xlsx file and validate the structure
     * See https://github.com/Adobe-Consulting-Services/acs-aem-commons/issues/2837
     */
    @Test
    public void createTagReport() throws Exception {
        String path = cfRule.createTagReport();
        String xlsPath = path + "/_jcr_content/report.xlsx";
        SlingHttpResponse response = cqClient.doStreamGet(xlsPath, null, null, SC_OK);
        byte[] bytes = EntityUtils.toByteArray(response.getEntity());

        Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes));
        Sheet sheet = wb.getSheetAt(0);
        Row headerRow = sheet.getRow(0);
        assertEquals("Reference Count", headerRow.getCell(0).getStringCellValue());
    }
}
