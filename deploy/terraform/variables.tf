variable "location" {
  type        = string
  description = "Azure region to host all services"
  default     = "westeurope"
}

variable "asc_rg" {
  type        = string
  description = "resource group that contains Azure Spring Cloud deployment"
}

variable "asc_service_name" {
  type        = string
  description = "Azure Spring Cloud service name. It should be unique in the world, so it is a good idea to add your alias in the name"
}

variable "config_repo_uri" {
  type        = string
  description = "repository that hosts the configuration"

}

variable "config_repo_username" {
  type        = string
  description = "username of githu configuration repository"
}

variable "config_repo_pat" {
  type        = string
  description = "personal access token for github configuration repository"
}

##

variable "springops_agent" {
  type=string
  description= "SpringOps agent application name"
  default = "springops-agent"
}
