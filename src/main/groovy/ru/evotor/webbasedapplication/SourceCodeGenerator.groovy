package ru.evotor.webbasedapplication

import groovy.xml.MarkupBuilder
import groovy.xml.XmlUtil
import org.gradle.util.GFileUtils

class SourceCodeGenerator {
    private String packageName
    private String packageSourceFilesPath
    private String outputDir
    private String assetsFolder

    public SourceCodeGenerator(String packageName, String outputDir) {
        this.packageName = packageName
        String[] packageDir = packageName.toString().split("\\.")
        StringBuilder packageFilesPathBuilder = new StringBuilder()
        packageDir.each { part -> packageFilesPathBuilder.append(part).append("/") }
        this.packageSourceFilesPath = packageFilesPathBuilder.toString()
        this.outputDir = outputDir
        this.assetsFolder = outputDir + "/assets/"
    }


    public void generateActivitySourceCode(Object view) {
        def className = view.name.toString().toUpperCase()
        StringBuilder stringBuilder = new StringBuilder()
        stringBuilder.append("package " + packageName + ";\n")
                .append("import ru.evotor.webtemplatelibriary.LauncherActivity;\n")
                .append("public class ").append(className).append(" extends LauncherActivity {}")

        GFileUtils.writeFile(stringBuilder.toString(), new File(outputDir
                + "/java/" + packageSourceFilesPath + className + ".java"))
    }

    public void generateColors(StringWriter stringWriter, Object view) {
        GFileUtils.writeFile(stringWriter.toString(), new File(outputDir.toString()
                + "/res/values/" + view.name + "_colors.xml"))
    }

    public void generatePluginAndDaemon() {
        def daemonServiceFilePath = outputDir + "/java/" + packageSourceFilesPath + "DaemonServiceImplementation.java"
        def uiPluginFilePath = outputDir + "/java/" + packageSourceFilesPath + "UIPluginServiceImplementation.java"
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
    }

    public void generateStringResources(String appUUID, String appName) {
        def stringResourcesWriter = new StringWriter()
        def stringResourcesMarkupBuilder = new MarkupBuilder(stringResourcesWriter)
        stringResourcesMarkupBuilder.resources {
            "string"(appUUID, "name": "app_uuid")
            "string"(appName, "name": "generated_app_name")
        }
        GFileUtils.writeFile(stringResourcesWriter.toString(), new File(outputDir.toString() + "/res/values/strings_generated.xml"))
        new File(outputDir.toString() + "/res/values/strings.xml").delete()
    }

    public void replaceManifestWithGenerated(Node manifestXml) {
        File originalManifest = new File(outputDir.toString() + '/AndroidManifest.xml')
        File manifestFileXML = new File(outputDir.toString() + '/AndroidManifest_Generated.xml')
        manifestFileXML.delete()
        manifestFileXML << XmlUtil.serialize(manifestXml)
        originalManifest.delete()
        GFileUtils.copyFile(new File(outputDir.toString() + '/AndroidManifest_Generated.xml'),
                new File(outputDir.toString() + '/AndroidManifest.xml'))
    }

    public void copyIconsForView(Object view, String icon96Path, String icon192Path, String icon256Path) {
        GFileUtils.copyFile(new File(assetsFolder + icon96Path),
                new File(outputDir.toString() + "/res/mipmap-hdpi/" + view.name + "_icon.png"))
        GFileUtils.copyFile(new File(assetsFolder + icon192Path),
                new File(outputDir.toString() + "/res/mipmap-xxhdpi/" + view.name + "_icon.png"))
        GFileUtils.copyFile(new File(assetsFolder + icon256Path),
                new File(outputDir.toString() + "/res/mipmap-xxxhdpi/" + view.name + "_icon.png"))
    }

    public void generateServiceForViewSalesScreen(Object view) {
        def className = view.name.toString().toUpperCase()
        def filePath = outputDir + "/java/" + packageSourceFilesPath + className + "_SERVICE.java"
        if (!new File(filePath).exists()) {
            StringBuilder stringBuilder = new StringBuilder()
            stringBuilder.append("package " + packageName + ";\n")
                    .append("import android.content.Intent;; \n")
                    .append("import android.os.Bundle; \n")
                    .append("import android.os.RemoteException; \n")
                    .append("import java.util.HashMap; \n")
                    .append("import java.util.Map; \n")
                    .append("import ru.evotor.framework.core.IntegrationService; \n")
                    .append("import ru.evotor.framework.core.action.processor.ActionProcessor; \n")
                    .append("public class " + className + "_SERVICE extends IntegrationService {\n")
                    .append("protected Map<String, ActionProcessor> createProcessors() { \n")
                    .append("Map<String, ActionProcessor> processorsMap = new HashMap<>(); \n")
                    .append("processorsMap.put(\"evo.v2.receipt.sell.receiptDiscount\", new ActionProcessor() {\n")
                    .append("@Override \n").append("public void process(String s, Bundle bundle, Callback callback) {\n")
                    .append("try {\ncallback.startActivity(new Intent(getApplicationContext(), " + view.name.toString().toUpperCase() + ".class));\n}\n")
                    .append("catch (RemoteException e) {\ne.printStackTrace();\n}\n").append("}\n});\n return processorsMap;\n}\n}\n")
            GFileUtils.writeFile(stringBuilder.toString(), new File(filePath))
        }
    }
}