
// Storage of the lambda artifacts used in this environment
resource "aws_s3_bucket" "lambda_artifact" {
  bucket = "${local.qualified_name}-lambda-artifact"
  tags = var.tags
}

resource "aws_s3_bucket_public_access_block" "lambda_artifact" {
  bucket = aws_s3_bucket.lambda_artifact.id
  block_public_acls = true
  block_public_policy = true
  ignore_public_acls = true
  restrict_public_buckets = true
}
