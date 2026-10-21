plugins {
  id("squaremap.parent")
  // Loaded once so the paper and demo projects share run-paper's build services
  alias(libs.plugins.run.paper) apply false
}
