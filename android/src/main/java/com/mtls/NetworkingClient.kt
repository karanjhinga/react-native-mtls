package com.mtls

import android.content.Context
import android.util.Log
import androidx.core.net.toUri
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.headers
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import io.ktor.http.parameters
import io.ktor.http.path
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import okhttp3.OkHttpClient
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.security.KeyFactory
import java.security.KeyStore
import java.security.SecureRandom
import java.security.cert.Certificate
import java.security.cert.CertificateFactory
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Base64
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager


val jsonInstance = Json {
  ignoreUnknownKeys = true
  explicitNulls = false
}

class NetworkingClient(
  certificateInputStream: InputStream,
  certificatePrivateKey: String,
  baseUrl: String,
  val context: Context
) {

  private val okHttpClient: OkHttpClient
  private val sslContext: SSLContext

  init {

    val certificateFactory = CertificateFactory.getInstance("X.509")

    // Get Private Key
    val rawPrivateKeyByteArray: ByteArray = Base64.getDecoder().decode(certificatePrivateKey)
    val keyFactory = KeyFactory.getInstance("RSA")
    val keySpec = PKCS8EncodedKeySpec(rawPrivateKeyByteArray)

    // Get certificate
    val certificate: Certificate =
      certificateFactory.generateCertificate(certificateInputStream)

    // Set up KeyStore
    val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())
    keyStore.load(null, KEY_SECRET.toCharArray())
    keyStore.setKeyEntry(
      "client",
      keyFactory.generatePrivate(keySpec),
      KEY_SECRET.toCharArray(),
      arrayOf(certificate)
    )
    certificateInputStream.close()

    // Set up Trust Managers
    val trustManagerFactory =
      TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
    trustManagerFactory.init(null as KeyStore?)
    val trustManagers = trustManagerFactory.trustManagers

    // Set up Key Managers
    val keyManagerFactory =
      KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
    keyManagerFactory.init(keyStore, KEY_SECRET.toCharArray())
    val keyManagers = keyManagerFactory.keyManagers

    // Obtain SSL Socket Factory
    sslContext = SSLContext.getInstance("TLS")
    sslContext.init(keyManagers, trustManagers, SecureRandom())
    val sslSocketFactory: SSLSocketFactory = sslContext.socketFactory

    okHttpClient = OkHttpClient.Builder()
      .sslSocketFactory(sslSocketFactory, trustManagers[0] as X509TrustManager)
      .build()

  }

  private var client = HttpClient(OkHttp.create {
    preconfigured = okHttpClient
  }) {
    install(ContentNegotiation) {
      json(jsonInstance)
    }
    defaultRequest {
      url(baseUrl)
      contentType(ContentType.Application.Json)
    }
  }

  suspend fun makeRequest(
    path: String,
    method: String,
    headers: Map<String, Any> = emptyMap(),
    params: Map<String, Any> = emptyMap(),
    body: JsonElement
  ): HttpResponse {
    return client.request {

      this.method = HttpMethod(method)

      headers {
        headers.forEach { (key, value) ->
          append(key, value.toString())
        }
      }

      parameters {
        params.forEach { (key, value) ->
          append(key, value.toString())
        }
      }

      setBody(body)

      url {
        path(path)
      }
    }
  }

  suspend fun multipart(
    path: String,
    headers: Map<String, Any> = emptyMap(),
    params: Map<String, Any> = emptyMap(),
    body: Map<String, Any> = emptyMap(),
    fileName: String,
    filePath: String,
    fileHeaders: Map<String, Any> = emptyMap()
  ): HttpResponse {
    val h = Headers.build {
      fileHeaders.forEach { (key, value) ->
        append(key, value.toString())
        append(HttpHeaders.ContentType, "image/jpeg")
        append(HttpHeaders.ContentDisposition, "filename=image.png")
      }
    }

    val iStream = context.contentResolver.openInputStream(filePath.toUri())!!
    val inputData: ByteArray = getBytes(iStream)

    iStream.close()
    return client.submitFormWithBinaryData(
      formData = formData {
        append(fileName, inputData, h)
        body.forEach { (key, value) ->
          append(key, value.toString())
        }
      }) {
      headers {
        headers.forEach { (key, value) ->
          append(key, value.toString())
        }
      }

      parameters {
        params.forEach { (key, value) ->
          append(key, value.toString())
        }
      }
      url {
        path(path)
      }
    }
  }

  @Throws(IOException::class)
  fun getBytes(inputStream: InputStream): ByteArray {
    val byteBuffer = ByteArrayOutputStream()
    val bufferSize = 1024
    val buffer = ByteArray(bufferSize)
    var len = 0
    while (inputStream.read(buffer).also { len = it } != -1) {
      byteBuffer.write(buffer, 0, len)
    }
    return byteBuffer.toByteArray()
  }

  companion object {
    private const val KEY_SECRET = "secret"
  }
}
