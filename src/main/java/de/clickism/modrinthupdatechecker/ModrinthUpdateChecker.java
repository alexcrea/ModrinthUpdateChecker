/*
 * MIT License
 *
 * Copyright (c) 2025 Clickism
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package de.clickism.modrinthupdatechecker;

import com.google.gson.*;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.function.Consumer;

/**
 * Utility class to check for newer versions of a project hosted on Modrinth.
 */
public class ModrinthUpdateChecker {

    private static final String API_URL = "https://api.modrinth.com/v2/project/{id}/version?{param}";

    private final String projectId;
    private final String loader;
    @Nullable
    private final String minecraftVersion;

    private boolean acceptRelease = true;
    private boolean acceptBeta = true;
    private boolean acceptAlpha = true;

    /**
     * Create a new update checker for the given project.
     * This will check the latest version for the given loader and any minecraft version.
     *
     * @param projectId the project ID
     * @param loader    the loader
     */
    public ModrinthUpdateChecker(String projectId, String loader) {
        this(projectId, loader, null);
    }

    /**
     * Create a new update checker for the given project.
     * This will check the latest version for the given loader and minecraft version.
     *
     * @param projectId        the project ID
     * @param loader           the loader
     * @param minecraftVersion the minecraft version, or null for any version
     */
    public ModrinthUpdateChecker(String projectId, String loader, @Nullable String minecraftVersion) {
        this.projectId = projectId;
        this.loader = loader;
        this.minecraftVersion = minecraftVersion;
    }

    /**
     * Check the latest version of the project for the given loader and minecraft version
     * and call the consumer with it.
     *
     * @param consumer the consumer
     */
    public void checkVersion(Consumer<String> consumer) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(PrepareURL()))
                    .GET()
                    .build();

            client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAcceptAsync(response -> {
                        if (response.statusCode() != 200) return;
                        JsonArray versionsArray = JsonParser.parseString(response.body()).getAsJsonArray();
                        String latestVersion = getLatestVersion(versionsArray);
                        if (latestVersion == null) return;
                        consumer.accept(latestVersion);
                    });
        } catch (Exception ignored) {
        }
    }

    /**
     * Get the latest compatible version from the versions array.
     *
     * @param versions the versions array
     * @return the latest compatible version
     */
    @Nullable
    protected String getLatestVersion(JsonArray versions) {
        return versions.asList().stream().findFirst()
                .map(JsonElement::getAsJsonObject)
                .map(version -> version.get("version_number").getAsString())
                .map(ModrinthUpdateChecker::getRawVersion)
                .orElse(null);
    }

    /**
     * Gets the raw version from a version string.
     * i.E: "fabric-1.2+1.17.1" -> "1.2"
     *
     * @param version the version string
     * @return the raw version string
     */
    public static String getRawVersion(String version) {
        if (version.isEmpty()) return version;
        version = version.replaceAll("^\\D+", "");
        String[] split = version.split("\\+");
        return split[0];
    }

    /**
     * Get the modrinth url correct for requested parameters.
     *
     * @return the url to request to. null if request cannot be satisfied
     */
    @Nullable
    private String PrepareURL(){
        if(!acceptAlpha && !acceptBeta && !acceptRelease) return null;

        // Prepare project url
        var projectURL = API_URL.replace("{id}", projectId);

        // Prepare arguments
        var parameterList = new ArrayList<String>();

        if(acceptRelease) parameterList.add("c=release");
        if(acceptBeta) parameterList.add("c=beta");
        if(acceptAlpha) parameterList.add("c=alpha");

        if(minecraftVersion != null) parameterList.add("g=" + minecraftVersion);

        parameterList.add("l=" + loader.toLowerCase());

        var parameters = String.join("&", parameterList);
        return projectURL.replace("{params}", parameters);
    }

    /**
     * Set if we should accept release versions.
     * Default is true.
     *
     * @param acceptRelease if we should accept release versions
     */
    public void setAcceptRelease(boolean acceptRelease) {
        this.acceptRelease = acceptRelease;
    }

    /**
     * Set if we should accept beta versions.
     * Default is true.
     *
     * @param acceptBeta if we should accept beta versions
     */
    public void setAcceptBeta(boolean acceptBeta) {
        this.acceptBeta = acceptBeta;
    }

    /**
     * Set if we should accept alpha versions.
     * Default is true.
     *
     * @param acceptAlpha if we should accept alpha versions
     */
    public void setAcceptAlpha(boolean acceptAlpha) {
        this.acceptAlpha = acceptAlpha;
    }

}
