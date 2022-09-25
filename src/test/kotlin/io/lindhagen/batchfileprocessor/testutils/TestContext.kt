package io.lindhagen.batchfileprocessor.testutils

import com.amazonaws.services.lambda.runtime.ClientContext
import com.amazonaws.services.lambda.runtime.CognitoIdentity
import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.LambdaLogger

class TestContext: Context {
  override fun getAwsRequestId(): String = "123123123"

  override fun getLogGroupName(): String = "testlog/group"

  override fun getLogStreamName(): String = "..."

  override fun getFunctionName(): String = "test-function"

  override fun getFunctionVersion(): String = "1"

  override fun getInvokedFunctionArn(): String = "arn:...:..."

  override fun getIdentity(): CognitoIdentity {
    TODO("Not yet implemented")
  }

  override fun getClientContext(): ClientContext {
    TODO("Not yet implemented")
  }

  override fun getRemainingTimeInMillis(): Int {
    TODO("Not yet implemented")
  }

  override fun getMemoryLimitInMB(): Int {
    TODO("Not yet implemented")
  }

  override fun getLogger(): LambdaLogger {
    TODO("Not yet implemented")
  }
}
