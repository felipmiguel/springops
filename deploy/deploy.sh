RESOURCE_GROUP="rg-asclab"
REGION="westeurope"
SPRING_CLOUD_SERVICE="fmiguelasclab"
SPRINGOPS_AGENT_NAME="springops-agent"
SPRINGOPS_AGENT_JAR="./springops-agent/target/springops-agent-1.0-SNAPSHOT.jar"



cd terraform
terraform init
terraform apply -var "asc_rg=${RESOURCE_GROUP}" -var "location=${REGION}" \
    -var "asc_service_name=${SPRING_CLOUD_SERVICE}" -var "springops_agent=${SPRINGOPS_AGENT_NAME}"
cd ..
cd ..
mvn clean package -DskipTests

az configure --defaults \
    group=${RESOURCE_GROUP} \
    location=${REGION} \
    spring-cloud=${SPRING_CLOUD_SERVICE}

az spring-cloud app deploy --name ${SPRINGOPS_AGENT_NAME} \
    --jar-path ${SPRINGOPS_AGENT_JAR} \
    --no-wait 
