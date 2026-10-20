terraform {
  required_version = ">= 1.5.0"

  required_providers {
    google = {
      source  = "hashicorp/google"
      version = "~> 8.7"
    }
  }
  backend "gcs" {
    bucket = "tienda-la-esquina-tfstate-664365722900"
    prefix = "production"
  }
}

provider "google" {
  project = var.project_id
  region  = var.region
}
