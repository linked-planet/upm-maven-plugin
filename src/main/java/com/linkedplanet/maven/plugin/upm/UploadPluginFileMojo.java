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

package com.linkedplanet.maven.plugin.upm;


import com.google.gson.JsonObject;
import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;

/**
 * Goal to upload a plugin JAR file via UPM REST API.
 */
@Mojo(name = "uploadPluginFile")
public class UploadPluginFileMojo extends AbstractUpmMojo {

    private static final String REST_PATH_PLUGINS = "/rest/plugins/1.0";

    @SuppressWarnings("unused")
    @Parameter(property = "pluginKey")
    private String pluginKey;

    @SuppressWarnings("unused")
    @Parameter(property = "pluginFile")
    private File pluginFile;

    @SuppressWarnings("unused")
    @Parameter(property = "waitForSuccessMillis", defaultValue = "60000")
    private int waitForSuccessMillis;

    @Override
    public void execute() throws MojoExecutionException {
        try (CloseableHttpClient httpClient = createHttpClient()) {
            getLog().info("Retrieving UPM token ...");
            String token = getUpmToken(httpClient);
            getLog().info("UPM token: " + token);

            getLog().info("Uploading file: " + pluginFile + " ...");
            uploadFile(httpClient, token);

            // wait for 5 seconds before checking plugin enabled state
            Thread.sleep(5000);
            poll("Plugin installation", waitForSuccessMillis, () -> checkPluginEnabled(httpClient));


        } catch (Exception e) {
            throw new MojoExecutionException("Plugin installation error", e);
        }
    }

    private String getUpmToken(CloseableHttpClient httpClient) throws Exception {
        String url = baseUrl.toString() + REST_PATH_PLUGINS + "/?os_authType=basic";
        HttpHead request = new HttpHead(url);
        request.setHeader(getAuthHeader());

        try (CloseableHttpResponse response = httpClient.execute(request)) {
            Header[] headers = response.getHeaders("upm-token");
            if (headers.length == 0) {
                String errorMessage = "Could not find upm-token header in response";
                String statusLine = response.getStatusLine().toString();
                throw new Exception(statusLine + " " + errorMessage);
            }
            return headers[0].getValue();
        }
    }

    private void uploadFile(CloseableHttpClient httpClient, String token) throws Exception {
        String url = baseUrl.toString() + REST_PATH_PLUGINS + "/?token=" + token;
        HttpPost request = new HttpPost(url);
        request.setHeader(getAuthHeader());
        request.setEntity(MultipartEntityBuilder.create().addBinaryBody("plugin", pluginFile).build());

        try (CloseableHttpResponse response = httpClient.execute(request)) {
            if (response.getStatusLine().getStatusCode() != 202) {
                String errorMessage = EntityUtils.toString(response.getEntity());
                String statusLine = response.getStatusLine().toString();
                throw new Exception(statusLine + " " + errorMessage);
            }
        }
    }

    private boolean checkPluginEnabled(CloseableHttpClient httpClient) {
        HttpGet request = new HttpGet(baseUrl.toString() + REST_PATH_PLUGINS + '/' + pluginKey + "-key");
        request.setHeader(getAuthHeader());
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            JsonObject jsonObject = parseResponseAsJsonObject(response);
            return jsonObject.getAsJsonPrimitive("enabled").getAsBoolean();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
