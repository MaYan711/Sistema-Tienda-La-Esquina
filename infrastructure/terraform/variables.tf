variable "project_id" {
  description = "ID del proyecto de Google Cloud"
  type        = string
  default     = "tienda-la-esquina"
}

variable "region" {
  description = "Region principal de despliegue"
  type        = string
  default     = "us-central1"
}

variable "github_repository" {
  description = "Repositorio autorizado para GitHub Actions"
  type        = string
  default     = "MaYan711/Sistema-Tienda-La-Esquina"
}
