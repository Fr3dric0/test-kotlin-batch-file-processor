terraform {
  required_version = "~> 1.0"

  required_providers {
    aws = {
      source = "hashicorp/aws"
      version = "4.29.0"
    }
  }

  backend "s3" {
    bucket = "lokalvert-terraform-state"
    key    = "batch-file-processor/dev.terraform.tfstate"
    region = "us-east-1"
    profile = "linio"
  }
}

provider "aws" {
  region = "us-east-1"
}

locals {
  last_commit_sha = trimspace(file("../../.git/${trimspace(trimprefix(file("../../.git/HEAD"), "ref:"))}"))
}

module "application" {
  source = "../template"
  application_name = "batch-file-processor"
  environment = "dev"
  commit_sha = local.last_commit_sha

  tags = {
    Application = "batch-file-processor"
    Environment = "dev"
    ManagedBy = "terraform"
  }
}
