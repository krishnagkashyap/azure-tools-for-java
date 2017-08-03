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

package com.microsoft.intellij.runner.container.webapponlinux;

import com.intellij.execution.DefaultExecutionResult;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionResult;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessListener;
import com.intellij.execution.process.ProcessOutputTypes;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azuretools.core.mvp.model.webapp.AzureWebAppMvpModel;
import com.microsoft.azuretools.core.mvp.model.webapp.PrivateRegistryImageSetting;
import com.microsoft.intellij.container.Constant;
import com.microsoft.intellij.container.utils.DockerUtil;
import com.microsoft.intellij.runner.RunProcessHandler;
import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.ProgressHandler;
import com.spotify.docker.client.exceptions.DockerException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.project.MavenProjectsManager;
import rx.Observable;
import rx.schedulers.Schedulers;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class WebAppOnLinuxDeployState implements RunProfileState {
    private final WebAppOnLinuxDeployModel webAppOnLinuxDeployModel;
    private final Project project;

    private final RunProcessHandler processHandler = new RunProcessHandler();

    public WebAppOnLinuxDeployState(Project project, WebAppOnLinuxDeployModel webAppOnLinuxDeployModel) {
        this.webAppOnLinuxDeployModel = webAppOnLinuxDeployModel;
        this.project = project;
    }

    @Override
    public ExecutionResult execute(Executor executor, @NotNull ProgramRunner programRunner) throws ExecutionException {
        ConsoleView consoleView = TextConsoleBuilderFactory.getInstance().createBuilder(this.project).getConsole();
        processHandler.startNotify();
        consoleView.attachToProcess(processHandler);

        Observable.fromCallable(
                () -> {
                    println("Starting job ...  ");
                    WebAppOnLinuxDeployModel.WebAppOnLinuxInfo webInfo = webAppOnLinuxDeployModel
                            .getWebAppOnLinuxInfo();

                    // locate war file to specified location
                    println("Locate war file ...  ");
                    List<MavenProject> mavenProjects = MavenProjectsManager.getInstance(project).getRootProjects();
                    String targetBuildPath = new File(mavenProjects.get(0).getBuildDirectory()).getPath()
                            + File.separator + mavenProjects.get(0).getFinalName() + ".war";
                    String fileName = mavenProjects.get(0).getFinalName() + ".war";

                    // build image
                    println("Build image ...  ");
                    String dockerContent = String.format(Constant.DOCKERFILE_CONTENT_TOMCAT, project.getName());
                    DockerUtil.createDockerFile(project, "target", "Dockerfile", dockerContent);
                    DockerClient docker = DefaultDockerClient.fromEnv().build();
                    String latestImageName = DockerUtil.buildImage(docker, project,
                            new File(mavenProjects.get(0).getBuildDirectory()).toPath());

                    // push to ACR
                    println("Push to ACR ...  ");
                    ProgressHandler progressHandler = message -> {
                        // TODO: progress output
                        if (message.error() != null) {
                            throw new DockerException(message.toString());
                        }
                    };
                    PrivateRegistryImageSetting acrInfo = webAppOnLinuxDeployModel.getAzureContainerRegistryInfo();
                    DockerUtil.pushImage(docker, acrInfo.getServerUrl(), acrInfo.getUsername(), acrInfo.getPassword(),
                            latestImageName, acrInfo.getImageNameWithTag(), progressHandler);

                    // update WebApp
                    println("Update WebApp ...  ");
                    WebApp app = AzureWebAppMvpModel.getInstance().updateWebAppOnLinux(webInfo.getSubscriptionId(),
                            webInfo.getWebAppId(), acrInfo);
                    if (app != null && app.name() != null) {
                        println(String.format("URL:  http://%s.azurewebsites.net/%s", app.name(), project.getName()));
                    }
                    return null;
                }
        ).subscribeOn(Schedulers.io()).subscribe(
                (res) -> {
                    println("Job done");
                    processHandler.notifyProcessTerminated(0);
                },
                (err) -> {
                    err.printStackTrace();
                    errorln(err.getMessage());
                    processHandler.notifyProcessTerminated(0);
                }
        );
        return new DefaultExecutionResult(consoleView, processHandler);
    }

    private void println(String message, Key type) {
        if (!processHandler.isProcessTerminating() && !processHandler.isProcessTerminated()) {
            processHandler.notifyTextAvailable(message + "\n", type);
        } else {
            throw new Error("The process has been terminated");
        }
    }

    private void println(String message) {
        println(message, ProcessOutputTypes.SYSTEM);
    }

    private void errorln(String message) {
        println(message, ProcessOutputTypes.STDERR);
    }
}