/*
 * Copyright (c) Microsoft Corporation
 * <p/>
 * All rights reserved.
 * <p/>
 * MIT License
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 */

package com.microsoft.azure.hdinsight.spark.ui;

import com.intellij.execution.configurations.ConfigurationUtil;
import com.intellij.execution.util.JreVersionDetector;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.LabeledComponent;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.packaging.impl.elements.ManifestFileUtil;
import com.intellij.psi.JavaCodeFragment;
import com.intellij.psi.PsiClass;
import com.intellij.psi.util.PsiMethodUtil;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.EditorTextFieldWithBrowseButton;
import com.intellij.ui.MacroAwareTextBrowseFolderListener;
import com.intellij.util.ui.UIUtil;
import com.microsoft.azure.hdinsight.spark.common.SparkLocalRunConfigurableModel;
import org.apache.commons.lang3.SystemUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.io.File;
import java.nio.file.Paths;
import java.util.Optional;

public class SparkLocalRunConfigurable {
    public final static String HADOOP_HOME_ENV = "HADOOP_HOME";
    public final static String WINUTILS_EXE_NAME = "winutils.exe";

    private JPanel myWholePanel;
    private SparkLocalRunCommonParametersPanel myCommonProgramParameters;
    private LabeledComponent<EditorTextFieldWithBrowseButton> myMainClass;
    private JCheckBox myParallelExecutionCheckbox;
    private TextFieldWithBrowseButton myWinutilsPathTextFieldWithBrowserButton;
    private TextFieldWithBrowseButton myDataRootDirectoryFieldWithBrowseButton;
    private JLabel myDataDefaultDirectory;
    private JLabel myHadoopUserDefaultDirectoryLabel;
    private JPanel myWinutilsLocationPanel;

    @Nullable
    private JComponent myAnchor;
    @NotNull
    private final JreVersionDetector myVersionDetector;
    @NotNull
    private final Project myProject;

    public SparkLocalRunConfigurable(@NotNull final Project project) {
        this.myProject = project;
        myVersionDetector = new JreVersionDetector();

        myAnchor = UIUtil.mergeComponentsWithAnchor(myMainClass, myCommonProgramParameters);

        myCommonProgramParameters.setModuleContext(ModuleManager.getInstance(project).findModuleByName(project.getName()));
        // Connect the workingDirectory update event with dataRootDirectory update
        myCommonProgramParameters.addWorkingDirectoryUpdateListener(new DocumentAdapter() {
            @Override
            protected void textChanged(DocumentEvent event) {
                String workingDirectory = myCommonProgramParameters.getWorkingDirectory();

                myDataRootDirectoryFieldWithBrowseButton.setText(Paths.get(workingDirectory, "data").toString());
            }
        });

        // Update other data directory texts
        myDataRootDirectoryFieldWithBrowseButton.getTextField().getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(DocumentEvent documentEvent) {
                myDataDefaultDirectory.setText(
                        Paths.get(myDataRootDirectoryFieldWithBrowseButton.getText(), "__default__").toString());
                myHadoopUserDefaultDirectoryLabel.setText(
                        Paths.get(myDataDefaultDirectory.getText(), "user", "current").toString());
            }
        });

        // Bind the folder file chooser for data root directory
        FileChooserDescriptor dataRootDirectoryChooser = FileChooserDescriptorFactory.createSingleFolderDescriptor();
        myDataRootDirectoryFieldWithBrowseButton.addBrowseFolderListener(
                new MacroAwareTextBrowseFolderListener(dataRootDirectoryChooser, myProject));

        // Winutils.exe setting, only for windows
        if (SystemUtils.IS_OS_WINDOWS) {
            updateWinUtilsPathTextField(System.getenv(HADOOP_HOME_ENV));
        } else {
            myWinutilsLocationPanel.setVisible(false);
        }
    }

    private void updateWinUtilsPathTextField(@Nullable String hadoopHomeEnv) {
        String windUtilsPath = Optional.ofNullable(hadoopHomeEnv)
                .map(hadoopHome -> Paths.get(hadoopHome, "bin", WINUTILS_EXE_NAME).toString())
                .map(File::new)
                .filter(File::exists)
                .map(File::toString)
                .orElse("");

        myWinutilsPathTextFieldWithBrowserButton.setText(windUtilsPath);

        // Bind winutils.exe file chooser
        FileChooserDescriptor winUtilsFileChooser =
                new FileChooserDescriptor(true, false, false, false, false, false)
                        .withFileFilter(file -> file.getName().equals(WINUTILS_EXE_NAME) && file.getParent().getName().equals("bin"));

        myWinutilsPathTextFieldWithBrowserButton.addBrowseFolderListener(
                new MacroAwareTextBrowseFolderListener(winUtilsFileChooser, myProject));

    }

    @NotNull
    public EditorTextFieldWithBrowseButton getMainClassField() {
        return myMainClass.getComponent();
    }

    private void createUIComponents() {
        myMainClass = new LabeledComponent<>();
        myMainClass.setComponent(new EditorTextFieldWithBrowseButton(myProject, true, (declaration, place) -> {
            if (declaration instanceof PsiClass) {
                final PsiClass aClass = (PsiClass)declaration;
                if (ConfigurationUtil.MAIN_CLASS.value(aClass) && PsiMethodUtil.findMainMethod(aClass) != null ||
                        place.getParent() != null) {
                    return JavaCodeFragment.VisibilityChecker.Visibility.VISIBLE;
                }
            }
            return JavaCodeFragment.VisibilityChecker.Visibility.NOT_VISIBLE;
        }));

        myMainClass.getComponent().getButton().addActionListener( e -> {
            PsiClass selected = ManifestFileUtil.selectMainClass(myProject, myMainClass.getComponent().getText());
            if (selected != null) {
                myMainClass.getComponent().setText(selected.getQualifiedName());
            }
        });
    }

    public void setData(@NotNull SparkLocalRunConfigurableModel data) {
        // Data -> Component
        myParallelExecutionCheckbox.setSelected(data.isIsParallelExecution());
        myMainClass.getComponent().setText(data.getRunClass());
        myCommonProgramParameters.reset(data);

        if (!data.getDataRootDirectory().trim().isEmpty()) {
            myDataRootDirectoryFieldWithBrowseButton.setText(data.getDataRootDirectory());
        }

        Optional.ofNullable(myCommonProgramParameters.getEnvs().get(HADOOP_HOME_ENV))
                .ifPresent(this::updateWinUtilsPathTextField);
    }

    public void getData(@NotNull SparkLocalRunConfigurableModel data) {
        // Component -> Data
        data.setIsParallelExecution(myParallelExecutionCheckbox.isSelected());
        data.setRunClass(myMainClass.getComponent().getText());
        myCommonProgramParameters.applyTo(data);
        data.setDataRootDirectory(myDataRootDirectoryFieldWithBrowseButton.getText());

        Optional.of(myWinutilsPathTextFieldWithBrowserButton.getText())
                .map((winUtilsFilePath) -> Paths.get(winUtilsFilePath))
                .filter(path -> path.endsWith(Paths.get("bin", WINUTILS_EXE_NAME)))
                .map(path -> path.getParent().getParent().toString())
                .ifPresent(hadoopHome -> data.getEnvs().put(HADOOP_HOME_ENV, hadoopHome));
    }
}
