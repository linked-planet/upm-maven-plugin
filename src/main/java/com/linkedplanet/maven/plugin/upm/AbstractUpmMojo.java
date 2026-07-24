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
import com.google.gson.JsonParser;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.function.Predicate;
import java.util.function.Supplier;

abstract class AbstractUpmMojo extends AbstractMojo {

    @SuppressWarnings("unused")
    @Parameter(property = "baseUrl")
    URL baseUrl;

    @SuppressWarnings("unused")
    @Parameter(property = "username")
    private String username;

    @SuppressWarnings("unused")
    @Parameter(property = "password")
    private String password;

    @SuppressWarnings("unused")
    @Parameter(property = "accessToken")
    private String accessToken;

    @SuppressWarnings("unused")
    @Parameter(property = "timeoutMillis", defaultValue = "10000")
    private int timeoutMillis;

    CloseableHttpClient createHttpClient() {
        return HttpClients.custom()
                .setDefaultRequestConfig(RequestConfig.custom()
                        .setCookieSpec(CookieSpecs.STANDARD)
                        .setSocketTimeout(timeoutMillis)
                        .setConnectTimeout(timeoutMillis)
                        .setConnectionRequestTimeout(timeoutMillis)
                        .build())
                .build();
    }

    void validateAuthConfiguration() throws MojoExecutionException {
        if (isBlankOrNull(accessToken) && (isBlankOrNull(username) || isBlankOrNull(password))) {
            throw new MojoExecutionException(
                    "No credentials configured: provide either 'accessToken' or both 'username' and 'password'.");
        }
        if (!isBlankOrNull(accessToken) && (!isBlankOrNull(username) || !isBlankOrNull(password))) {
            getLog().warn("Both 'accessToken' and 'username'/'password' are configured; 'accessToken' takes precedence and the username/password will be ignored.");
        }
    }

    BasicHeader getAuthHeader() {
        if (!isBlankOrNull(accessToken)) {
            return new BasicHeader("authorization", "Bearer " + accessToken);
        }
        return new BasicHeader(
                "authorization",
                "Basic " + Base64.encodeBase64String((username + ":" + password).getBytes(StandardCharsets.UTF_8)));
    }

    private static boolean isBlankOrNull(String value) {
        return value == null || value.trim().isEmpty();
    }

    Boolean poll(String taskName, long maxWaitMillis, Supplier<Boolean> task) {
        return poll(taskName, maxWaitMillis, task, result -> result);
    }

    Result pollResult(String taskName, long maxWaitMillis, Supplier<Result> task) {
        return poll(taskName, maxWaitMillis, task, Result::isSuccess);
    }

    <T> T poll(String taskName, long maxWaitMillis, Supplier<T> task, Predicate<T> isCompleted) {
        long millisWaited = 0;
        T taskResult = null;
        while (millisWaited < maxWaitMillis) {
            getLog().info(taskName + ": Waiting for success (" + millisWaited + "/" + maxWaitMillis + " millis waited) ...");
            long beginWaitMillis = System.currentTimeMillis();
            taskResult = task.get();
            if (isCompleted.test(taskResult)) {
                return taskResult;
            }
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
            millisWaited += System.currentTimeMillis() - beginWaitMillis;
        }
        getLog().info(taskName + ": No longer waiting for success after " + maxWaitMillis + " millis");
        return taskResult;
    }

    static JsonObject parseResponseAsJsonObject(CloseableHttpResponse response) throws Exception {
        if (response.getStatusLine().getStatusCode() != 200) {
            throw new Exception(response.getStatusLine().toString());
        }
        String json = EntityUtils.toString(response.getEntity());
        return JsonParser.parseString(json).getAsJsonObject();
    }

}
