data "aws_caller_identity" "current" {}
data "aws_region" "current" {}

locals {
  qualified_name = "${var.application_name}-${var.environment}"

  distribution_file_path = "../../build/distributions/CommuteWeatherApplication.zip"
}

resource "aws_iam_role" "lambda_execution_role" {
  name = "${local.qualified_name}-execution-role"

  assume_role_policy = <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Action": "sts:AssumeRole",
      "Principal": {
        "Service": "lambda.amazonaws.com"
      },
      "Effect": "Allow",
      "Sid": ""
    }
  ]
}
EOF

  tags = var.tags
}

resource "aws_cloudwatch_log_group" "weather_forecast" {
  name              = "/aws/lambda/${local.qualified_name}-forecast"
  retention_in_days = 7
}

resource "aws_iam_policy" "weather_forecast" {
  name        = "${local.qualified_name}-forecast-logging"

  policy = <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Action": [
        "logs:CreateLogGroup",
        "logs:CreateLogStream",
        "logs:PutLogEvents"
      ],
      "Resource": "arn:aws:logs:*:*:*",
      "Effect": "Allow"
    }
  ]
}
EOF
}

resource "aws_iam_role_policy_attachment" "weather_forecast" {
  role       = aws_iam_role.lambda_execution_role.name
  policy_arn = aws_iam_policy.weather_forecast.arn
}

// Deployment of the function
resource "aws_s3_object" "weather_forecast_artifact" {
  bucket = aws_s3_bucket.lambda_artifact.id
  key = "forecast.zip"
  source = local.distribution_file_path

  etag = filemd5(local.distribution_file_path)
  tags = var.tags
}

resource "aws_lambda_function" "weather_forecast" {
  role = aws_iam_role.lambda_execution_role.arn
  function_name = "${local.qualified_name}-forecast"
  handler = "io.lindhagen.commute.weather.CommuteWeatherApplicationAppFunction"

  runtime = "java11"
  memory_size = 256
  architectures = ["arm64"]

  s3_bucket = aws_s3_object.weather_forecast_artifact.bucket
  s3_key = aws_s3_object.weather_forecast_artifact.key
  s3_object_version = aws_s3_object.weather_forecast_artifact.version_id

  tags = var.tags

  depends_on = [
    aws_iam_role_policy_attachment.weather_forecast,
    aws_cloudwatch_log_group.weather_forecast,
  ]
}
