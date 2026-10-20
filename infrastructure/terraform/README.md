# Infraestructura como Codigo - Tienda La Esquina

La infraestructura de produccion de Tienda La Esquina se administra mediante Terraform sobre Google Cloud Platform.

## Recursos administrados

- Artifact Registry para imagenes Docker.
- Cloud SQL con PostgreSQL 18.
- Base de datos tienda_la_esquina_db.
- Secret Manager.
- Service Accounts.
- IAM con minimo privilegio.
- Workload Identity Federation para GitHub Actions.
- Cloud Run para backend y frontend.
- Permisos de acceso de Cloud Run.

## Estado remoto

El estado de Terraform se almacena remotamente en Google Cloud Storage:

gs://tienda-la-esquina-tfstate-664365722900/production/default.tfstate

El bucket utiliza versionado y prevencion de acceso publico.

## Seguridad

Los valores sensibles no se almacenan en los archivos Terraform ni en Git.

Los secretos utilizados por el backend se administran mediante Google Secret Manager:

- tienda-db-password
- tienda-jwt-secret
- tienda-admin-password
- tienda-mail-password

GitHub Actions se autentica con Google Cloud mediante Workload Identity Federation, sin utilizar llaves JSON permanentes.

## Terraform y CI/CD

Terraform administra la configuracion de la infraestructura.

GitHub Actions administra la integracion y despliegue continuo:

1. Ejecuta pruebas del backend.
2. Compila el frontend.
3. Construye las imagenes Docker.
4. Publica las imagenes en Artifact Registry.
5. Despliega backend y frontend en Cloud Run.
6. Ejecuta smoke tests.

Las imagenes Docker y metadatos generados por GitHub Actions se ignoran en el ciclo de vida de Terraform para evitar conflictos entre IaC y CI/CD.

## Comandos principales

Inicializar:

terraform init

Validar:

terraform validate

Revisar cambios:

terraform plan

Antes de ejecutar terraform apply en produccion debe revisarse siempre el resultado de terraform plan.
