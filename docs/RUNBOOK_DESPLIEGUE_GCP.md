# Runbook de Despliegue - Tienda La Esquina

## 1. Objetivo

Este documento describe el procedimiento de despliegue, operacion, recuperacion y mantenimiento del sistema Tienda La Esquina sobre Google Cloud Platform.

El sistema utiliza infraestructura como codigo con Terraform y CI/CD mediante GitHub Actions.

---

## 2. Arquitectura

La solucion esta compuesta por:

- Frontend Angular desplegado en Cloud Run.
- Backend Spring Boot desplegado en Cloud Run.
- PostgreSQL 18 administrado mediante Cloud SQL.
- Imagenes Docker almacenadas en Artifact Registry.
- Secretos almacenados en Secret Manager.
- Autenticacion de GitHub Actions mediante Workload Identity Federation.
- Estado de Terraform almacenado en Cloud Storage.

Proyecto Google Cloud:

tienda-la-esquina

Region:

us-central1

---

## 3. Recursos principales

### Cloud Run

Backend:

tienda-backend

Frontend:

tienda-frontend

### Cloud SQL

Instancia:

tienda-la-esquina-db

Base de datos:

tienda_la_esquina_db

Motor:

PostgreSQL 18

### Artifact Registry

Repositorio:

tienda-la-esquina

Region:

us-central1

### Secret Manager

Secretos utilizados:

- tienda-db-password
- tienda-jwt-secret
- tienda-admin-password
- tienda-mail-password

Los valores de los secretos no se almacenan en Git ni en Terraform.

### Service Accounts

Backend:

tienda-backend-run@tienda-la-esquina.iam.gserviceaccount.com

Frontend:

tienda-frontend-run@tienda-la-esquina.iam.gserviceaccount.com

CI/CD:

github-actions-deploy@tienda-la-esquina.iam.gserviceaccount.com

---

## 4. CI/CD

GitHub Actions implementa integracion y despliegue continuo.

### Ramas feature

Un push hacia una rama feature ejecuta:

1. Checkout del codigo.
2. Configuracion de Java 21.
3. Pruebas Gradle del backend.
4. Configuracion de Node.
5. Instalacion de dependencias.
6. Build de Angular.

Las ramas feature no despliegan automaticamente a Google Cloud.

### Develop y Main

Un push hacia develop o main ejecuta:

1. Integracion continua.
2. Construccion de imagen Docker del backend.
3. Construccion de imagen Docker del frontend.
4. Autenticacion con Google Cloud mediante Workload Identity Federation.
5. Publicacion de imagenes en Artifact Registry.
6. Despliegue del backend en Cloud Run.
7. Despliegue del frontend en Cloud Run.
8. Smoke test del backend.
9. Smoke test del frontend.
10. Validacion del proxy frontend-backend.

No se utilizan llaves JSON permanentes para GitHub Actions.

---

## 5. Flujo Git

El flujo utilizado es:

feature
  |
  v
develop
  |
  v
main

Las funcionalidades se desarrollan en ramas feature.

Una vez validadas se integran hacia develop.

Main representa la version estable final.

---

## 6. Despliegue automatico

Para desplegar una nueva version normalmente no es necesario ejecutar comandos manuales de Google Cloud.

El procedimiento es:

git push origin develop

GitHub Actions detecta el cambio y ejecuta automaticamente el pipeline de CI/CD.

El resultado debe verificarse en:

GitHub
→ Actions
→ CI
→ Deploy GCP

Ambos workflows deben finalizar correctamente.

---

## 7. Validacion del backend

Obtener URL:

gcloud run services describe tienda-backend \
  --project=tienda-la-esquina \
  --region=us-central1 \
  --format='value(status.url)'

Una solicitud sin autenticacion:

curl -i "$(gcloud run services describe tienda-backend \
  --project=tienda-la-esquina \
  --region=us-central1 \
  --format='value(status.url)')/api/v1/products"

El resultado esperado para un endpoint protegido es:

HTTP 401

Esto confirma que el backend esta disponible y que Spring Security esta protegiendo el recurso.

---

## 8. Validacion del frontend

Obtener URL:

gcloud run services describe tienda-frontend \
  --project=tienda-la-esquina \
  --region=us-central1 \
  --format='value(status.url)'

Validar:

curl -I "$(gcloud run services describe tienda-frontend \
  --project=tienda-la-esquina \
  --region=us-central1 \
  --format='value(status.url)')"

Resultado esperado:

HTTP 200

---

## 9. Logs de produccion

Backend:

gcloud run services logs read tienda-backend \
  --project=tienda-la-esquina \
  --region=us-central1 \
  --limit=100

Frontend:

gcloud run services logs read tienda-frontend \
  --project=tienda-la-esquina \
  --region=us-central1 \
  --limit=100

Para buscar errores:

gcloud run services logs read tienda-backend \
  --project=tienda-la-esquina \
  --region=us-central1 \
  --limit=200 | grep -Ei 'ERROR|Exception|failed'

---

## 10. Terraform

La infraestructura se encuentra en:

infrastructure/terraform

Inicializar:

cd infrastructure/terraform
terraform init

Validar configuracion:

terraform validate

Revisar diferencias:

terraform plan

El resultado normal en una infraestructura sincronizada debe ser:

No changes. Your infrastructure matches the configuration.

No debe ejecutarse terraform apply sin revisar previamente terraform plan.

---

## 11. Estado remoto de Terraform

El estado se almacena en Google Cloud Storage:

gs://tienda-la-esquina-tfstate-664365722900/production/default.tfstate

