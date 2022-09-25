
variable "application_name" {
  type = string
}

variable "environment" {
  type = string
}

variable "commit_sha" {
  type = string
}

variable "tags" {
  type = map(string)
}
