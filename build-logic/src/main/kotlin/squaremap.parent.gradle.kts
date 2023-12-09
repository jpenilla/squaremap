plugins {
  base
  id("net.kyori.indra.publishing.sonatype")
}

indraSonatype {
  useAlternateSonatypeOSSHost("s01")
}
