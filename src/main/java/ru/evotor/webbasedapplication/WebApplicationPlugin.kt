package ru.evotor.webbasedapplication

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.rauschig.jarchivelib.ArchiveFormat
import org.rauschig.jarchivelib.ArchiverFactory
import java.net.URL
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.io.File


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

        ArchiverFactory.createArchiver(ArchiveFormat.ZIP).extract(zipFile, path)

        zipFile.delete()
    }

    private fun downloadToFile(url: String, file: File) {
        URL(url).openStream().use { inputStream ->
            Files.copy(inputStream, Paths.get(file.toURI()), StandardCopyOption.REPLACE_EXISTING)
        }
    }

}