No se debe versionar ningun archivo terraform.tfstate en Git.

El bucket utiliza versionado para facilitar recuperacion de estados anteriores.

---

## 12. Recuperacion del estado Terraform

Inicializar nuevamente:

terraform init

Consultar recursos administrados:

terraform state list

Verificar infraestructura:

terraform plan

Si Terraform encuentra diferencias inesperadas, no ejecutar apply hasta revisar cada cambio.

---

## 13. Secret Manager

Listar secretos:

gcloud secrets list \
  --project=tienda-la-esquina

Consultar versiones de un secreto:

gcloud secrets versions list tienda-mail-password \
  --project=tienda-la-esquina

Nunca mostrar el contenido de un secreto en documentacion, capturas o commits.

---

## 14. Actualizacion de secretos

Ejemplo para agregar una nueva version:

printf '%s' 'NUEVO_VALOR' | \
gcloud secrets versions add NOMBRE_SECRETO \
  --project=tienda-la-esquina \
  --data-file=-

Despues de modificar un secreto utilizado por Cloud Run puede ser necesario generar una nueva revision del servicio dependiendo de la version configurada.

---

## 15. Cloud SQL

Consultar instancia:

gcloud sql instances describe tienda-la-esquina-db \
  --project=tienda-la-esquina

Estado esperado:

RUNNABLE

La base de datos utilizada es:

tienda_la_esquina_db

El usuario de aplicacion es:

tienda_app

La contraseña se obtiene mediante Secret Manager.

---

## 16. Artifact Registry

Consultar repositorio:

gcloud artifacts repositories describe tienda-la-esquina \
  --project=tienda-la-esquina \
  --location=us-central1

Las imagenes se almacenan en:

us-central1-docker.pkg.dev/tienda-la-esquina/tienda-la-esquina/

Las imagenes utilizan el SHA del commit como etiqueta.

---

## 17. Workload Identity Federation

Pool:

github-actions

Provider:

github-tienda

Repositorio autorizado:

MaYan711/Sistema-Tienda-La-Esquina

GitHub Actions obtiene credenciales temporales mediante OIDC.

No existen llaves JSON permanentes almacenadas en GitHub.

---

## 18. IAM

Se aplica el principio de minimo privilegio.

La cuenta del backend tiene acceso solamente a los recursos que necesita para:

- conectarse a Cloud SQL;
- leer sus secretos.

La cuenta de GitHub Actions tiene permisos para:

- escribir imagenes en Artifact Registry;
- desplegar Cloud Run;
- utilizar las cuentas de servicio de ejecucion.

El frontend utiliza una cuenta de servicio independiente.

---

## 19. Rollback de Cloud Run

Listar revisiones del backend:

gcloud run revisions list \
  --service=tienda-backend \
  --project=tienda-la-esquina \
  --region=us-central1

Listar revisiones del frontend:

gcloud run revisions list \
  --service=tienda-frontend \
  --project=tienda-la-esquina \
  --region=us-central1

En caso de una version defectuosa se puede dirigir nuevamente el trafico hacia una revision anterior desde Cloud Run o mediante gcloud.

No eliminar la revision estable antes de comprobar la nueva version.

---

## 20. Recuperacion ante fallo

Si el frontend falla:

1. Revisar GitHub Actions.
2. Revisar logs de tienda-frontend.
3. Comprobar BACKEND_URL.
4. Comprobar la ultima revision desplegada.
5. Realizar rollback si es necesario.

Si el backend falla:

1. Revisar GitHub Actions.
2. Revisar logs de tienda-backend.
3. Comprobar Cloud SQL.
4. Comprobar Secret Manager.
5. Comprobar variables de entorno.
6. Revisar migraciones Flyway.
7. Realizar rollback si es necesario.

Si la base de datos falla:

1. Consultar estado de Cloud SQL.
2. Revisar conexiones.
3. Revisar credenciales.
4. Revisar logs del backend.
5. Restaurar desde respaldo si existe uno valido.

---

## 21. Presupuesto y costos

El proyecto utiliza un presupuesto de Google Cloud con alertas de consumo.

Umbrales recomendados:

50%
75%
90%
100%

Los presupuestos generan alertas pero no detienen automaticamente los recursos.

Los principales recursos cuyo consumo debe vigilarse son:

- Cloud SQL.
- Cloud Run.
- Artifact Registry.
- Cloud Storage.

---

## 22. Checklist posterior a despliegue

Despues de cada despliegue se debe comprobar:

- CI exitoso.
- Deploy GCP exitoso.
- Backend disponible.
- Frontend disponible.
- Login funcional.
- Conexion con Cloud SQL.
- Secret Manager funcional.
- Correo SMTP funcional.
- OTP y 2FA funcionales.
- Ventas funcionales.
- Reportes funcionales.
- terraform plan sin diferencias inesperadas.

---

## 23. Seguridad

La solucion utiliza:

- HTTPS administrado por Cloud Run.
- JWT.
- Autenticacion de dos factores.
- Secret Manager.
- IAM con minimo privilegio.
- Cuentas de servicio independientes.
- Workload Identity Federation.
- CI/CD automatizado.
- Terraform.
- Estado remoto protegido.
- Variables sensibles fuera del repositorio.

---

## 24. Responsabilidad de herramientas

Terraform administra la infraestructura.

GitHub Actions administra build y despliegue.

Artifact Registry almacena imagenes.

Cloud Run ejecuta frontend y backend.

Cloud SQL administra PostgreSQL.

Secret Manager almacena credenciales.

IAM controla permisos.

Google Cloud Storage almacena el estado remoto de Terraform.
