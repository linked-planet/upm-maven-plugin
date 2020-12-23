# UPM Maven Plugin
Provides Maven goals to interact with Atlassian UPM REST API.

## Configuration
```
<plugin>
    <groupId>com.linked-planet.maven.plugin.atlassian</groupId>
    <artifactId>upm-maven-plugin</artifactId>
    <version>2.0.0</version>
    <configuration>
        <!-- mandatory parameters -->
        <baseUrl>https://jira.example.com</baseUrl>
        <username>admin</username>
        <password>admin</password>
        <!-- optional parameters -->
        <timeoutMillis>10000</timeoutMillis>
    </configuration>
</plugin>
```

## Upload plugin JAR file
```
mvn upm:uploadPluginFile -DpluginKey="my-plugin" -DpluginFile="my-plugin-1.0.0.jar"
```
Plugin file path is either absolute or relative to `pom.xml` directory.

### Optional parameters

| Parameter | Default value | Purpose |
| --------- | ------------- | ------- |
| waitForSuccessMillis | 60000 | How long to wait for installation success after file upload |

Note that we cannot detect whether the plugin failed to install for certain.
We can detect that it did not enable within the given `waitForSuccessMillis`.

## Trigger re-index
Atlassian Jira will complain if not re-indexed after changes to plugins.
The plugin therefore provides a command that can be used to re-index Jira after
plugin installation. Note however that in most cases, this is not necessary and
probably even a bad idea for busy sites.

Caveat: Reindex is pointless if triggered before plugin installation finishes.
To avoid this, use `waitForSuccessMillis` when uploading the plugin file.

```
mvn upm:reindex
```

### Optional parameters

| Parameter | Default value | Purpose |
| --------- | ------------- | ------- |
| waitForSuccessMillis | 60000 | How long to wait for reindex to finish; Build will still succeed if exceeded |
