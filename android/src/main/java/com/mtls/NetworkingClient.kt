package com.mtls

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.headers
import io.ktor.client.request.request
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpMethod
import io.ktor.http.parameters
import io.ktor.http.path
import okhttp3.OkHttpClient
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

class NetworkingClient(
  certificateInputStream: InputStream,
  certificatePrivateKey: String,
  baseUrl: String
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
    defaultRequest { url(baseUrl) }
  }

  suspend fun makeRequest(
    path: String,
    method: String,
    headers: Map<String, Any> = emptyMap(),
    params: Map<String, Any> = emptyMap()
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

      url {
        path(path)
      }
    }
  }

  companion object {
    private const val KEY_SECRET = "secret"
  }
}
