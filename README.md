<div align="center">

# squaremap

[![License](https://img.shields.io/badge/license-MIT-blue)](LICENSE)
[![GitHub Workflow Status](https://img.shields.io/github/workflow/status/jpenilla/squaremap/Build)](https://github.com/jpenilla/squaremap/actions)
[![Discord](https://img.shields.io/discord/390942438061113344?color=8C9CFE&label=discord&logo=discord&logoColor=white)](https://discord.gg/PHpuzZS)

squaremap (formerly known as Pl3xMap) is a minimalistic and lightweight live world map viewer for Minecraft servers.

</div>

## What is squaremap

If, like me, you have no real need for 3D views, the novelty of Dynmap and Bluemap have worn off, and you're ready for something actually usable for navigation without all the heavy bulk or slow renders then this is the plugin for you.

## Features

* Ultra fast render times. Get your map viewable today, not next week!
* Simple vanilla-like top down 2D view, designed for navigation.
* Player markers showing yaw rotation, health, and armor
* [Easy configuration](https://github.com/jpenilla/squaremap/wiki/Default-config.yml). Even a caveman can do it.
* Up to date Leaflet front-end.
* [Addons and integrations](https://github.com/jpenilla/squaremap/wiki/Addons) for many popular plugins.

## Downloads
Downloads can be obtained from the [releases](https://github.com/jpenilla/squaremap/releases) section.

## License
[![MIT License](https://img.shields.io/badge/license-MIT-blue)](LICENSE)

This project is licensed under the [MIT license](https://github.com/jpenilla/squaremap/blob/master/LICENSE)

Leaflet (the web ui frontend) is licensed under [2-clause BSD License](https://github.com/Leaflet/Leaflet/blob/master/LICENSE)

## bStats

[![bStats Graph Data](https://bstats.org/signatures/bukkit/squaremap.svg)](https://bstats.org/plugin/bukkit/squaremap/13571)

## API

squaremap provides simple APIs to draw markers, shapes, icons, and etc. on rendered maps. Javadocs are hosted on the maven repository alongside the binaries, and should be automatically downloaded by your IDE. 

### Dependency Information
Maven
```xml
<repository>
    <id>squaremap-snapshots</id>
    <url>https://repo.jpenilla.xyz/snapshots/</url>
</repository>
```
```xml
<dependency>
    <groupId>xyz.jpenilla</groupId>
    <artifactId>squaremap-api</artifactId>
    <version>1.1.0-SNAPSHOT</version>
    <scope>provided</scope>
</dependency>
```

Gradle
```kotlin
repositories {
    maven("https://repo.jpenilla.xyz/snapshots/")
}
```
```kotlin
dependencies {
    compileOnly("xyz.jpenilla", "squaremap-api", "1.1.0-SNAPSHOT")
}
```

## Building from source

Build squaremap by invoking the `build` task with Gradle.

```
./gradlew build
```
