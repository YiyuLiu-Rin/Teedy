package com.sismics.docs.rest;

import java.util.Date;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Form;

import org.junit.Assert;
import org.junit.Test;

import com.sismics.util.filter.TokenBasedSecurityFilter;

/**
 * Test the audit log resource.
 * 
 * @author bgamard
 */
public class TestAuditLogResource extends BaseJerseyTest {
    /**
     * Test the audit log resource.
     */
    @Test
    public void testAuditLogResource() {
        // Login auditlog1
        clientUtil.createUser("auditlog1");
        String auditlog1Token = clientUtil.login("auditlog1");
        
        // Create a tag
        JsonObject json = target().path("/tag").request()
                .cookie(TokenBasedSecurityFilter.COOKIE_NAME, auditlog1Token)
                .put(Entity.form(new Form()
                        .param("name", "SuperTag")
                        .param("color", "#ffff00")), JsonObject.class);
        String tag1Id = json.getString("id");
        Assert.assertNotNull(tag1Id);

        // Create a document
        long create1Date = new Date().getTime();
        json = target().path("/document").request()
                .cookie(TokenBasedSecurityFilter.COOKIE_NAME, auditlog1Token)
                .put(Entity.form(new Form()
                        .param("title", "My super title document 1")
                        .param("description", "My super description for document 1")
                        .param("tags", tag1Id)
                        .param("language", "eng")
                        .param("create_date", Long.toString(create1Date))), JsonObject.class);
        String document1Id = json.getString("id");
        Assert.assertNotNull(document1Id);
        
        // Get all logs for the document
        json = target().path("/auditlog")
                .queryParam("document", document1Id)
                .request()
                .cookie(TokenBasedSecurityFilter.COOKIE_NAME, auditlog1Token)
                .get(JsonObject.class);
        JsonArray logs = json.getJsonArray("logs");
        Assert.assertTrue(logs.size() == 3);
        Assert.assertEquals(countByClass(logs, "Document"), 1);
        Assert.assertEquals(countByClass(logs, "Acl"), 2);
        Assert.assertEquals("auditlog1", logs.getJsonObject(0).getString("username"));
        Assert.assertNotNull(logs.getJsonObject(0).getString("id"));
        Assert.assertNotNull(logs.getJsonObject(0).getString("target"));
        Assert.assertNotNull(logs.getJsonObject(0).getString("type"));
        Assert.assertNotNull(logs.getJsonObject(0).getString("message"));
        Assert.assertNotNull(logs.getJsonObject(0).getJsonNumber("create_date"));
        Assert.assertEquals("auditlog1", logs.getJsonObject(1).getString("username"));
        Assert.assertEquals("auditlog1", logs.getJsonObject(2).getString("username"));
        
        // Get all logs for the current user
        json = target().path("/auditlog").request()
                .cookie(TokenBasedSecurityFilter.COOKIE_NAME, auditlog1Token)
                .get(JsonObject.class);
        logs = json.getJsonArray("logs");
        Assert.assertTrue(logs.size() == 2);
        Assert.assertEquals(countByClass(logs, "Document"), 1);
        Assert.assertEquals(countByClass(logs, "Tag"), 1);
        
        // Deletes a tag
        json = target().path("/tag/" + tag1Id).request()
                .cookie(TokenBasedSecurityFilter.COOKIE_NAME, auditlog1Token)
                .delete(JsonObject.class);
        Assert.assertEquals("ok", json.getString("status"));
        
        // Get all logs for the current user
        json = target().path("/auditlog").request()
                .cookie(TokenBasedSecurityFilter.COOKIE_NAME, auditlog1Token)
                .get(JsonObject.class);
        logs = json.getJsonArray("logs");
        Assert.assertTrue(logs.size() == 3);
        Assert.assertEquals(countByClass(logs, "Document"), 1);
        Assert.assertEquals(countByClass(logs, "Tag"), 2);
    }
    
    /**
     * Count logs by class.
     * 
     * @param logs Logs
     * @param clazz Class
     * @return Count by class
     */
    private int countByClass(JsonArray logs, String clazz) {
        int count = 0;
        for (int i = 0; i < logs.size(); i++) {
            if (logs.getJsonObject(i).getString("class").equals(clazz)) {
                count++;
            }
        }
        return count;
    }
}