package ru.evotor.webbasedapplication

import groovy.xml.MarkupBuilder
import groovy.xml.Namespace
import groovy.xml.XmlUtil
import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction
import org.gradle.util.GFileUtils
import org.rauschig.jarchivelib.ArchiveFormat
import org.rauschig.jarchivelib.ArchiverFactory
import org.yaml.snakeyaml.Yaml

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
            project.tasks.create("generateManifest", GenerateManifest.class).execute()
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

                compile 'com.github.evotor:integration-library:v0.0.7'
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

class GenerateManifest extends DefaultTask {

    @TaskAction
    public void generate() {

        def outputDir = new File(getProject().projectDir.absolutePath + "/src/main")

        //Parse and load yaml file
        def yaml = new Yaml()
        final def ASSETS_FOLDER = outputDir.toString() + "/assets/";
        def yamlObject = yaml.load(new FileInputStream(new File(ASSETS_FOLDER + "client.yaml")))
        //Get string resource values from yaml file
        def appID = yamlObject.packageName
        def versionNameYaml = yamlObject.versionName
        def versionYaml = yamlObject.version
        def appName = yamlObject.appName
        def appUUID = yamlObject.appUUID
        def label = ""
        def launcher_color = "#000"

        //Defining prefixes and namespace
        def androidNameKey = "android:name"
        def grantsFilePostfix = "_grants"
        def mainActionName = "android.intent.action.MAIN"
        def defaultCategoryName = "android.intent.category.DEFAULT"
        def evotorCategoryName = "android.intent.category.EVOTOR"
        def grantsMetaDataName = "ru.evotor.launcher.GRANTS"
        def launcherIntent = "android.intent.category.EVOTOR"
        def salesScreenIntent = "android.intent.category.SALES_SCREEN"
        def backgroundColorMetaSales = "ru.evotor.sales_screen.BACKGROUND_COLOR"
        def backgroundColorMetaLauncher = "ru.evotor.launcher.BACKGROUND_COLOR"
        def manifestFile = new File(outputDir.toString() + '/AndroidManifest.xml')
        def nameSpace = new Namespace("http://schemas.android.com/apk/res/android", "android")
        def manifestXML = new XmlParser().parseText(manifestFile.text)

        def shouldRebuild = false
        manifestXML.application."meta-data".each { metaData ->
            if (metaData.attribute(nameSpace.name) == "app_uuid")
                shouldRebuild = true
        }
        if (shouldRebuild) {
            for (int i = 0; i < manifestXML.children().size(); i++) {
                manifestXML.children().remove(i)
            }
            new Node(manifestXML, "application", new HashMap() {
                {
                    put("xmlns:android", "http://schemas.android.com/apk/res/android")
                    put("android:icon", "@mipmap/ic_launcher")
                    put("android:allowBackup", "true")
                    put("android:theme", "@style/AppTheme")
                }
            })
        }
        //Adding meta-data to app
        def packageName = manifestXML.attributes().get("package")
        NodeList apps = manifestXML.application
        String[] packageDir = packageName.toString().split("\\.")
        StringBuilder packageFilesPath = new StringBuilder()
        packageDir.each { part -> packageFilesPath.append(part).append("/") }
        def daemonServiceFilePath = outputDir.toString() + "/java/" + packageFilesPath.toString() + "DaemonServiceImplementation.java"
        def uiPluginFilePath = outputDir.toString() + "/java/" + packageFilesPath.toString() + "UIPluginServiceImplementation.java"
        apps.each {
            appNode ->
                NodeList metaDataArray = manifestXML.application."meta-data"
                metaDataArray.each { metaData ->
                    if (metaData.attribute(nameSpace.name) == "app_uuid")
                        appNode.remove(metaData)
                }
                ((Node) appNode).attributes().remove(nameSpace.label)
                ((Node) appNode).attributes().put("xmlns:tools", "http://schemas.android.com/tools")
                ((Node) appNode).attributes().put(nameSpace.label, "@string/generated_app_name")
                ((Node) appNode).attributes().put("tools:replace", "android:label")

                if (!new File(daemonServiceFilePath).exists() && !new File(uiPluginFilePath).exists()) {
                    StringBuilder stringBuilder = new StringBuilder()
                    stringBuilder.append("package " + packageName + ";\n")
                            .append("import ru.evotor.webtemplatelibriary.UiPluginService; \n")
                            .append("public class UIPluginServiceImplementation extends UiPluginService {}")

                    GFileUtils.writeFile(stringBuilder.toString(), new File(uiPluginFilePath))

                    stringBuilder = new StringBuilder()
                    stringBuilder.append("package " + packageName + ";\n")
                            .append("import ru.evotor.webtemplatelibriary.DaemonService; \n")
                            .append("public class DaemonServiceImplementation extends DaemonService {}")

                    GFileUtils.writeFile(stringBuilder.toString(), new File(daemonServiceFilePath))
                }

                Map<String, String> metaData = new HashMap<>()
                metaData.put(androidNameKey, "app_uuid")
                metaData.put("android:value", appUUID)
                new Node(appNode, "meta-data", metaData)
                yamlObject.views.each {
                    view ->
                        if (view.label != null) {
                            Node launcherActivity = new Node(appNode, "activity", new HashMap() {
                                {
                                    put(nameSpace.name, "." + view.name.toString().toUpperCase())
                                }
                            })
                            new Node(launcherActivity, "intent-filter", new HashMap() {
                                {
                                    put("android:priority", "90")
                                }
                            })
                            def className = view.name.toString().toUpperCase()
                            StringBuilder stringBuilder = new StringBuilder()
                            stringBuilder.append("package " + packageName + ";\n")
                                    .append("import ru.evotor.webtemplatelibriary.LauncherActivity;\n")
                                    .append("public class ").append(className).append(" extends LauncherActivity {}")

                            GFileUtils.writeFile(stringBuilder.toString(), new File(outputDir.toString()
                                    + "/java/" + packageFilesPath.toString() + className + ".java"))
                        }
                }
                def uiPluginNode = new Node(appNode, "service", new HashMap() {
                    {
                        put(nameSpace.name, ".UIPluginServiceImplementation")
                        put("android:enabled", "true")
                        put("android:exported", "true")
                    }
                })
                def uiPluginIntentFilter = new Node(uiPluginNode, "intent-filter", new HashMap())
                new Node(uiPluginIntentFilter, "action", new HashMap() {
                    {
                        put(androidNameKey, mainActionName)
                    }
                })
                new Node(uiPluginIntentFilter, "category", new HashMap() {
                    {
                        put(androidNameKey, defaultCategoryName)
                    }
                })
                new Node(uiPluginIntentFilter, "category", new HashMap() {
                    {
                        put(androidNameKey, evotorCategoryName)
                    }
                })
                def daemonServiceNode = new Node(appNode, "service", new HashMap() {
                    {
                        put(nameSpace.name, ".DaemonServiceImplementation")
                        put("android:enabled", "true")
                        put("android:exported", "true")
                    }
                })
                def daemonIntentFilter = new Node(daemonServiceNode, "intent-filter", new HashMap())
                new Node(daemonIntentFilter, "action", new HashMap() {
                    {
                        put(androidNameKey, mainActionName)
                    }
                })
                new Node(daemonIntentFilter, "category", new HashMap() {
                    {
                        put(androidNameKey, defaultCategoryName)
                    }
                })
                new Node(daemonIntentFilter, "category", new HashMap() {
                    {
                        put(androidNameKey, evotorCategoryName)
                    }
                })
        }

        //Adding intent-filters to manifest
        NodeList services = manifestXML.application.service
        services.each {
            service ->
                if (service.attributes().get(nameSpace.name).equals(".UiPluginService")) {
                    yamlObject.plugins.each {
                        plugin ->
                            plugin.moments.each {
                                moment ->
                                    if (service.children().action.findAll {
                                        it.attribute(nameSpace.name) == moment
                                    }.size() == 0) {
                                        Map<String, String> attributes = new HashMap<>()
                                        attributes.put(androidNameKey, moment)
                                        service."intent-filter".each { intentFilterNode -> new Node(intentFilterNode, "action", attributes) }
                                    }
                            }
                            if (plugin.grants != null) {
                                def stringWriter = new StringWriter()
                                def markupBuilder = new MarkupBuilder(stringWriter)
                                markupBuilder.resources {
                                    "string-array"("name": plugin.name + grantsFilePostfix) {
                                        plugin.grants.each {
                                            permission ->
                                                item(permission)
                                        }
                                    }
                                }

                                GFileUtils.writeFile(stringWriter.toString(), new File(outputDir.toString()
                                        + "/res/values/" + plugin.name + grantsFilePostfix + ".xml"))
                                new Node(service, "meta-data", new HashMap() {
                                    {
                                        put(androidNameKey, grantsMetaDataName)
                                        put("android:resource", "@array/" + plugin.name + grantsFilePostfix)
                                    }
                                })
                            }
                    }
                } else if (service.attributes().get(nameSpace.name).equals(".DaemonService")) {
                    yamlObject.daemons.each { daemon ->
                        daemon.events.each { event ->
                            if (service.children().action.findAll {
                                it.attribute(nameSpace.name) == event
                            }.size() == 0) {
                                Map<String, String> attributes = new HashMap<>()
                                attributes.put(androidNameKey, event)
                                service."intent-filter".each { intentFilterNode -> new Node(intentFilterNode, "action", attributes) }
                            }
                        }

                        if (daemon.grants != null) {
                            def stringWriter = new StringWriter()
                            def markupBuilder = new MarkupBuilder(stringWriter)
                            markupBuilder.resources {
                                "string-array"("name": daemon.name + grantsFilePostfix) {
                                    daemon.grants.each {
                                        permission ->
                                            item(permission)
                                    }
                                }
                            }

                            GFileUtils.writeFile(stringWriter.toString(), new File(outputDir.toString()
                                    + "/res/values/" + daemon.name + grantsFilePostfix + ".xml"))
                            new Node(service, "meta-data", new HashMap() {
                                {
                                    put(androidNameKey, grantsMetaDataName)
                                    put("android:resource", "@array/" + daemon.name + grantsFilePostfix)
                                }
                            })
                        }
                    }
                }
        }

        //Applying icons and launcher properties
        def viewName = null
        def isIntegrationPoint = false
        def icon96Path
        def icon192Path
        def icon256Path

        NodeList activities = manifestXML.application.activity
        yamlObject.views.each {
            view ->
                if (view.label != null) {
                    label = view.label
                    viewName = view.name
                    icon96Path = view.icon_96
                    icon192Path = view.icon_192
                    icon256Path = view.icon_256
                    if (view.color != null)
                        launcher_color = view.color
                    isIntegrationPoint = true
                    def integrationPoint = view.point

                    //Add icon and intent-filters to launcher activity if it's placed in yaml
                    activities.each {
                        activity ->
                            if (activity.attributes().get(nameSpace.name).equals("." + view.name.toString().toUpperCase())) {
                                def stringWriter = new StringWriter()
                                def markupBuilder = new MarkupBuilder(stringWriter)
                                def launcherColorName = view.name + "_launcher_color"
                                markupBuilder.resources {
                                    "color"("name": launcherColorName, view.color)
                                }

                                GFileUtils.writeFile(stringWriter.toString(), new File(outputDir.toString()
                                        + "/res/values/" + view.name + "_colors.xml"))

                                new Node(activity, "meta-data", new HashMap() {
                                    {
                                        if (integrationPoint == "SALES_SCREEN")
                                            put(nameSpace.name, backgroundColorMetaSales)
                                        else if (integrationPoint == "MAIN_SCREEN")
                                            put(nameSpace.name, backgroundColorMetaLauncher)
                                        put("android:value", "@color/" + launcherColorName)
                                    }
                                })
                                if (viewName != null) {
                                    activity.attributes().put("android:icon", "@mipmap/" + viewName + "_icon")
                                    activity.attributes().put("android:label", view.label)
                                }
                                if (isIntegrationPoint) {
                                    activity.children().each {
                                        childNode ->
                                            if (childNode.name().equals("intent-filter")) {
                                                new Node(childNode, "action", new HashMap() {
                                                    {
                                                        put(androidNameKey, mainActionName)
                                                    }
                                                })
                                                new Node(childNode, "category", new HashMap() {
                                                    {
                                                        if (integrationPoint == "SALES_SCREEN")
                                                            put(androidNameKey, salesScreenIntent)
                                                        else if (integrationPoint == "MAIN_SCREEN") {
                                                            put(androidNameKey, launcherIntent)
                                                        }
                                                    }
                                                })
                                            }
                                    }
                                }
                                //Copy icons to /res folder
                                if (isIntegrationPoint) {
                                    GFileUtils.copyFile(new File(ASSETS_FOLDER + icon96Path),
                                            new File(outputDir.toString() + "/res/mipmap-hdpi/" + viewName + "_icon.png"))
                                    GFileUtils.copyFile(new File(ASSETS_FOLDER + icon192Path),
                                            new File(outputDir.toString() + "/res/mipmap-xxhdpi/" + viewName + "_icon.png"))
                                    GFileUtils.copyFile(new File(ASSETS_FOLDER + icon256Path),
                                            new File(outputDir.toString() + "/res/mipmap-xxxhdpi/" + viewName + "_icon.png"))
                                }

                                if (view.grants != null) {
                                    def viewStringWriter = new StringWriter()
                                    def viewMarkupBuilder = new MarkupBuilder(viewStringWriter)
                                    viewMarkupBuilder.resources {
                                        "string-array"("name": view.name + grantsFilePostfix) {
                                            view.grants.each {
                                                permission ->
                                                    item(permission)
                                            }
                                        }
                                    }

                                    GFileUtils.writeFile(viewStringWriter.toString(), new File(outputDir.toString()
                                            + "/res/values/" + view.name + grantsFilePostfix + ".xml"))
                                    new Node(activity, "meta-data", new HashMap() {
                                        {
                                            put(androidNameKey, grantsMetaDataName)
                                            put("android:resource", "@array/" + view.name + grantsFilePostfix)
                                        }
                                    })
                                }
                            }
                    }
                }
        }

        //Replace application manifest by generated one
        File originalManifest = new File(outputDir.toString() + '/AndroidManifest.xml')
        File manifestFileXML = new File(outputDir.toString() + '/AndroidManifest_Generated.xml')
        manifestFileXML.delete()
        manifestFileXML << XmlUtil.serialize(manifestXML)
        originalManifest.delete()
        GFileUtils.copyFile(new File(outputDir.toString() + '/AndroidManifest_Generated.xml'),
                new File(outputDir.toString() + '/AndroidManifest.xml'))

        //Generate resources
        def stringResourcesWriter = new StringWriter()
        def stringResourcesMarkupBuilder = new MarkupBuilder(stringResourcesWriter)
        stringResourcesMarkupBuilder.resources {
            "string"(appUUID, "name": "app_uuid")
            "string"(appName, "name": "generated_app_name")
        }
        GFileUtils.writeFile(stringResourcesWriter.toString(), new File(outputDir.toString() + "/res/values/strings_generated.xml"))
        new File(outputDir.toString() + "/res/values/strings.xml").delete()

        getProject().with {
            android {
                defaultConfig {
                    versionCode versionYaml
                    versionName versionNameYaml
                }
            }
        }
    }
}