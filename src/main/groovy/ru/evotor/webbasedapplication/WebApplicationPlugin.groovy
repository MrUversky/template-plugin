package ru.evotor.webbasedapplication

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.rauschig.jarchivelib.ArchiveFormat
import org.rauschig.jarchivelib.ArchiverFactory

import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

class WebApplicationPlugin implements Plugin<Project> {
    private final def ANDROID_PLUGIN_NAME = "com.android.application"

    private final def TEMPLATE_ASSETS = "template.zip"
    private final def TEMPLATE_AAR = "template.aar"
    private final
    def TEMPLATE_ASSETS_URL = "https://market.evotor.ru/static/webapptemplate/2_0_0/" + TEMPLATE_ASSETS
    private final
    def TEMPLATE_AAR_URL = "https://market.evotor.ru/static/webapptemplate/" + TEMPLATE_AAR

    def sourcesDirectoryPath = "src/main/assets/"
    def yamlFileName = "client.yaml"

    @Override
    void apply(Project project) {
        if (!project.pluginManager.hasPlugin(ANDROID_PLUGIN_NAME)) {
            throw new IllegalStateException("You should also apply '$ANDROID_PLUGIN_NAME' plugin")
        }

        def path = new File(project.projectDir, sourcesDirectoryPath)

        println("Validating template...")
        validateAndDownloadTemplateByType(path, TemplateType.ASSETS)
        try {
            project.tasks.create("generateManifestTask", ManifestTask.class).execute()
        }
        catch (Exception e) {
            e.printStackTrace()
        }

        validateAndDownloadTemplateByType(new File(project.projectDir, "libs"), TemplateType.AAR)

        project.with {
            android {
                packagingOptions {
                    exclude 'META-INF/ASL2.0'
                    exclude 'META-INF/LICENSE'
                    exclude 'META-INF/NOTICE'
                    exclude 'META-INF/LICENSE.txt'
                    exclude 'META-INF/NOTICE.txt'
                    exclude 'META-INF/DEPENDENCIES'
                    exclude 'META-INF/app_release.kotlin_module'
                }

                splits {
                    abi {
                        enable true
                        reset()
                        include 'armeabi-v7a'
                        universalApk false
                    }
                }

                lintOptions {
                    abortOnError false
                }

            }

            repositories {
                jcenter()
                mavenCentral()
                maven { url "https://jitpack.io" }
                flatDir {
                    dirs 'libs'
                }
            }

            dependencies {
                compile(name: 'template', ext: 'aar')

                compile 'com.github.evotor:integration-library:v0.3.1'
                // RxJava and RxAndroid
                compile 'io.reactivex:rxandroid:1.2.0'
                compile 'io.reactivex:rxjava:1.1.5'
                compile 'com.google.dagger:dagger:2.0.2'
                provided 'org.glassfish:javax.annotation:10.0-b28'

                //Android JSCore
                compile 'com.github.ericwlange:AndroidJSCore:3.0.1'

                // YAML parsing
                compile 'org.yaml:snakeyaml:1.17'

                // OkHttp and logging interceptor
                compile 'com.squareup.okhttp3:okhttp:3.6.0'
                compile 'com.squareup.okhttp3:logging-interceptor:3.6.0'

                // Retrofit
                compile 'com.squareup.retrofit2:retrofit:2.2.0'

                // Jackson and Retrofit converter and adapter
                compile 'com.squareup.retrofit2:adapter-rxjava:2.2.0'
                compile 'com.squareup.retrofit2:converter-jackson:2.2.0'
                compile 'com.fasterxml.jackson.core:jackson-databind:2.8.6'
                compile 'com.fasterxml.jackson.core:jackson-core:2.8.6'
                compile 'com.fasterxml.jackson.core:jackson-annotations:2.8.6'
                compile 'com.fasterxml.jackson.module:jackson-module-kotlin:2.8.7'

            }
        }

    }

    void validateAndDownloadTemplateByType(File path, TemplateType templateType) {
        if (!path.exists()) {
            println("Path ${path.canonicalPath} doesn't exist, creating...")
            path.mkdirs()
        }

        if (templateType == TemplateType.ASSETS && new File(path.toString() + "/" + yamlFileName).exists())
            return
        if (path.list().length >= 1 && path.list().contains(TEMPLATE_ASSETS)) {
            new File(path, TEMPLATE_ASSETS).delete()
        } else if (path.list().length >= 1 && path.list().contains(TEMPLATE_AAR)) {
            new File(path, TEMPLATE_AAR).delete()
        }

        switch (templateType) {
            case TemplateType.ASSETS: downloadTemplate(path); break;
            case TemplateType.AAR: downloadTemplateAar(path); break;
        }

    }

    void downloadTemplate(File path) {
        def zipFile = new File(path, "template.zip")

        downloadToFile(TEMPLATE_ASSETS_URL, zipFile)

        ArchiverFactory.createArchiver(ArchiveFormat.ZIP).extract(zipFile, path)

        zipFile.delete()
    }

    void downloadTemplateAar(File path) {
        def aarFile = new File(path, "template.aar")
        downloadToFile(TEMPLATE_AAR_URL, aarFile)
    }

    void downloadToFile(String url, File file) {
        InputStream inputStream = new URL(url).openStream()
        Files.copy(inputStream, Paths.get(file.toURI()), StandardCopyOption.REPLACE_EXISTING)
    }

}

enum TemplateType {
    ASSETS, AAR
}


