resource "google_artifact_registry_repository" "tienda" {
  project       = var.project_id
  location      = var.region
  repository_id = "tienda-la-esquina"
  description   = "Imagenes Docker de Tienda La Esquina"
  format        = "DOCKER"

  lifecycle {
    prevent_destroy = true
  }
}
