variable "mysql_root_password" {
  description = "MySQL root password"
  type        = string
  default     = "shopizer_root"
  sensitive   = true
}

variable "mysql_database" {
  description = "MySQL database name (must match Spring Boot config)"
  type        = string
  default     = "SALESMANAGER"
}

variable "backend_image" {
  description = "Local Docker image for the Spring Boot backend"
  type        = string
  default     = "shopizer-backend:latest"
}

variable "admin_image" {
  description = "Local Docker image for the Angular admin panel"
  type        = string
  default     = "shopizer-admin:latest"
}

variable "shop_image" {
  description = "Local Docker image for the React storefront"
  type        = string
  default     = "shopizer-shop:latest"
}

# NodePorts — must be in range 30000-32767
variable "backend_nodeport" {
  description = "NodePort exposed on the Mac for the backend API"
  type        = number
  default     = 30080
}

variable "admin_nodeport" {
  description = "NodePort exposed on the Mac for the admin panel"
  type        = number
  default     = 30042
}

variable "shop_nodeport" {
  description = "NodePort exposed on the Mac for the React shop"
  type        = number
  default     = 30030
}
