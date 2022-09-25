package io.lindhagen.batchfileprocessor.testutils

import java.lang.IllegalArgumentException
import org.junit.jupiter.api.BeforeEach
import org.testcontainers.containers.localstack.LocalStackContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.eventbridge.EventBridgeClient
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.PutObjectRequest

@Testcontainers
abstract class TestContainersWrapper {
  companion object {
    internal val localStackEventBridgeEnabledService = LocalStackContainer.EnabledService.named("events")
  }

  /**
   * The LocalStack instance started before each test
   * */
  @Container
  protected var localStack: LocalStackContainer = LocalStackContainer(DockerImageName.parse("localstack/localstack:1.1.0"))
    .withServices(LocalStackContainer.Service.S3, localStackEventBridgeEnabledService)

  private val s3BaseUri by lazy { localStack.getEndpointOverride(LocalStackContainer.Service.S3) }

  /**
   * S3 client connected to LocalStack
   * */
  protected lateinit var s3Client: S3Client
  /**
   * EventBridge Client connected to LocalStack
   * */
  protected lateinit var eventBridgeClient: EventBridgeClient

  @BeforeEach
  fun setup() {
    s3Client = buildLocalStackS3Client(localStack)
    eventBridgeClient = buildEventBridgeClient(localStack)
  }

  protected fun createS3Bucket(s3Client: S3Client, bucketName: String): String {
    val bucketPath = s3Client.createBucket { it.bucket(bucketName) }.location()

    return "$s3BaseUri$bucketPath"
  }

  fun uploadFileToS3Bucket(s3Client: S3Client, bucketName: String, fileName: String, filePath: String) {
    val bytes = this::class.java.getResourceAsStream(filePath)?.readAllBytes()
      ?: throw IllegalArgumentException("Failed to find file with path: $filePath")

    val body = RequestBody.fromBytes(bytes)

    s3Client.putObject(
      PutObjectRequest.builder()
        .bucket(bucketName)
        .key(fileName)
        .build(),
      body,
    )
  }
}

fun buildLocalStackS3Client(localStack: LocalStackContainer): S3Client = S3Client
  .builder()
  .endpointOverride(localStack.getEndpointOverride(LocalStackContainer.Service.S3))
  .credentialsProvider(
    StaticCredentialsProvider.create(
      AwsBasicCredentials.create(localStack.accessKey, localStack.secretKey)
    )
  )
  .region(Region.of(localStack.region))
  .build()

fun buildEventBridgeClient(localStack: LocalStackContainer): EventBridgeClient = EventBridgeClient
  .builder()
  .endpointOverride(localStack.getEndpointOverride(TestContainersWrapper.localStackEventBridgeEnabledService))
  .credentialsProvider(
    StaticCredentialsProvider.create(
      AwsBasicCredentials.create(localStack.accessKey, localStack.secretKey)
    )
  )
  .region(Region.of(localStack.region))
  .build()
