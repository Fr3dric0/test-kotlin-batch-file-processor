package io.lindhagen.batchfileprocessor.util

import software.amazon.awssdk.regions.Region

data class EnvConfig(
  val region: Region,
  val eventBridgeBusArn: String,
) {

  companion object {
    fun load(): EnvConfig {
      return EnvConfig(
        region = Region.US_EAST_1,
        eventBridgeBusArn = "",
      )
    }
  }
}
