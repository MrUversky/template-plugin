package ru.evotor.webbasedapplication

import org.gradle.api.Project
import org.yaml.snakeyaml.Yaml

class ProjectFileManager {
    private String ASSETS_FOLDER
    private Project currentProject
    private Object clientYaml
    private Node manifestXML

    public ProjectFileManager(Project project) {
        this.currentProject = project
    }

    public void loadProjectFiles() {
        def outputDir = new File(currentProject.projectDir.absolutePath + "/src/main")
        ASSETS_FOLDER = outputDir.toString() + "/assets/";
        clientYaml = new Yaml().load(new FileInputStream(new File(ASSETS_FOLDER + "client.yaml")))
        def manifestFile = new File(outputDir.toString() + '/AndroidManifest.xml')
        manifestXML = new XmlParser().parseText(manifestFile.text)
    }

    public Object getClientYamlObject() {
        return clientYaml
    }

    public Node getManifestXML() {
        return manifestXML
    }
}