plugins {
  base
}

allprojects {
  group = "xyz.jpenilla"
  version = "1.1.0-SNAPSHOT"
  description = "Minimalistic and lightweight world map viewer for Paper servers"
}

subprojects {
  apply(plugin = "base-conventions")
}
