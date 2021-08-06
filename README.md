# SpringOps
GitOps Lab implementation for Azure Spring Cloud.

This solution consists of an agent that can be deployed as an application in an Azure Spring Cloud (ASC) service. The agent reads Application Deployment definitions from a given GIT repository and deploys to the ASC Service.

The agent retrieves changes from GIT periodically and applies the configuration to the ASC deployed applications.
A sample Application Deployment definition:
```yaml
appName: "springopsdemo"
version: "2.0"
artifactsSource: "https://publicserver.org/releases/spring-cloud-microservice-0.0.1-SNAPSHOT.jar"
blueGreen: true
instanceCount: 2
environmentVariables:
  env1: "envvalue1"
cpu: 1
memoryInGb: 1
jvmOptions: "-Xms512m"
isPublic: true
identity: false
runtime: "Java_11"
httpsOnly: false
endToEndTls: true
persistentDisk:
  sizeInGb: 5
  mountPath: "/persistent"
```
The agent is able to perform a blue/green deployment. The behavior is:
1. Create a Azure Spring Cloud application deployment
2. Deploy the application in that deployment slot.
3. Validate the deployment by making requests to application. Actually only the health endpoint (requires to configure actuator). Ideally a request to the an endpoint of the given application.
4. If valid, set the deployment as active.
5. Remove the other Azure Spring Cloud application deployments.

For this lab, the artifact should be accessible in a public location. Ideally it could be a private location such as a private Azure Storage Blob, GitHub releases, Azure DevOps published artifact.

The agent is compatible with Azure Spring Cloud configuration service and some parts of the application configuration can be dinamically modified without agent restarts.

Sample application.yaml configuration
```yaml
spring:
    application:
        name: "spring-ops"
    cloud:
        config:
            auto-refresh: true
            refresh-interval: 60
springops:
    git:
        uri: https://github.com/<your-account>/<your-repo>.git
        user: 
        password:  
        baseDirectory: /tmp/springops
        frequency: "* * * * *" # Cron expression for git pulling frequency. This value is every minute
```

## How it works
The agent uses Azure Resource Management API to manage the applications. The agent should run with a managed identity and that identity should have permissions over the Azure Spring Cloud service instance.

There is job that checks the GIT repo for commits. If detects a new commit it enqueue a new deployment.

The deployment agent checks if the application exists. If it exists, retrieves the actual configuration and determines if it requires an update. It only applies the required changes.
It the application doesn't exist in Azure Spring Cloud it creates a new application and deploys it.

The job scheduling is implemented using [Jobrunr](https://www.jobrunr.io/).

## Deployment
Build the JAR package and deploy as a regular Azure Spring Cloud application. 
The following script assumes that you have an Azure subscription with an existing Azure Spring Cloud Service and az-cli installed locally.
```bash
RESOURCE_GROUP="<your resource group>"
SPRING_CLOUD_SERVICE="<your azure spring cloud service>"
SPRINGOPS_AGENT_NAME="springops-agent"
SPRINGOPS_AGENT_JAR="./springops-agent/target/springops-agent-1.0-SNAPSHOT.jar"

az configure --defaults \
    group=${RESOURCE_GROUP} \
    spring-cloud=${SPRING_CLOUD_SERVICE}

az spring-cloud app create --name ${SPRINGOPS_AGENT_NAME} \
    --runtime-version Java_11 --assign-identity true

# retrieve application managed identity
APP_IDENTITY=$(az spring-cloud app show --name ${SPRINGOPS_AGENT_NAME} \
    --query "identity.principalId" --output tsv)

# retrieve Spring Cloud Service id
ASC_ID=$(az spring-cloud show -n ${SPRING_CLOUD_SERVICE} --query id --output tsv)

az role assignment create --assignee ${APP_IDENTITY} --role "Contributor" --scope ${ASC_ID}

az spring-cloud app deploy --name ${SPRINGOPS_AGENT_NAME} \
    --jar-path ${SPRINGOPS_AGENT_JAR} \
    --no-wait 
```
If the application fails at starting most probably the application is not yet configured. See sample application.yaml above. Azure Spring Cloud config server can be used for configuration. 

If you don't have yet an Azure Spring Cloud service deployed you can use the script [deploy.sh](./deploy/deploy.sh). It uses terraform to deploy a new Azure Spring Cloud Service and the agent application.