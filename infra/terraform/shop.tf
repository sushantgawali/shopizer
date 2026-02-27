resource "kubernetes_config_map" "shop" {
  metadata {
    name      = "shop-config"
    namespace = kubernetes_namespace.shopizer.metadata[0].name
  }
  data = {
    # Frontends run in the browser, so this must be the Mac-accessible URL
    APP_BASE_URL           = "http://localhost:${var.backend_nodeport}"
    APP_API_VERSION        = "/api/v1/"
    APP_MERCHANT           = "DEFAULT"
    APP_PRODUCT_GRID_LIMIT = "15"
    APP_PAYMENT_TYPE       = "STRIPE"
    APP_STRIPE_KEY         = ""
    APP_THEME_COLOR        = "#D1D1D1"
    APP_MAP_API_KEY        = ""
  }
}

resource "kubernetes_deployment" "shop" {
  metadata {
    name      = "shopizer-shop"
    namespace = kubernetes_namespace.shopizer.metadata[0].name
  }
  spec {
    replicas = 1
    selector {
      match_labels = { app = "shopizer-shop" }
    }
    template {
      metadata {
        labels = { app = "shopizer-shop" }
      }
      spec {
        container {
          name              = "shop"
          image             = var.shop_image
          image_pull_policy = "IfNotPresent"
          port {
            container_port = 80
          }
          env_from {
            config_map_ref {
              name = kubernetes_config_map.shop.metadata[0].name
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

resource "kubernetes_service" "shop" {
  metadata {
    name      = "shopizer-shop"
    namespace = kubernetes_namespace.shopizer.metadata[0].name
  }
  spec {
    selector = { app = "shopizer-shop" }
    type     = "NodePort"
    port {
      port        = 80
      target_port = 80
      node_port   = var.shop_nodeport
    }
  }
}
