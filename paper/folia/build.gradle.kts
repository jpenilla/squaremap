plugins {
  id("squaremap.base-conventions")
}

dependencies {
  compileOnly(projects.squaremapCommon)
  compileOnly(libs.foliaApi)
}
