package com.mtls

import android.util.Log
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.ReadableMap
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.request
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
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
      reactApplicationContext.assets.open(PUBLIC_CERT_FILE_NAME), privateKey, baseUrl, reactApplicationContext
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

      var response: JsonObject? = null

      try {
        val apiResponse = networkingClient.makeRequest(
          path, method, headers.toHashMap(), params.toHashMap(), body.toHashMap().toJsonElement()
        )

        if (BuildConfig.DEBUG) {
          Log.d("ReactNativeMtls", "responseUrl = ${apiResponse.request.url}, status=${apiResponse.status.value}")
        }

        response = JsonObject(
          mapOf(
            "kind" to "ok".toJsonElement(),
            "status" to apiResponse.status.value.toJsonElement(),
            "body" to jsonInstance.parseToJsonElement(apiResponse.bodyAsText())
          )
        )
      } catch (e: Exception) {
        if (BuildConfig.DEBUG) {
          Log.d("ReactNativeMtls", "exception = ${e.message}", e)
        }
        if (e is IOException) {
          response = JsonObject(
            mapOf("kind" to "cannot-connect".toJsonElement())
          )
        }
      }

      response = response ?: JsonObject(
        mapOf("kind" to "unknown".toJsonElement())
      )
      promise.resolve(
        response.toJsonString()
      )
    }
  }

  @ReactMethod
  fun multipart(
    path: String,
    headers: ReadableMap,
    params: ReadableMap,
    body: ReadableMap,
    fileName: String,
    filePath: String,
    fileHeaders: ReadableMap,
    promise: Promise
  ) {
    val networkingClient = networkingClient
      ?: throw IllegalStateException("networking client must be created before a network request can be initiated")

    GlobalScope.launch(IO) {

      var response: JsonObject? = null

      try {
        val apiResponse = networkingClient.multipart(
          path,
          headers.toHashMap(),
          params.toHashMap(),
          body.toHashMap(),
          fileName,
          filePath,
          fileHeaders.toHashMap()
        )

        if (BuildConfig.DEBUG) {
          Log.d("ReactNativeMtls", "response = $apiResponse")
        }

        response = JsonObject(
          mapOf(
            "kind" to "ok".toJsonElement(),
            "status" to apiResponse.status.value.toJsonElement(),
            "body" to jsonInstance.parseToJsonElement(apiResponse.bodyAsText())
          )
        )
      } catch (e: Exception) {
        if (BuildConfig.DEBUG) {
          Log.d("ReactNativeMtls", "exception = ${e.message}", e)
        }
        if (e is IOException) {
          response = JsonObject(
            mapOf("kind" to "cannot-connect".toJsonElement())
          )
        }
      }

      response = response ?: JsonObject(
        mapOf("kind" to "unknown".toJsonElement())
      )
      promise.resolve(
        response.toJsonString()
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
    else -> jsonInstance.encodeToJsonElement(serializer(this::class.createType()), this)
  }

  fun Any?.toJsonString(): String = jsonInstance.encodeToString(this.toJsonElement())

  companion object {
    const val NAME = "Mtls"
    const val PUBLIC_CERT_FILE_NAME = "mtls_public_cert.cer"
  }
}
