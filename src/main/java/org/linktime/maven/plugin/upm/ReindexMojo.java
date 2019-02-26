/*
 * #%L
 * upm-maven-plugin
 * %%
 * Copyright (C) 2019 The Plugin Authors
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

package org.linktime.maven.plugin.upm;


import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Goal to upload a plugin JAR file via UPM REST API.
 */
@Mojo(name = "reindex")
public class ReindexMojo extends AbstractUpmMojo {

    private static final String REST_PATH = "/rest/api/2/reindex";

    @SuppressWarnings("unused")
    @Parameter(property = "waitForSuccessMillis", defaultValue = "60000")
    private int waitForSuccessMillis;

    @Override
    public void execute() throws MojoExecutionException {
        try (CloseableHttpClient httpClient = createHttpClient()) {
            getLog().info("Triggering background re-index ...");
            triggerReindex(httpClient);

            long millisWaited = 0;
            boolean success = false;
            while (!success && millisWaited < waitForSuccessMillis) {
                getLog().info("Waiting for re-index success (" + millisWaited + "/" + waitForSuccessMillis + " millis waited) ...");
                long beginWaitMillis = System.currentTimeMillis();
                success = checkReindexProgress(httpClient);
                millisWaited += System.currentTimeMillis() - beginWaitMillis;
            }

            if (millisWaited >= waitForSuccessMillis && !success) {
                getLog().info("No longer waiting for re-index success after " + waitForSuccessMillis + " millis.");
            }
            if (success) {
                getLog().info("Background re-index finished successfully.");
            }

        } catch (Exception e) {
            throw new MojoExecutionException("Reindex error", e);
        }
    }

    private void triggerReindex(CloseableHttpClient httpClient) throws Exception {
        HttpPost request = new HttpPost(baseUrl.toString() + REST_PATH);
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            if (response.getStatusLine().getStatusCode() != 202) {
                throw new Exception(response.getStatusLine().toString());
            }
        }
    }

    private boolean checkReindexProgress(CloseableHttpClient httpClient) throws Exception {
        HttpGet request = new HttpGet(baseUrl.toString() + REST_PATH + "/progress");
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            if (response.getStatusLine().getStatusCode() != 200) {
                throw new Exception(response.getStatusLine().toString());
            }
            String json = EntityUtils.toString(response.getEntity());
            JsonObject jsonElement = new JsonParser().parse(json).getAsJsonObject();
            int progress = jsonElement.getAsJsonPrimitive("currentProgress").getAsInt();
            boolean success = jsonElement.getAsJsonPrimitive("success").getAsBoolean();
            if (progress == 100 && !success) {
                throw new Exception("Re-index failed");
            }
            return success;
        }
    }

}
