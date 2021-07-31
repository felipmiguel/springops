provider "azurerm" {
  features {}
}


## Azure Spring Cloud resources 
resource "azurerm_resource_group" "rg_asc" {
  name     = var.asc_rg
  location = var.location
}

locals {
  app_insights_name  = "pcsms-appinsights-${var.asc_rg}"
  log_analytics_name = "pcsms-log-${var.asc_rg}"
}

resource "azurerm_spring_cloud_service" "asc_service" {
  name                = var.asc_service_name
  resource_group_name = azurerm_resource_group.rg_asc.name
  location            = azurerm_resource_group.rg_asc.location

  config_server_git_setting {
    uri          = var.config_repo_uri
    label        = "main"
    search_paths = ["."]
    http_basic_auth {
      username = var.config_repo_username
      password = var.config_repo_pat
    }
  }

  trace {
    instrumentation_key = azurerm_application_insights.appinsights.instrumentation_key
  }
}

resource "azurerm_application_insights" "appinsights" {
  name                = local.app_insights_name
  location            = azurerm_resource_group.rg_asc.location
  resource_group_name = azurerm_resource_group.rg_asc.name

  application_type = "java"
}

resource "azurerm_spring_cloud_app" "springops_agent" {
  name                = var.springops_agent
  resource_group_name = azurerm_resource_group.rg_asc.name
  service_name        = azurerm_spring_cloud_service.asc_service.name
  identity {
    type = "SystemAssigned"
  }
}

resource "azurerm_spring_cloud_java_deployment" "springops_agent_deployment" {
  # name                = "${var.vets_service}-deployment"
  name                = "default"
  spring_cloud_app_id = azurerm_spring_cloud_app.springops_agent.id
  cpu                 = 1
  instance_count      = 1
  memory_in_gb        = 1
  jvm_options         = "-Xmx1024m"
  runtime_version     = "Java_11"
  environment_variables = {
    "spring.profiles.active": "lab" 
  }
}

resource "azurerm_spring_cloud_active_deployment" "springops_agent_deployment" {
  spring_cloud_app_id = azurerm_spring_cloud_app.springops_agent.id
  deployment_name     = azurerm_spring_cloud_java_deployment.springops_agent_deployment.name
}

resource "azurerm_role_assignment" "springops_contributor_role" {
  principal_id         = azurerm_spring_cloud_app.springops_agent.identity[0].principal_id
  role_definition_name = "Contributor"
  scope                = azurerm_resource_group.rg_asc.id
}