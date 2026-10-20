locals {
  backend_service_account_member = "serviceAccount:${google_service_account.backend_run.email}"
  github_service_account_member  = "serviceAccount:${google_service_account.github_deploy.email}"

  github_repository_principal = "principalSet://iam.googleapis.com/${google_iam_workload_identity_pool.github.name}/attribute.repository/${var.github_repository}"
}

resource "google_project_iam_member" "backend_cloudsql_client" {
  project = var.project_id
  role    = "roles/cloudsql.client"
  member  = local.backend_service_account_member
}

resource "google_artifact_registry_repository_iam_member" "github_writer" {
  project    = var.project_id
  location   = var.region
  repository = google_artifact_registry_repository.tienda.repository_id

  role   = "roles/artifactregistry.writer"
  member = local.github_service_account_member
}

resource "google_service_account_iam_member" "github_workload_identity" {
  service_account_id = google_service_account.github_deploy.name

  role   = "roles/iam.workloadIdentityUser"
  member = local.github_repository_principal
}

resource "google_service_account_iam_member" "github_backend_service_account_user" {
  service_account_id = google_service_account.backend_run.name

  role   = "roles/iam.serviceAccountUser"
  member = local.github_service_account_member
}

resource "google_service_account_iam_member" "github_frontend_service_account_user" {
  service_account_id = google_service_account.frontend_run.name

  role   = "roles/iam.serviceAccountUser"
  member = local.github_service_account_member
}

resource "google_secret_manager_secret_iam_member" "backend_db_password" {
  project   = var.project_id
  secret_id = google_secret_manager_secret.db_password.secret_id

  role   = "roles/secretmanager.secretAccessor"
  member = local.backend_service_account_member
}

resource "google_secret_manager_secret_iam_member" "backend_jwt_secret" {
  project   = var.project_id
  secret_id = google_secret_manager_secret.jwt_secret.secret_id

  role   = "roles/secretmanager.secretAccessor"
  member = local.backend_service_account_member
}

resource "google_secret_manager_secret_iam_member" "backend_admin_password" {
  project   = var.project_id
  secret_id = google_secret_manager_secret.admin_password.secret_id

  role   = "roles/secretmanager.secretAccessor"
  member = local.backend_service_account_member
}

resource "google_secret_manager_secret_iam_member" "backend_mail_password" {
  project   = var.project_id
  secret_id = google_secret_manager_secret.mail_password.secret_id

  role   = "roles/secretmanager.secretAccessor"
  member = local.backend_service_account_member
}

resource "google_cloud_run_v2_service_iam_member" "backend_public" {
  project  = var.project_id
  location = var.region
  name     = google_cloud_run_v2_service.backend.name

  role   = "roles/run.invoker"
  member = "allUsers"
}

resource "google_cloud_run_v2_service_iam_member" "frontend_public" {
  project  = var.project_id
  location = var.region
  name     = google_cloud_run_v2_service.frontend.name

  role   = "roles/run.invoker"
  member = "allUsers"
}

resource "google_cloud_run_v2_service_iam_member" "github_backend_developer" {
  project  = var.project_id
  location = var.region
  name     = google_cloud_run_v2_service.backend.name

  role   = "roles/run.developer"
  member = local.github_service_account_member
}

resource "google_cloud_run_v2_service_iam_member" "github_frontend_developer" {
  project  = var.project_id
  location = var.region
  name     = google_cloud_run_v2_service.frontend.name

  role   = "roles/run.developer"
  member = local.github_service_account_member
}
