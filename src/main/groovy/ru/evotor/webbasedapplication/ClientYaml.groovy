package ru.evotor.webbasedapplication

class ClientYaml {
    def appUuid
    def capabilities
    def daemons
    def plugins
    def views
    def packageName
    def appName

    public ClientYaml(Object clientYaml) {
        this.appUuid = clientYaml.appUUID
        this.capabilities = clientYaml.capabilities
        this.daemons = clientYaml.daemons
        this.plugins = clientYaml.plugins
        this.views = clientYaml.views
        this.packageName = clientYaml.packageName
        this.appName = clientYaml.appName
    }
}