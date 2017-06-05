package ru.evotor.webbasedapplication

import groovy.xml.MarkupBuilder
import org.gradle.api.Project

class ManifestXmlConfigurator {
    private Node manifestXml
    private ClientYaml clientYaml
    private String packageName
    private Project currentProject
    private SourceCodeGenerator sourceCodeGenerator

    public ManifestXmlConfigurator(Node manifestXML, ClientYaml clientYaml, Project project) {
        this.manifestXml = manifestXML
        this.clientYaml = clientYaml
        this.currentProject = project
    }

    public void processConfiguration() {
        setCorrectApplicationUUID()
        recreateApplicationNode()
        addCapabilities()
        addUIPluginAndDaemonServices()
        addActivities()
        sourceCodeGenerator.generateStringResources(clientYaml.appUuid, clientYaml.appName)
        sourceCodeGenerator.replaceManifestWithGenerated(manifestXml)
    }

    private void setCorrectApplicationUUID() {
        packageName = manifestXml.attributes().get("package")
        sourceCodeGenerator = new SourceCodeGenerator(packageName, currentProject.projectDir.absolutePath + "/src/main")
        removeApplicationUUIDNode()
        addApplicationUUIDNode()
    }

    /*
        Below goes routine methods
     */

    private void addApplicationUUIDNode() {
        manifestXml.application.each {
            applicationNode ->
                Map<String, String> metaDataNode = new HashMap<>()
                metaDataNode.put(Constants.namespace.name, Constants.APPLICATION_UUID)
                metaDataNode.put(Constants.namespace.value, clientYaml.appUuid)
                new Node(applicationNode, "meta-data", metaDataNode)
        }
    }

    private void removeApplicationUUIDNode() {
        NodeList applicationNodes = manifestXml.application
        applicationNodes.each {
            appNode ->
                NodeList metaDataArray = manifestXml.application."meta-data"
                metaDataArray.each {
                    metaData ->
                        if (metaData.attribute(Constants.namespace.name) == Constants.APPLICATION_UUID)
                            appNode.remove(metaData)
                }
        }
    }

    private void recreateApplicationNode() {
        manifestXml.application.each {
            applicationNode ->
                manifestXml.remove(applicationNode)
        }
        new Node(manifestXml, "application", new HashMap() {
            {
                put(Constants.namespace.icon, "@mipmap/ic_launcher")
                put(Constants.namespace.allowBackup, "true")
                put(Constants.namespace.theme, "@style/AppTheme")
            }
        })
    }

    private void addActivities() {
        def viewName = null
        clientYaml.views.each {
            view ->
                if (view.label == null || view.point == null)
                    return
                viewName = view.name
                def integrationPoint = view.point
                def stringWriter = new StringWriter()
                def markupBuilder = new MarkupBuilder(stringWriter)
                def launcherColorName = view.name + "_launcher_color"
                markupBuilder.resources {
                    "color"("name": launcherColorName, view.color)
                }
                Node activity
                manifestXml.application.each { applicationNode ->
                    activity = new Node(applicationNode, "activity", new HashMap() {
                        {
                            put(Constants.namespace.name, "." + view.name.toString().toUpperCase())
                        }
                    })
                    new Node(activity, "intent-filter", new HashMap() {
                        {
                            put("android:priority", "90")
                        }
                    })
                }

                sourceCodeGenerator.generateColors(stringWriter, view)
                sourceCodeGenerator.generateActivitySourceCode(view)
                sourceCodeGenerator.copyIconsForView(view, view.icon_96, view.icon_192, view.icon_256)

                new Node(activity, "meta-data", new HashMap() {
                    {
                        if (integrationPoint == Constants.INTEGRATION_POINT_SALES_SCREEN)
                            put(Constants.namespace.name, backgroundColorMetaSales)
                        else if (integrationPoint == Constants.INTEGRATION_POINT_MAIN_SCREEN)
                            put(Constants.namespace.name, backgroundColorMetaLauncher)
                        put("android:value", "@color/" + launcherColorName)
                    }
                })

                if (viewName != null) {
                    activity.attributes().put("android:icon", "@mipmap/" + viewName + "_icon")
                    activity.attributes().put("android:label", view.label)
                }
                activity.children().each {
                    childNode ->
                        if (childNode.name().equals("intent-filter")) {
                            new Node(childNode, "action", new HashMap() {
                                {
                                    put(Constants.namespace.name, Constants.ANDROID_ACTION_MAIN)
                                }
                            })
                            if (integrationPoint == Constants.INTEGRATION_POINT_SALES_SCREEN) {
                                new Node(childNode, "category", new HashMap() {
                                    {
                                        put(Constants.namespace.name, Constants.ANDROID_CATEGORY_SALES_SCREEN)
                                    }
                                })
                                new Node(childNode, "category", new HashMap() {
                                    {
                                        put(Constants.namespace.name, Constants.ANDROID_CATEGORY_DEFAULT)
                                    }
                                })
                            } else if (integrationPoint == Constants.INTEGRATION_POINT_MAIN_SCREEN) {
                                new Node(childNode, "category", new HashMap() {
                                    {
                                        put(Constants.namespace.name, Constants.ANDROID_CATEGORY_EVOTOR)
                                    }
                                })
                                new Node(childNode, "category", new HashMap() {
                                    {
                                        put(Constants.namespace.name, Constants.ANDROID_CATEGORY_DEFAULT)
                                    }
                                })
                            }
                        }
                }
        }
    }

