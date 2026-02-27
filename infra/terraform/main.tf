terraform {
  required_version = ">= 1.5"
  required_providers {
    kubernetes = {
      source  = "hashicorp/kubernetes"
      version = "~> 2.27"
    }
  }
}

# Colima writes its kubeconfig to ~/.kube/config with context "colima"
provider "kubernetes" {
  config_path    = "~/.kube/config"
  config_context = "colima"
}

resource "kubernetes_namespace" "shopizer" {
  metadata {
    name = "shopizer"
  }
}
