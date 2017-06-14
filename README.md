[![Join the chat at https://gitter.im/evotor/template-plugin](https://badges.gitter.im/evotor/template-plugin.svg)](https://gitter.im/evotor/template-plugin?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
[![](https://jitpack.io/v/evotor/template-plugin.svg)](https://jitpack.io/#evotor/template-plugin)

В файле `build.gradle` нового проекта, созданного в Android Studio укажите следующие зависимости:

```
allprojects {
    repositories {
      ...
      maven { url 'https://jitpack.io' }
    }
  }
  dependencies {
          compile 'com.github.evotor:template-plugin:v1.1.3'
  }
```

В зависимости `buildscript`'а добавьте:

```
classpath group: 'com.github.evotor', name: 'template-plugin', version: 'v1.1.3'
```

В файле `build.gradle` приложения укажите:

```
apply plugin: 'ru.evotor.webbasedapplication'
```
