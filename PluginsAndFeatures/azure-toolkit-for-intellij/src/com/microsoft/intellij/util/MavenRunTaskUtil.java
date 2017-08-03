package com.microsoft.intellij.util;

import com.intellij.execution.BeforeRunTask;
import com.intellij.execution.RunManagerEx;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import org.jetbrains.idea.maven.model.MavenConstants;
import org.jetbrains.idea.maven.project.MavenProjectsManager;
import org.jetbrains.idea.maven.tasks.MavenBeforeRunTask;
import org.jetbrains.idea.maven.tasks.MavenBeforeRunTasksProvider;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MavenRunTaskUtil {

    private static final String MAVEN_TASK_PACKAGE = "package";

    public static boolean shouldAddMavenPackageTask(List<BeforeRunTask> tasks, Project project) {
        boolean shouldAdd = true;
        for (BeforeRunTask task : tasks) {
            if (task.getProviderId().equals(MavenBeforeRunTasksProvider.ID)) {
                MavenBeforeRunTask mavenTask = (MavenBeforeRunTask) task;
                if (mavenTask.getGoal().contains(MAVEN_TASK_PACKAGE) && Comparing.equal(mavenTask.getProjectPath(),
                        project.getBasePath() + File.separator + MavenConstants.POM_XML)) {
                    mavenTask.setEnabled(true);
                    shouldAdd = false;
                    break;
                }
            }
        }
        return shouldAdd;
    }

    public static boolean isMavenProject(Project project) {
        return MavenProjectsManager.getInstance(project).isMavenizedProject();
    }


    public static void addMavenPackageBeforeRunTask(RunConfiguration runConfiguration) {
        final RunManagerEx manager = RunManagerEx.getInstanceEx(runConfiguration.getProject());
        if (isMavenProject(runConfiguration.getProject())) {
            List<BeforeRunTask> tasks = new ArrayList<>(manager.getBeforeRunTasks(runConfiguration));
            if (MavenRunTaskUtil.shouldAddMavenPackageTask(tasks, runConfiguration.getProject())) {
                MavenBeforeRunTask task = new MavenBeforeRunTask();
                task.setEnabled(true);
                task.setProjectPath(runConfiguration.getProject().getBasePath() + File.separator
                        + MavenConstants.POM_XML);
                task.setGoal(MAVEN_TASK_PACKAGE);
                tasks.add(task);
                manager.setBeforeRunTasks(runConfiguration, tasks, false);
            }
        }
    }
}