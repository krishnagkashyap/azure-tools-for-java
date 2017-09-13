/*
 * Copyright (c) Microsoft Corporation
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.azuretools.core.mvp.model.container;

import com.google.gson.Gson;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.util.Utils;
import com.microsoft.azuretools.core.mvp.model.container.pojo.Repository;
import com.microsoft.azuretools.core.mvp.model.container.pojo.Tag;

import okhttp3.Credentials;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.util.ArrayList;
import java.util.List;

public class ContainerExplorerMvpModel {

    private static final OkHttpClient sharedClient = new OkHttpClient();
    private static final String URL_PREFIX = "https://";
    private static final String REPOSITORY_PATH = "/v2/_catalog";
    private static final String TAG_PATH = "/v2/%s/tags/list";
    private static final String HEADER_LINK = "link";
    private static final String HEADER_AUTH = "Authorization";

    private ContainerExplorerMvpModel() {
    }

    private static final class ContainerExplorerMvpModelHolder {
        private static final ContainerExplorerMvpModel INSTANCE = new ContainerExplorerMvpModel();
    }

    public static ContainerExplorerMvpModel getInstance() {
        return ContainerExplorerMvpModelHolder.INSTANCE;
    }

    /**
     * list repositories under the given private registry.
     */
    public List<String> listRepositories(@NotNull String serverUrl, @NotNull String username,
                                         @NotNull String password) throws Exception {
        List<String> ret = new ArrayList<>();
        OkHttpClient client = createRestClient(username, password);
        final String host = URL_PREFIX + serverUrl;
        String path = REPOSITORY_PATH;
        boolean executeNextHttpCall = true;
        while (executeNextHttpCall) {
            HttpUrl route = HttpUrl.parse(host + path);
            Request request = new Request.Builder().url(route).get().build();
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    Gson gson = new Gson();
                    Repository repo = gson.fromJson(response.body().string(), Repository.class);
                    ret.addAll(repo.getRepositories());
                    // TODO: add page division
                    String linkHeader = response.header(HEADER_LINK);
                    if (!Utils.isEmptyString(linkHeader)) {
                        path = linkHeader.substring(linkHeader.indexOf("<") + 1, linkHeader.lastIndexOf(">"));
                        executeNextHttpCall = true;
                    } else {
                        executeNextHttpCall = false;
                    }
                } else {
                    throw new Exception(response.message());
                }
            }
        }
        return ret;
    }

    /**
     * list repositories under the given repository.
     */
    public List<String> listTags(@NotNull String serverUrl, @NotNull String username, @NotNull String password,
                                 @NotNull String repo) throws Exception {
        List<String> ret = new ArrayList<>();
        OkHttpClient client = createRestClient(username, password);
        final String host = URL_PREFIX + serverUrl;
        String path = String.format(TAG_PATH, repo);
        boolean executeNextHttpCall = true;
        while (executeNextHttpCall) {
            HttpUrl route = HttpUrl.parse(host + path);
            Request request = new Request.Builder().url(route).get().build();
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    Gson gson = new Gson();
                    Tag tag = gson.fromJson(response.body().string(), Tag.class);
                    ret.addAll(tag.getTags());
                    // TODO: add page division
                    String linkHeader = response.header(HEADER_LINK);
                    if (!Utils.isEmptyString(linkHeader)) {
                        path = linkHeader.substring(linkHeader.indexOf("<") + 1, linkHeader.lastIndexOf(">"));
                        executeNextHttpCall = true;
                    } else {
                        executeNextHttpCall = false;
                    }
                } else {
                    throw new Exception(response.message());
                }
            }
        }
        return ret;
    }

    private OkHttpClient createRestClient(@NotNull String username, @NotNull String password) {
        return sharedClient.newBuilder()
                .authenticator((route, response) -> {
                    String credential = Credentials.basic(username, password);
                    return response.request().newBuilder().header(HEADER_AUTH, credential).build();
                })
                .build();
    }
}