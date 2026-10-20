resource "google_secret_manager_secret" "db_password" {
  project   = var.project_id
  secret_id = "tienda-db-password"

  replication {
    auto {}
  }

  lifecycle {
    prevent_destroy = true
  }
}

resource "google_secret_manager_secret" "jwt_secret" {
  project   = var.project_id
  secret_id = "tienda-jwt-secret"

  replication {
    auto {}
  }

  lifecycle {
    prevent_destroy = true
  }
}

resource "google_secret_manager_secret" "admin_password" {
  project   = var.project_id
  secret_id = "tienda-admin-password"

  replication {
    auto {}
  }

  lifecycle {
    prevent_destroy = true
  }
}

resource "google_secret_manager_secret" "mail_password" {
  project   = var.project_id
  secret_id = "tienda-mail-password"

  replication {
    auto {}
  }

  lifecycle {
    prevent_destroy = true
  }
}
