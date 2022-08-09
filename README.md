<div align="center">

# squaremap

[![License](https://img.shields.io/badge/license-MIT-blue)](LICENSE)
[![GitHub Workflow Status](https://img.shields.io/github/workflow/status/jpenilla/squaremap/Build)](https://github.com/jpenilla/squaremap/actions)
[![Discord](https://img.shields.io/discord/390942438061113344?color=8C9CFE&label=discord&logo=discord&logoColor=white)](https://discord.gg/PHpuzZS)

</div>

## What is squaremap

squaremap (formerly known as Pl3xMap) is a minimalistic and lightweight live world map viewer for Minecraft servers.

squaremap hooks into your Minecraft server as a plugin or mod on a [supported platform](#supported-platforms), then generates and manages a live map of your server, viewable in any web browser.

## Features

* Ultra fast render times. Get your map rendered today, not next week.
* Simple vanilla-like top down 2D view, designed for navigation.
* Player markers showing yaw rotation, health, and armor.
* Easy to [setup](https://github.com/jpenilla/squaremap/wiki/Installation) and [configure](https://github.com/jpenilla/squaremap/wiki/Default-config.yml).
* Up to date Leaflet front-end.
* [Addons and integrations](https://github.com/jpenilla/squaremap/wiki/Addons) for many popular plugins.

## Supported platforms

- [Paper](https://papermc.io/)
- [Sponge 10](https://www.spongepowered.org/)
- [Fabric](https://fabricmc.net/) (requires [Fabric API](https://www.curseforge.com/minecraft/mc-mods/fabric-api))

## Downloads

Downloads can be obtained from the [releases](https://github.com/jpenilla/squaremap/releases) section.

<details>
<summary>Development builds</summary>

> Development builds are available at https://jenkins.jpenilla.xyz/job/squaremap/
</details>

## Demo

Official squaremap demo: https://squaremap-demo.jpenilla.xyz/

## License

This project is licensed under the [MIT license](https://github.com/jpenilla/squaremap/blob/master/LICENSE)

Leaflet (the web ui frontend) is licensed under [2-clause BSD License](https://github.com/Leaflet/Leaflet/blob/master/LICENSE)

## API

squaremap provides simple APIs to draw markers, shapes, icons, and etc. on rendered maps. Javadocs are hosted on the maven repository alongside the binaries, and should be automatically downloaded by your IDE. 

### Dependency Information

Releases are published to Maven Central

<details>
<summary>Using snapshot builds</summary>

> Snapshot builds are available on the Sonatype snapshots maven repository: `https://s01.oss.sonatype.org/content/repositories/snapshots/`
>
> Consult your build tool's documentation for details on adding maven repositories to your project.
</details>

Maven
```xml
<dependency>
    <groupId>xyz.jpenilla</groupId>
    <artifactId>squaremap-api</artifactId>
    <version>1.1.8</version>
    <scope>provided</scope>
</dependency>
```

Gradle
```kotlin
repositories {
    mavenCentral()
}

dependencies {
    compileOnly("xyz.jpenilla", "squaremap-api", "1.1.8")
}
```

## Building from source

Build squaremap by invoking the `build` task with Gradle.

```
./gradlew build
```

## bStats

Usage stats for the Paper platform

[![bStats Graph Data](https://bstats.org/signatures/bukkit/squaremap.svg)](https://bstats.org/plugin/bukkit/squaremap/13571)
