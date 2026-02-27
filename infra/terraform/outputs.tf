output "shop_url" {
  description = "React storefront"
  value       = "http://localhost:${var.shop_nodeport}"
}

output "admin_url" {
  description = "Angular admin panel"
  value       = "http://localhost:${var.admin_nodeport}"
}

output "api_url" {
  description = "Spring Boot REST API"
  value       = "http://localhost:${var.backend_nodeport}"
}

output "swagger_url" {
  description = "Swagger UI"
  value       = "http://localhost:${var.backend_nodeport}/swagger-ui.html"
}

output "dashboard_token" {
  description = "Long-lived bearer token for the Kubernetes dashboard"
  value       = kubernetes_secret.dashboard_admin_token.data["token"]
  sensitive   = true
}