    private void addCapabilities() {
        clientYaml.capabilities.each {
            capability ->
                if (capability == "barcode-scanner") {
                    new Node(manifestXml, "uses-permission", new HashMap() {
                        {
                            put(Constants.namespace.name, "ru.evotor.devices.SCANNER_RECEIVER");
                        }
                    })
                } else if (capability == "internet") {
                    new Node(manifestXml, "uses-permission", new HashMap() {
                        {
                            put(Constants.namespace.name, "android.permission.INTERNET");
                        }
                    })
                }
        }
    }

    private void addUIPluginAndDaemonServices() {
        sourceCodeGenerator.generatePluginAndDaemon()
        manifestXml.application.each { appNode ->
            def uiPluginNode = new Node(appNode, "service", new HashMap() {
                {
                    put(Constants.namespace.name, ".UIPluginServiceImplementation")
                    put("android:enabled", "true")
                    put("android:exported", "true")
                }
            })
            def uiPluginIntentFilter = new Node(uiPluginNode, "intent-filter", new HashMap())
            new Node(uiPluginIntentFilter, "action", new HashMap() {
                {
                    put(Constants.namespace.name, Constants.ANDROID_ACTION_MAIN)
                }
            })
            new Node(uiPluginIntentFilter, "category", new HashMap() {
                {
                    put(Constants.namespace.name, Constants.ANDROID_CATEGORY_DEFAULT)
                }
            })
            new Node(uiPluginIntentFilter, "category", new HashMap() {
                {
                    put(Constants.namespace.name, Constants.ANDROID_CATEGORY_EVOTOR)
                }
            })
            def daemonServiceNode = new Node(appNode, "service", new HashMap() {
                {
                    put(Constants.namespace.name, ".DaemonServiceImplementation")
                    put("android:enabled", "true")
                    put("android:exported", "true")
                }
            })
            def daemonIntentFilter = new Node(daemonServiceNode, "intent-filter", new HashMap())
            new Node(daemonIntentFilter, "action", new HashMap() {
                {
                    put(Constants.namespace.name, Constants.ANDROID_ACTION_MAIN)
                }
            })
            new Node(daemonIntentFilter, "category", new HashMap() {
                {
                    put(Constants.namespace.name, Constants.ANDROID_CATEGORY_DEFAULT)
                }
            })
            new Node(daemonIntentFilter, "category", new HashMap() {
                {
                    put(Constants.namespace.name, Constants.ANDROID_CATEGORY_EVOTOR)
                }
            })
        }
        addIntentFiltersToServices()
    }

    private void addIntentFiltersToServices() {
        NodeList services = manifestXml.application.service
        services.each {
            service ->
                if (service.attributes().get(Constants.namespace.name).equals(".UIPluginServiceImplementation")) {
                    clientYaml.plugins.each {
                        plugin ->
                            plugin.moments.each {
                                moment ->
                                    if (service.children().action.findAll {
                                        it.attribute(Constants.namespace.name) == moment
                                    }.size() == 0) {
                                        Map<String, String> attributes = new HashMap<>()
                                        attributes.put(Constants.namespace.name, moment)
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
                            }
                    }
                } else if (service.attributes().get(Constants.namespace.name).equals(".DaemonServiceImplementation")) {
                    clientYaml.daemons.each { daemon ->
                        daemon.events.each { event ->
                            if (service.children().action.findAll {
                                it.attribute(Constants.namespace.name) == event
                            }.size() == 0) {
                                Map<String, String> attributes = new HashMap<>()
                                attributes.put(Constants.namespace.name, event)
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
                        }
                    }
                }
        }
    }

    private boolean containsMetaDataWithAppUUID() {
        manifestXml.application."meta-data".each {
            metaData ->
                if (metaData.attribute(Constants.namespace.name) == "app_uuid")
                    return true
        }
        return false
    }


}