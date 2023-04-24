/*
 * #%L
 * upm-maven-plugin
 * %%
 * Copyright (C) 2019-2023 The Plugin Authors
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

package com.linkedplanet.maven.plugin.upm;


import com.google.gson.JsonObject;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Goal to upload a plugin JAR file via UPM REST API.
 */
@Mojo(name = "reindex")
public class ReindexMojo extends AbstractUpmMojo {

    private static final String REST_PATH_REINDEX = "/rest/api/2/reindex";

    @SuppressWarnings("unused")
    @Parameter(property = "waitForSuccessMillis", defaultValue = "60000")
    private int waitForSuccessMillis;

    @Override
    public void execute() throws MojoExecutionException {
        try (CloseableHttpClient httpClient = createHttpClient()) {
            getLog().info("Triggering background re-index ...");
            triggerReindex(httpClient);
            poll("Re-index", waitForSuccessMillis, () -> checkReindexProgress(httpClient));

        } catch (Exception e) {
            throw new MojoExecutionException("Reindex error", e);
        }
    }

    private void triggerReindex(CloseableHttpClient httpClient) throws Exception {
        HttpPost request = new HttpPost(baseUrl.toString() + REST_PATH_REINDEX);
        request.setHeader(getAuthHeader());
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            if (response.getStatusLine().getStatusCode() != 202) {
                throw new Exception(response.getStatusLine().toString());
            }
        }
    }

    private boolean checkReindexProgress(CloseableHttpClient httpClient) {
        HttpGet request = new HttpGet(baseUrl.toString() + REST_PATH_REINDEX + "/progress");
        request.setHeader(getAuthHeader());
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            JsonObject jsonObject = parseResponseAsJsonObject(response);
            int progress = jsonObject.getAsJsonPrimitive("currentProgress").getAsInt();
            getLog().info("Reindex progress: " + progress + "/100");

            boolean success = jsonObject.getAsJsonPrimitive("success").getAsBoolean();
            if (progress == 100 && !success) {
                throw new Exception("Re-index failed");
            }
            return success;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
