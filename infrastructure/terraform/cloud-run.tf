resource "google_cloud_run_v2_service" "backend" {
  project             = var.project_id
  name                = "tienda-backend"
  location            = var.region
  ingress             = "INGRESS_TRAFFIC_ALL"
  deletion_protection = true

  scaling {
    max_instance_count = 2
  }

  template {
    service_account                  = google_service_account.backend_run.email
    timeout                          = "300s"
    max_instance_request_concurrency = 80

    scaling {
      min_instance_count = 0
      max_instance_count = 20
    }

    containers {
      image = "us-central1-docker.pkg.dev/tienda-la-esquina/tienda-la-esquina/backend:32617883473d39545a4a52374259ab84da8a40fc"

      ports {
        name           = "http1"
        container_port = 8080
      }

      resources {
        limits = {
          cpu    = "1"
          memory = "512Mi"
        }

        startup_cpu_boost = true
        cpu_idle          = true
      }

      env {
        name  = "SPRING_PROFILES_ACTIVE"
        value = "prod"
      }

      env {
        name  = "DATABASE_USERNAME"
        value = "tienda_app"
      }

      env {
        name  = "INITIAL_ADMIN_EMAIL"
        value = "sistemasss404@gmail.com"
      }

      env {
        name  = "MAIL_USERNAME"
        value = "sistemasss404@gmail.com"
      }

      env {
        name  = "MAIL_FROM"
        value = "sistemasss404@gmail.com"
      }

      env {
        name  = "DATABASE_URL"
        value = "jdbc:postgresql:///tienda_la_esquina_db?cloudSqlInstance=tienda-la-esquina:us-central1:tienda-la-esquina-db&socketFactory=com.google.cloud.sql.postgres.SocketFactory&cloudSqlRefreshStrategy=lazy&ipTypes=PUBLIC"
      }

      env {
        name  = "OPERATING_HOURS_TIME_ZONE"
        value = "America/Guatemala"
      }

      env {
        name = "DATABASE_PASSWORD"

        value_source {
          secret_key_ref {
            secret  = google_secret_manager_secret.db_password.secret_id
            version = "latest"
          }
        }
      }

      env {
        name = "SECRET_KEY_JWT"

        value_source {
          secret_key_ref {
            secret  = google_secret_manager_secret.jwt_secret.secret_id
            version = "latest"
          }
        }
      }

      env {
        name = "INITIAL_ADMIN_PASSWORD"

        value_source {
          secret_key_ref {
            secret  = google_secret_manager_secret.admin_password.secret_id
            version = "2"
          }
        }
      }

      env {
        name = "MAIL_PASSWORD"

        value_source {
          secret_key_ref {
            secret  = google_secret_manager_secret.mail_password.secret_id
            version = "2"
          }
        }
      }

      startup_probe {
        failure_threshold = 1
        period_seconds    = 240
        timeout_seconds   = 240

        tcp_socket {
          port = 8080
        }
      }
    }
  }

  lifecycle {
    prevent_destroy = true

    ignore_changes = [
      client,
      client_version,
      template[0].labels,
      template[0].containers[0].image
    ]
  }
}

resource "google_cloud_run_v2_service" "frontend" {
  project             = var.project_id
  name                = "tienda-frontend"
  location            = var.region
  ingress             = "INGRESS_TRAFFIC_ALL"
  deletion_protection = true

  scaling {
    max_instance_count = 2
  }

  template {
    service_account                  = google_service_account.frontend_run.email
    timeout                          = "60s"
    max_instance_request_concurrency = 80

    scaling {
      min_instance_count = 0
      max_instance_count = 20
    }

    containers {
      image = "us-central1-docker.pkg.dev/tienda-la-esquina/tienda-la-esquina/frontend:32617883473d39545a4a52374259ab84da8a40fc"

      ports {
        name           = "http1"
        container_port = 8080
      }

      resources {
        limits = {
          cpu    = "1"
          memory = "256Mi"
        }

        startup_cpu_boost = true
        cpu_idle          = true
      }

      env {
        name  = "BACKEND_URL"
        value = "https://tienda-backend-wdyp2f3a2a-uc.a.run.app"
      }

      startup_probe {
        failure_threshold = 1
        period_seconds    = 240
        timeout_seconds   = 240

        tcp_socket {
          port = 8080
        }
      }
    }
  }

  lifecycle {
    prevent_destroy = true

    ignore_changes = [
      client,
      client_version,
      template[0].labels,
      template[0].containers[0].image
    ]
  }
}
