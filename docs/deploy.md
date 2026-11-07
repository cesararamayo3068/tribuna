# Demo en Render, Neon y Vercel

1. En Neon creá una base y un rol exclusivos para esta demo. Usá una conexión directa y conservá el host, la base, el usuario y la contraseña.
2. En Render creá un Blueprint a partir de este repositorio. Cargá **SPRING_DATASOURCE_URL** con formato jdbc:postgresql://HOST:5432/BASE?sslmode=require, **SPRING_DATASOURCE_USERNAME** y **SPRING_DATASOURCE_PASSWORD** con los datos de Neon. El servicio usa el perfil demo y su endpoint de salud es /api/health.
3. En Vercel importá el repositorio, elegí **frontend** como Root Directory y Node.js 24. Configurá **RENDER_API_ORIGIN** con el origen HTTPS que asignó Render. La configuración del repositorio compila Angular y reenvía /api al backend.

La demo de Tribuna permite consultar el torneo y el análisis de socios ficticios. El backend rechaza escrituras, incluida la subida de CSV. Docker Compose conserva la edición y carga de archivos para uso local.
