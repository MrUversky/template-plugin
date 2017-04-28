package ru.evotor.webbasedapplication

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.yaml.snakeyaml.Yaml
import java.io.*
import java.lang.IllegalArgumentException
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

/**
 * Created by nixan on 28.04.17.
 */

private const val ANDROID_PLUGIN_NAME = "com.android.application"

private const val TEMPLATE_URL = "https://market.evotor.ru/static/webapptemplate/2_0_0/"

class WebApplicationPlugin : Plugin<Project> {

    val sourcesDirectoryPath = "src/main/assets/"
    val yamlFileName = "client.yaml"

    override fun apply(project: Project) {
        if (!project.pluginManager.hasPlugin(ANDROID_PLUGIN_NAME)) {
            throw IllegalStateException("You should also apply '$ANDROID_PLUGIN_NAME' plugin")
        }

        val path = File(project.projectDir, sourcesDirectoryPath)

        println("Validating template...")
        validateTemplateLayout(path)

    }

    private fun validateTemplateLayout(path: File) {
        if (!path.exists()) {
            println("Path ${path.canonicalPath} doesn't exist, creating...")
            createTemplateFolder(folderPath = path)
        } else {
            println("Path ${path.canonicalPath} exists...")
        }

        val manifestFile = File(path, yamlFileName)
        if (!manifestFile.exists()) {
            println("Downloading manifest example...")
            createTemplateManifest(manifestPath = manifestFile)
        }

        validateTemplateManifest(manifestPath = manifestFile)
    }

    private fun createTemplateFolder(folderPath: File) {
        folderPath.mkdirs()
    }

    private fun createTemplateManifest(manifestPath: File) {
        downloadToFile("$TEMPLATE_URL${manifestPath.name}", manifestPath)
    }

    private fun validateTemplateManifest(manifestPath: File) {
        val manifestYaml = Yaml().load(FileInputStream(manifestPath)).let {
            if (it is Map<*, *>) {
                it as Map<String, *>
            } else {
                throw IllegalArgumentException("Corrupt manifest file: ${manifestPath.name}")
            }
        }
        manifestYaml[""]
    }

    private fun downloadToFile(url: String, file: File) {
        URL(url).openStream().use { inputStream ->
            Files.copy(inputStream, Paths.get(file.toURI()), StandardCopyOption.REPLACE_EXISTING)
        }
    }

}
