resource "kubernetes_config_map" "backend" {
  metadata {
    name      = "backend-config"
    namespace = kubernetes_namespace.shopizer.metadata[0].name
  }
  data = {
    SPRING_PROFILES_ACTIVE = "mysql"
    DB_HOST                = "mysql"   # resolves via headless Service in same namespace
    DB_PORT                = "3306"
    DB_NAME                = var.mysql_database
    DB_USER                = "root"
    DB_JDBC_URL            = "jdbc:mysql://mysql:3306/${var.mysql_database}?autoReconnect=true&serverTimeZone=UTC&useUnicode=true&characterEncoding=UTF-8"
    JAVA_OPTS              = "-Xmx512m -Xms256m"
  }
}

resource "kubernetes_deployment" "backend" {
  metadata {
    name      = "shopizer-backend"
    namespace = kubernetes_namespace.shopizer.metadata[0].name
  }
  spec {
    replicas = 1
    selector {
      match_labels = { app = "shopizer-backend" }
    }
    template {
      metadata {
        labels = { app = "shopizer-backend" }
      }
      spec {
        # Wait for MySQL TCP port before starting the JVM
        init_container {
          name  = "wait-for-mysql"
          image = "busybox:1.36"
          command = [
            "sh", "-c",
            "until nc -z mysql 3306; do echo 'Waiting for MySQL...'; sleep 3; done; echo 'MySQL is up'"
          ]
        }
        container {
          name              = "shopizer"
          image             = var.backend_image
          image_pull_policy = "IfNotPresent"
          command           = ["sh", "-c"]
          args              = ["java -Xmx512m -Xms256m -Dsearch.noindex=true -Ddb.jdbcUrl=\"$DB_JDBC_URL\" -Ddb.user=\"$DB_USER\" -Ddb.password=\"$DB_PASSWORD\" -jar shopizer.jar"]
          port {
            container_port = 8080
          }
          env_from {
            config_map_ref {
              name = kubernetes_config_map.backend.metadata[0].name
            }
          }
          env {
            name = "DB_PASSWORD"
            value_from {
              secret_key_ref {
                name = kubernetes_secret.mysql.metadata[0].name
                key  = "root-password"
              }
            }
          }
          # Spring Boot startup is slow; give it 5 minutes before giving up
          readiness_probe {
            tcp_socket {
              port = 8080
            }
            initial_delay_seconds = 60
            period_seconds        = 10
            failure_threshold     = 30
          }
          resources {
            requests = {
              memory = "512Mi"
              cpu    = "250m"
            }
            limits = {
              memory = "1Gi"
              cpu    = "1"
            }
          }
        }
      }
    }
  }
}

resource "kubernetes_service" "backend" {
  metadata {
    name      = "shopizer-backend"
    namespace = kubernetes_namespace.shopizer.metadata[0].name
  }
  spec {
    selector = { app = "shopizer-backend" }
    type     = "NodePort"
    port {
      port        = 8080
      target_port = 8080
      node_port   = var.backend_nodeport
    }
  }
}
