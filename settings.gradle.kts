rootProject.name = "ec-api"

include("core", "backend", "batch")

if (file("gatling").exists()) {
    include("gatling")
}
