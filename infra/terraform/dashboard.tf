# Long-lived token for the Kubernetes Dashboard (or any k8s UI)
#
# In Kubernetes 1.24+ short-lived projected tokens are the default.
# A Secret of type kubernetes.io/service-account-token creates a
# non-expiring token that is suitable for dashboard access.

resource "kubernetes_service_account" "dashboard_admin" {
  metadata {
    name      = "dashboard-admin"
    namespace = "kube-system"
  }
}

resource "kubernetes_cluster_role_binding" "dashboard_admin" {
  metadata {
    name = "dashboard-admin"
  }

  role_ref {
    api_group = "rbac.authorization.k8s.io"
    kind      = "ClusterRole"
    name      = "cluster-admin"
  }

  subject {
    kind      = "ServiceAccount"
    name      = kubernetes_service_account.dashboard_admin.metadata[0].name
    namespace = kubernetes_service_account.dashboard_admin.metadata[0].namespace
  }
}

resource "kubernetes_secret" "dashboard_admin_token" {
  metadata {
    name      = "dashboard-admin-token"
    namespace = "kube-system"
    annotations = {
      "kubernetes.io/service-account.name" = kubernetes_service_account.dashboard_admin.metadata[0].name
    }
  }

  type = "kubernetes.io/service-account-token"

  # Kubernetes populates .data.token automatically after Secret creation
  wait_for_service_account_token = true
}
