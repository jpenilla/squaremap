plugins {
  id("parent-conventions")
}

allprojects {
  group = "xyz.jpenilla"
  version = "1.1.0"
  description = "Minimalistic and lightweight world map viewer for Minecraft servers"
}

subprojects {
  apply(plugin = "base-conventions")
}
