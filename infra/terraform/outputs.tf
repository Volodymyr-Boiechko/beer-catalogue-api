output "cluster_name" {
  description = "EKS cluster name"
  value       = module.eks.cluster_name
}

output "cluster_endpoint" {
  description = "Kubernetes API server endpoint"
  value       = module.eks.cluster_endpoint
}

output "region" {
  description = "AWS region the stack was deployed to"
  value       = var.aws_region
}

output "configure_kubectl" {
  description = "Run this command after apply to configure kubectl"
  value       = "aws eks update-kubeconfig --region ${var.aws_region} --name ${module.eks.cluster_name}"
}

output "rds_endpoint" {
  description = "RDS hostname (without port). Paste into k8s/configmap.yaml as DB_HOST."
  value       = aws_db_instance.beer_catalogue.address
}

output "rds_port" {
  description = "RDS port"
  value       = aws_db_instance.beer_catalogue.port
}

output "jdbc_url" {
  description = "Full JDBC URL — matches the value DB_URL in k8s/configmap.yaml when DB_HOST is set"
  value       = "jdbc:postgresql://${aws_db_instance.beer_catalogue.address}:${aws_db_instance.beer_catalogue.port}/${var.db_name}"
}
