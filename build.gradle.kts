plugins {
  id("parent-conventions")
}

allprojects {
  group = "xyz.jpenilla"
  version = "1.1.2-SNAPSHOT"
  description = "Minimalistic and lightweight world map viewer for Minecraft servers"
}

subprojects {
  apply(plugin = "base-conventions")
}
