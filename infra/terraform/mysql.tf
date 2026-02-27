resource "kubernetes_secret" "mysql" {
  metadata {
    name      = "mysql-secret"
    namespace = kubernetes_namespace.shopizer.metadata[0].name
  }
  data = {
    root-password = var.mysql_root_password
  }
}

resource "kubernetes_persistent_volume_claim" "mysql" {
  metadata {
    name      = "mysql-pvc"
    namespace = kubernetes_namespace.shopizer.metadata[0].name
  }
  # wait_until_bound = false is required for WaitForFirstConsumer storage class
  # (local-path in k3s). The PVC binds only once a pod is scheduled — Terraform
  # must not block here or it creates a deadlock with the StatefulSet.
  wait_until_bound = false
  spec {
    access_modes = ["ReadWriteOnce"]
    resources {
      requests = {
        storage = "5Gi"
      }
    }
  }
}

resource "kubernetes_stateful_set" "mysql" {
  metadata {
    name      = "mysql"
    namespace = kubernetes_namespace.shopizer.metadata[0].name
  }
  spec {
    service_name = "mysql"
    replicas     = 1
    selector {
      match_labels = { app = "mysql" }
    }
    template {
      metadata {
        labels = { app = "mysql" }
      }
      spec {
        container {
          name  = "mysql"
          image = "mysql:8.0"
          port {
            container_port = 3306
          }
          env {
            name = "MYSQL_ROOT_PASSWORD"
            value_from {
              secret_key_ref {
                name = kubernetes_secret.mysql.metadata[0].name
                key  = "root-password"
              }
            }
          }
          env {
            name  = "MYSQL_DATABASE"
            value = var.mysql_database
          }
          volume_mount {
            name       = "mysql-data"
            mount_path = "/var/lib/mysql"
          }
          readiness_probe {
            exec {
              command = ["mysqladmin", "ping", "-h", "localhost"]
            }
            initial_delay_seconds = 20
            period_seconds        = 5
            failure_threshold     = 10
          }
        }
        volume {
          name = "mysql-data"
          persistent_volume_claim {
            claim_name = kubernetes_persistent_volume_claim.mysql.metadata[0].name
          }
        }
      }
    }
  }
}

# Headless service lets the backend resolve "mysql" within the namespace
resource "kubernetes_service" "mysql" {
  metadata {
    name      = "mysql"
    namespace = kubernetes_namespace.shopizer.metadata[0].name
  }
  spec {
    selector   = { app = "mysql" }
    cluster_ip = "None"
    port {
      port        = 3306
      target_port = 3306
    }
  }
}
