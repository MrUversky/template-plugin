package ru.evotor.webbasedapplication

import org.apache.commons.io.IOUtils
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
import java.util.zip.ZipFile

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
        val archiveFile = ZipFile(zipFile)
        archiveFile.entries().toList().forEach { entry ->
            if (entry.isDirectory) {
                File(path, entry.name).mkdirs()
            } else {
                val outputFile = File(path, entry.name)
                if (!outputFile.parentFile.exists()) {
                    outputFile.parentFile.mkdirs()
                }

                val input = BufferedInputStream(archiveFile.getInputStream(entry))
                val output = BufferedOutputStream(FileOutputStream(outputFile))
                IOUtils.copy(input, output)
                output.close()
                input.close()
            }
        }
        archiveFile.close()
        zipFile.delete()
    }

    private fun downloadToFile(url: String, file: File) {
        URL(url).openStream().use { inputStream ->
            Files.copy(inputStream, Paths.get(file.toURI()), StandardCopyOption.REPLACE_EXISTING)
        }
    }

}
