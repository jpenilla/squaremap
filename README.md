<div align="center">
<img src="https://raw.githubusercontent.com/pl3xgaming/Pl3xMap/master/plugin/src/main/resources/web/images/og.png" alt="Pl3xMap">

# Pl3xMap

[![MIT License](https://img.shields.io/github/license/pl3xgaming/Pl3xMap?&logo=github)](License)
[![CodeFactor](https://www.codefactor.io/repository/github/pl3xgaming/pl3xmap/badge)](https://www.codefactor.io/repository/github/pl3xgaming/pl3xmap)
[![Join us on Discord](https://img.shields.io/discord/838127837667131433.svg?label=&logo=discord&logoColor=ffffff&color=7389D8&labelColor=6A7EC2)](https://discord.gg/B8WpDPXeBh)

[![Pl3xMap's Stargazers](https://img.shields.io/github/stars/pl3xgaming/Pl3xMap?label=stars&logo=github)](https://github.com/pl3xgaming/Pl3xMap/stargazers)
[![BillyGalbreath's Followers](https://img.shields.io/github/followers/BillyGalbreath?label=followers&logo=github)](https://github.com/BillyGalbreath?tab=followers)
[![Pl3xMap Forks](https://img.shields.io/github/forks/pl3xgaming/Pl3xMap?label=forks&logo=github)](https://github.com/pl3xgaming/Pl3xMap/network/members)
[![Pl3xMap Watchers](https://img.shields.io/github/watchers/pl3xgaming/Pl3xMap?label=watchers&logo=github)](https://github.com/pl3xgaming/Pl3xMap/watchers)

Pl3xMap is a minimalistic and lightweight live world map viewer for Paper servers.

</div>

## What is Pl3xMap

If, like me, you have no real need for 3D views, the novelty of Dynmap and Bluemap have worn off, and you're ready for something actually usable for navigation without all the heavy bulk or slow renders then this is the plugin for you.

## Features

* Ultra fast render times. Get your map viewable today, not next week!
* Simple vanilla-like top down 2D view, designed for navigation.
* Player markers showing yaw rotation, health, and armor
* [Easy configuration](https://github.com/pl3xgaming/Pl3xMap/wiki/Default-config.yml). Even a caveman can do it.
* Up to date Leaflet front-end.
* [Addons and integrations](ADDONS_INTEGRATIONS.md) for many popular plugins.

## Contact
[![Join us on Discord](https://img.shields.io/discord/838127837667131433.svg?label=&logo=discord&logoColor=ffffff&color=7389D8&labelColor=6A7EC2)](https://discord.gg/B8WpDPXeBh)

Join us on [Discord](https://discord.gg/B8WpDPXeBh) in the #pl3xmap channel

## Downloads
Downloads can be obtained from the [releases](https://github.com/pl3xgaming/Pl3xMap/releases) section.

## Servers Using Pl3xMap

[Click here](SERVERS.md) to view a list of servers using Pl3xMap.

## License
[![MIT License](https://img.shields.io/github/license/pl3xgaming/Pl3xMap?&logo=github)](License)

This project is licensed under the [MIT license](https://github.com/pl3xgaming/Pl3xMap/blob/master/LICENSE)

Leaflet (the web ui frontend) is licensed under [2-clause BSD License](https://github.com/Leaflet/Leaflet/blob/master/LICENSE)

## bStats

[![bStats Graph Data](https://bstats.org/signatures/bukkit/Pl3xMap.svg)](https://bstats.org/plugin/bukkit/Pl3xMap/10133)

## API

### [Javadoc](https://javadoc.pl3x.net/pl3xmap/)

### Dependency Information
Maven
```xml
<repository>
    <id>pl3x-repo</id>
    <url>https://repo.pl3x.net/</url>
</repository>
```
```xml
<dependency>
    <groupId>net.pl3x.map</groupId>
    <artifactId>pl3xmap-api</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <scope>provided</scope>
</dependency>
```

Gradle
```kotlin
repositories {
    maven("https://repo.pl3x.net/")
}
```
```kotlin
dependencies {
    compileOnly("net.pl3x.map", "pl3xmap-api", "1.0.0-SNAPSHOT")
}
```

## Building from source

To compile Pl3xMap, you need to have the Paper server implementation installed to your local maven repository.

Download Paper from [papermc.io/downloads](https://papermc.io/downloads) then run this Java command (using the JAR you downloaded)

```
java -jar -Dpaperclip.install=true paper-1.17-75.jar
```

Once that is complete you can compile Pl3xMap with this command

```
./gradlew build
```
