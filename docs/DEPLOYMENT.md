# Deployment

Helm chart can be found in `chart` folder.

## Local K8s namespace - stage=local

Install a local K8s cluster, e.g. "Rancher Desktop" with

- Container Runtime = dockerd (moby)
- Kubernetes enabled
- Traefik enabled

## One-time setup

The helm chart expects the K8s namespace to contain some configurations as K8s opaque secrets:

- `rnr-<stage>-aip-config-properties`: Contains an `application.properties` file to overwrite default properties.
- `rnr-<stage>-aip-config-files`: Contains additional files to be referenced in application properties.

Create pull secrets:

- `rnr-<stage>-pull-secret-docker-public` for registry `docker-public.docker.devstack.vwgroup.com`
- `rnr-<stage>-pull-secret-rnr-docker` for registry `rnr-docker.docker.devstack.vwgroup.com`

Overwrite Application.properties in K8s secret:

- `app.rnr.base-url=http://rnr-aip:8080`

## Deployment to Local K8's

Login to Docker:

    docker login -u [vw_username] -p [vw_password] docker-public.docker.devstack.vwgroup.com

Build Docker image:

    ./gradlew build -x test && docker build -t rnr-aip:1 .

Install application:

    helm upgrade --install --set "image.name=rnr-aip:1" -f chart/values-local.yaml rnr-local-aip chart

Check dashboard of "Rancher Desktop" or try below command to verify the pod is running:

    kubectl get pods

Uninstall application:

    helm uninstall rnr-local-aip