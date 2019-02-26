/*
 * #%L
 * atlassian-upm-maven-plugin
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


import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;

/**
 * Goal to upload a plugin JAR file via UPM REST API.
 */
@Mojo(name = "uploadPluginFile")
public class UploadPluginFileMojo extends AbstractUpmMojo {

    private static final String REST_PATH = "/rest/plugins/1.0/";

    @SuppressWarnings("unused")
    @Parameter(property = "pluginFile")
    private File pluginFile;

    @SuppressWarnings("unused")
    @Parameter(property = "waitForInstallationMillis", defaultValue = "5000")
    private int waitForInstallationMillis;

    @Override
    public void execute() throws MojoExecutionException {
        try (CloseableHttpClient httpClient = createHttpClient()) {
            getLog().info("Retrieving UPM token ...");
            String token = getUpmToken(httpClient);

            getLog().info("Uploading file: " + pluginFile + "...");
            uploadFile(httpClient, token);

        } catch (Exception e) {
            throw new MojoExecutionException("Plugin installation error", e);
        }
    }

    private String getUpmToken(CloseableHttpClient httpClient) throws Exception {
        String url = baseUrl.toString() + REST_PATH + "?os_authType=basic";
        HttpHead request = new HttpHead(url);

        try (CloseableHttpResponse response = httpClient.execute(request)) {
            Header[] headers = response.getHeaders("upm-token");
            if (headers.length == 0) {
                throw new Exception("Could not find upm-token header in response");
            }
            return headers[0].getValue();
        }
    }

    private void uploadFile(CloseableHttpClient httpClient, String token) throws Exception {
        String url = baseUrl.toString() + REST_PATH + "?token=" + token;
        HttpPost request = new HttpPost(url);
        request.setEntity(MultipartEntityBuilder.create().addBinaryBody("plugin", pluginFile).build());

        try (CloseableHttpResponse response = httpClient.execute(request)) {
            if (response.getStatusLine().getStatusCode() != 202) {
                throw new Exception(response.getStatusLine().toString());
            }
            // wait for installation to finish - currently we just have to assume that:
            // - installation will be successful
            // - it takes a maximum of waitForInstallationMillis to install the plugin
            Thread.sleep(waitForInstallationMillis);
        }
    }

}
