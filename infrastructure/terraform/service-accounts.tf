resource "google_service_account" "backend_run" {
  project      = var.project_id
  account_id   = "tienda-backend-run"
  display_name = "Tienda La Esquina Backend"

  lifecycle {
    prevent_destroy = true
  }
}

resource "google_service_account" "frontend_run" {
  project      = var.project_id
  account_id   = "tienda-frontend-run"
  display_name = "Tienda La Esquina Frontend"

  lifecycle {
    prevent_destroy = true
  }
}

resource "google_service_account" "github_deploy" {
  project      = var.project_id
  account_id   = "github-actions-deploy"
  display_name = "GitHub Actions Deploy"

  lifecycle {
    prevent_destroy = true
  }
}
