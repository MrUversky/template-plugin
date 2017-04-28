package ru.evotor.webbasedapplication

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Created by nixan on 28.04.17.
 */

private const val ANDROID_PLUGIN_NAME = "com.android.application"

class WebApplicationPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        if (!project.pluginManager.hasPlugin(ANDROID_PLUGIN_NAME)) {
            throw IllegalStateException("You should also apply '$ANDROID_PLUGIN_NAME' plugin")
        }
    }

}
