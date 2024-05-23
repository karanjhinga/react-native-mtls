package com.mtls

import android.util.Log
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.ReadableMap
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.serializer
import okio.IOException
import kotlin.reflect.full.createType


class MtlsModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {

  private var networkingClient: NetworkingClient? = null

  override fun getName(): String {
    return NAME
  }

  @ReactMethod
  fun setup(privateKey: String, baseUrl: String, promise: Promise) {
    networkingClient = NetworkingClient(
      reactApplicationContext.assets.open(PUBLIC_CERT_FILE_NAME), privateKey, baseUrl
    )
    promise.resolve(true)
  }

  @OptIn(DelicateCoroutinesApi::class)
  @ReactMethod
  fun makeRequest(
    path: String,
    method: String,
    headers: ReadableMap,
    params: ReadableMap,
    body: ReadableMap,
    promise: Promise
  ) {

    val networkingClient = networkingClient
      ?: throw IllegalStateException("networking client must be created before a network request can be initiated")

    GlobalScope.launch(IO) {

      var response: NetworkResponse? = null

      try {
        val apiResponse = networkingClient.makeRequest(
          path, method, headers.toHashMap(), params.toHashMap(), body.toJsonElement()
        )

        if (BuildConfig.DEBUG) {
          Log.d("ReactNativeMtls", "response = $apiResponse")
        }

        response = NetworkResponse(
          "ok",
          apiResponse.status.value,
          apiResponse.bodyAsText().ifBlank { "{}" }
        )
      } catch (e: Exception) {
        if (BuildConfig.DEBUG) {
          Log.d("ReactNativeMtls", "exception = ${e.message}", e)
        }
        if (e is IOException) {
          response = NetworkResponse("cannot-connect", null, null)
        }
      }

      promise.resolve(
        (response ?: NetworkResponse("unknown", null, null)).toString()
      )
    }
  }

  private fun Any?.toJsonElement(): JsonElement = when (this) {
    null -> JsonNull
    is JsonElement -> this
    is Number -> JsonPrimitive(this)
    is Boolean -> JsonPrimitive(this)
    is String -> JsonPrimitive(this)
    is Array<*> -> JsonArray(map { it.toJsonElement() })
    is List<*> -> JsonArray(map { it.toJsonElement() })
    is Map<*, *> -> JsonObject(map { it.key.toString() to it.value.toJsonElement() }.toMap())
    else -> Json.encodeToJsonElement(serializer(this::class.createType()), this)
  }

  fun Any?.toJsonString(): String = Json.encodeToString(this.toJsonElement())

  companion object {
    const val NAME = "Mtls"
    const val PUBLIC_CERT_FILE_NAME = "mtls_public_cert.cer"
  }
}
