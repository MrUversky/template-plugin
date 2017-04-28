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
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import java.io.IOException
import java.io.File
import java.util.zip.ZipInputStream


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
        unzip(zipFile, path)
        zipFile.delete()
    }

    private fun unzip(zipFile: File, outputPath: File) {
        // buffer for read and write data to file
        val buffer = ByteArray(1024)
        var len: Int
        val zis = ZipInputStream(FileInputStream(zipFile))
        var ze: ZipEntry? = zis.nextEntry
        while (ze != null) {
            val fileName = ze.name
            val newFile = File(outputPath.toString() + File.separator + fileName)
            println("Unzipping to " + newFile.absolutePath)
            // create directories for sub directories in zip
            File(newFile.parent).mkdirs()
            val fos = FileOutputStream(newFile)
            IOUtils.copy(zis, fos)
            fos.close()
            zis.closeEntry()
            ze = zis.nextEntry
        }
        // close last ZipEntry
        zis.closeEntry()
        zis.close()

        println("Done!!!")
    }

    private fun downloadToFile(url: String, file: File) {
        URL(url).openStream().use { inputStream ->
            Files.copy(inputStream, Paths.get(file.toURI()), StandardCopyOption.REPLACE_EXISTING)
        }
    }

}
