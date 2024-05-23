package com.mtls

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
class NetworkResponse(
  @SerialName("kind") val kind: String,
  @SerialName("status") val status: Int?,
  @SerialName("body") val body: String?
) {

  override fun toString(): String {
    return jsonInstance.encodeToString(this)
  }
}
