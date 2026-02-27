resource "kubernetes_config_map" "admin" {
  metadata {
    name      = "admin-config"
    namespace = kubernetes_namespace.shopizer.metadata[0].name
  }
  data = {
    # Frontends run in the browser, so this must be the Mac-accessible URL
    APP_BASE_URL = "http://localhost:${var.backend_nodeport}/api"
  }
}

resource "kubernetes_deployment" "admin" {
  metadata {
    name      = "shopizer-admin"
    namespace = kubernetes_namespace.shopizer.metadata[0].name
  }
  spec {
    replicas = 1
    selector {
      match_labels = { app = "shopizer-admin" }
    }
    template {
      metadata {
        labels = { app = "shopizer-admin" }
      }
      spec {
        container {
          name              = "admin"
          image             = var.admin_image
          image_pull_policy = "IfNotPresent"
          port {
            container_port = 80
          }
          env_from {
            config_map_ref {
              name = kubernetes_config_map.admin.metadata[0].name
            }
          }
          readiness_probe {
            http_get {
              path = "/"
              port = 80
            }
            initial_delay_seconds = 10
            period_seconds        = 5
          }
          resources {
            requests = {
              memory = "64Mi"
              cpu    = "50m"
            }
            limits = {
              memory = "128Mi"
              cpu    = "200m"
            }
          }
        }
      }
    }
  }
}

resource "kubernetes_service" "admin" {
  metadata {
    name      = "shopizer-admin"
    namespace = kubernetes_namespace.shopizer.metadata[0].name
  }
  spec {
    selector = { app = "shopizer-admin" }
    type     = "NodePort"
    port {
      port        = 80
      target_port = 80
      node_port   = var.admin_nodeport
    }
  }
}
