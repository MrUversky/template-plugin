package ru.evotor.webbasedapplication

import groovy.lang.Closure
import groovy.util.Node
import groovy.util.NodeList
import groovy.util.XmlParser
import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.publish.maven.MavenArtifact
import org.gradle.api.tasks.TaskAction
import org.rauschig.jarchivelib.ArchiveFormat
import org.rauschig.jarchivelib.ArchiverFactory
import org.yaml.snakeyaml.Yaml
import java.net.URL
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.io.File
import java.io.FileInputStream
import javax.xml.parsers.DocumentBuilderFactory


/**
 * Created by nixan on 28.04.17.
 */

private const val ANDROID_PLUGIN_NAME = "com.android.application"

private const val TEMPLATE_URL = "https://market.evotor.ru/static/webapptemplate/2_0_0/template.zip"

class WebApplicationPlugin : Plugin<Project> {

    val sourcesDirectoryPath = "src/main/assets/"
    val yamlFileName = "client.yaml"

    override fun apply(project: Project) {
        if (!project.pluginManager.hasPlugin(ANDROID_PLUGIN_NAME)) {
            throw IllegalStateException("You should also apply '$ANDROID_PLUGIN_NAME' plugin")
        }

        project.tasks.create("parseYaml", ManifestGenerationTask::class.java)

        TODO("project.task(\"preBuild\").dependsOn.")

        val path = File(project.projectDir, sourcesDirectoryPath)

        println("Validating template...")
        validateTemplateLayout(path)

    }

    private fun validateTemplateLayout(path: File) {
        if (!path.exists()) {
            println("Path ${path.canonicalPath} doesn't exist, creating...")
            path.mkdirs()
        }

        if (path.list().size == 1 && path.list().contains("template.zip")) {
            File(path, "template.zip").delete()
        }

        if (path.list().isEmpty()) {
            downloadTemplate(path)
        }
    }

    private fun downloadTemplate(path: File) {
        val zipFile = File(path, "template.zip")

        downloadToFile(TEMPLATE_URL, zipFile)

        ArchiverFactory.createArchiver(ArchiveFormat.ZIP).extract(zipFile, path)

        zipFile.delete()
    }

    private fun downloadToFile(url: String, file: File) {
        URL(url).openStream().use { inputStream ->
            Files.copy(inputStream, Paths.get(file.toURI()), StandardCopyOption.REPLACE_EXISTING)
        }
    }

}

class ManifestGenerationTask : DefaultTask() {

    @TaskAction
    fun generateManifest() {

        val manifestYaml = Yaml().load(FileInputStream(File(project.projectDir, "src/main/assets/client.yaml"))) as Map<*, *>

        val applicationPackage = manifestYaml["packageName"] as String
        val versionName = manifestYaml["versionName"] as String
        val versionCode = manifestYaml["version"] as Int
        val applicationName = manifestYaml["appName"] as String
        val applicationUuid = manifestYaml["appUUID"] as String
    }

    private fun generateAndroidManifest(applicationUuid: String, yaml: Map<*, *>) {
        val manifestFile = File(project.projectDir, "src/main/AndroidManifest_Original.xml")
        val manifestXml = XmlParser().parseText(manifestFile.readText())
        (manifestXml["application"] as NodeList).map { it as Node }.forEach { appNode ->
            (appNode["meta-data"] as NodeList).map { it as Node }.forEach { metaDataNode ->
                if (metaDataNode.attribute("android:name") == "app_uuid") {
                    appNode.remove(metaDataNode)
                }
            }
            val appUuidMetadata = HashMap<String, String>().apply {
                put("android:name", "app_uuid")
                put("android:value", applicationUuid)
            }
            Node(appNode, "meta-data", appUuidMetadata)
            (yaml["views"] as Map<*, *>)
                    .map { it as Map<*, *> }
                    .filter { it.contains("label") }
                    .forEach { view ->
                        val activityNode = Node(appNode, "activity", HashMap<String, String>().apply {
                            put("android:name", view["name"].toString().toUpperCase())
                            put("android:theme", "@style/WebApplicationTheme")
                        })
                        

                    }
        }
    }
}
