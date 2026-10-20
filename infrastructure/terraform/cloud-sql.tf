resource "google_sql_database_instance" "tienda" {
  project          = var.project_id
  name             = "tienda-la-esquina-db"
  region           = var.region
  database_version = "POSTGRES_18"

  settings {
    tier                        = "db-f1-micro"
    edition                     = "ENTERPRISE"
    availability_type           = "ZONAL"
    enable_dataplex_integration = true

    disk_type       = "PD_SSD"
    disk_size       = 10
    disk_autoresize = true

    backup_configuration {
      enabled = false
    }

    ip_configuration {
      ipv4_enabled = true
      ssl_mode     = "ALLOW_UNENCRYPTED_AND_ENCRYPTED"
    }
  }

  lifecycle {
    prevent_destroy = true

    ignore_changes = [
      settings[0].disk_size
    ]
  }
}

resource "google_sql_database" "tienda" {
  project   = var.project_id
  name      = "tienda_la_esquina_db"
  instance  = google_sql_database_instance.tienda.name
  charset   = "UTF8"
  collation = "en_US.UTF8"

  deletion_policy = "DELETE"

  lifecycle {
    prevent_destroy = true
  }
}