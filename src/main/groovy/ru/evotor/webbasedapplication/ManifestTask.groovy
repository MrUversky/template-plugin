package ru.evotor.webbasedapplication

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

class ManifestTask extends DefaultTask {
    @TaskAction
    public void generate() {
        ProjectFileManager projectFileManager = new ProjectFileManager(project)
        projectFileManager.loadProjectFiles()
        ClientYaml clientYaml = new ClientYaml(projectFileManager.clientYamlObject)
        ManifestXmlConfigurator xmlConfigurator =
                new ManifestXmlConfigurator(projectFileManager.manifestXML, clientYaml, project)
        xmlConfigurator.processConfiguration()
    }
}