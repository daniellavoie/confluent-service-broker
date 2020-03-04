#!/bin/bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null && pwd )"

#set -o nounset \
#    -o errexit \
#    -o verbose

verify_installed()
{
  local cmd="$1"
  if [[ $(type $cmd 2>&1) =~ "not found" ]]; then
    echo -e "\nERROR: This script requires '$cmd'. Please install '$cmd' and run again.\n"
    exit 1
  fi
}
verify_installed "jq"
verify_installed "docker-compose"
verify_installed "keytool"

# Verify Docker memory is increased to at least 8GB
DOCKER_MEMORY=$(docker system info | grep Memory | grep -o "[0-9\.]\+")
if (( $(echo "$DOCKER_MEMORY 7.0" | awk '{print ($1 < $2)}') )); then
  echo -e "\nWARNING: Did you remember to increase the memory available to Docker to at least 8GB (default is 2GB)? Demo may otherwise not work properly.\n"
  sleep 3
fi

# Stop existing demo Docker containers
${DIR}/stop.sh

# Generate keys and certificates used for SSL
echo -e "Generate keys and certificates used for SSL"
(cd ${DIR}/security && ./certs-create.sh)

# Bring up Docker Compose
echo -e "Bringing up Docker Compose"
docker-compose -f $DIR/../docker-compose.yml up -d

# Verify Docker containers started
if [[ $(docker-compose -f $DIR/../docker-compose.yml ps) =~ "Exit 137" ]]; then
  echo -e "\nERROR: At least one Docker container did not start properly, see 'docker-compose ps'. Did you remember to increase the memory available to Docker to at least 8GB (default is 2GB)?\n"
  exit 1
fi