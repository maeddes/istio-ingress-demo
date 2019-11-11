# istio-ingress-demo

Change to the root directory of this repo

Download:

`curl -L https://git.io/getLatestIstio | ISTIO_VERSION=1.3.4 sh -`

Add Helm Repo:

`helm repo add istio.io https://storage.googleapis.com/istio-release/releases/1.3.4/charts/`

`helm repo update`

Create namespace:

`kubectl create namespace istio-system`

Install CRDs:

`helm template install/kubernetes/helm/istio-init --name istio-init --namespace istio-system | kubectl apply -f -`

Validate:

`kubectl get crds | grep 'istio.io' | wc -l` -> 23

Install minimal Istio setup:

`helm template install/kubernetes/helm/istio --name istio --namespace istio-system \
    --values install/kubernetes/helm/istio/values-istio-minimal.yaml | kubectl apply -f -`

Validate:

Services:
```
kubectl get svc -n istio-system

NAME                     TYPE           CLUSTER-IP       EXTERNAL-IP   PORT(S)                                                                                                                                      AGE
istio-citadel            ClusterIP      10.103.196.124   <none>        8060/TCP,15014/TCP                                                                                                                           5m2s
istio-galley             ClusterIP      10.98.222.183    <none>        443/TCP,15014/TCP,9901/TCP                                                                                                                   5m3s
istio-ingressgateway     LoadBalancer   10.109.44.207    localhost     15020:31439/TCP,80:31380/TCP,443:31390/TCP,31400:31400/TCP,15029:30040/TCP,15030:31193/TCP,15031:31194/TCP,15032:31458/TCP,15443:30839/TCP   5m3s
istio-pilot              ClusterIP      10.98.15.252     <none>        15010/TCP,15011/TCP,8080/TCP,15014/TCP                                                                                                       5m2s
istio-policy             ClusterIP      10.96.195.190    <none>        9091/TCP,15004/TCP,15014/TCP                                                                                                                 5m2s
istio-sidecar-injector   ClusterIP      10.99.10.38      <none>        443/TCP,15014/TCP                                                                                                                            5m2s
istio-telemetry          ClusterIP      10.111.42.165    <none>        9091/TCP,15004/TCP,15014/TCP,42422/TCP                                                                                                       5m2s
prometheus               ClusterIP      10.108.79.243    <none>        9090/TCP                                                                                                                                     5m2s
```

Pods:
```
kubectl get pods -n istio-system

NAME                                      READY   STATUS      RESTARTS   AGE
istio-citadel-555bff45bf-trdd9            1/1     Running     0          10m
istio-galley-7f8b6db7d7-nnf8l             1/1     Running     0          10m
istio-ingressgateway-5ddbcf57f4-q5lkw     1/1     Running     0          10m
istio-init-crd-10-1.3.4-fnflq             0/1     Completed   0          13m
istio-init-crd-11-1.3.4-tw6cp             0/1     Completed   0          13m
istio-init-crd-12-1.3.4-5r758             0/1     Completed   0          13m
istio-pilot-76d457885b-pf2z7              2/2     Running     0          10m
istio-policy-598564cc7d-bzrx7             2/2     Running     3          10m
istio-security-post-install-1.3.4-s5m4k   0/1     Completed   0          10m
istio-sidecar-injector-65d5f8db56-fkhpl   1/1     Running     0          10m
istio-telemetry-59bf8b5b5f-xg4sw          2/2     Running     3          10m
prometheus-7d7b9f7844-8klnp               1/1     Running     0          10m
```

Build the app:

`RestEndpoint/mvnw clean install -f RestEndpoint/pom.xml`

Build the container:

`docker build -t maeddes/dynamic-api-app:v0.1 .`

Output validation:

```
Sending build context to Docker daemon  19.59MB
Step 1/5 : FROM openjdk:8-jre-alpine
 ---> f7a292bbb70c
Step 2/5 : RUN mkdir -p /opt/RestEndpoint
 ---> Using cache
 ---> fc7d4c4fdf8b
Step 3/5 : WORKDIR /opt/RestEndpoint
 ---> Using cache
 ---> 4103e23341d1
Step 4/5 : COPY RestEndpoint/target/RestEndpoint-0.0.1-SNAPSHOT.jar /opt/RestEndpoint
 ---> 39b8327dc6f3
Step 5/5 : CMD ["java", "-jar", "RestEndpoint-0.0.1-SNAPSHOT.jar"]
 ---> Running in 98552e3d2558
Removing intermediate container 98552e3d2558
 ---> 8288a535e158
Successfully built 8288a535e158
Successfully tagged maeddes/dynamic-api-app:v0.1
```

Kubernetes deployment:

```
kubectl apply -f kubernetes/deploy-v1.yml
deployment.extensions/apiapp created
```

Check pods:

```
kubectl get pods
NAME                      READY   STATUS    RESTARTS   AGE
apiapp-74549b849b-cn2sg   1/1     Running   0          2m4s
```

Port-forward validation (needs replacement with own pod name):

```
kubectl port-forward apiapp-74549b849b-cn2sg 8080:8080
Forwarding from 127.0.0.1:8080 -> 8080
Forwarding from [::1]:8080 -> 8080
```

In separate terminal:

```
curl localhost:8080/test
this is static endpoint /test. api.endpoint is set to: /matthias

curl localhost:8080/matthias
saying Hello from: /matthias
```

The API/REST Endpoint is set in the deploy-v1.yml file.
The `API_ENDPOINT` variable specifies the endpoint. Requires redeployment.

```
    spec:
      containers:
      - env:
        - name: API_ENDPOINT
          value: /matthias
``` 

Expose deployment as ClusterIP service:

```
kubectl expose deploy apiapp --port=8080
service/apiapp exposed
```

Deployment of Istio gateway:

```
kubectl apply -f istio/gateway.yml
gateway.networking.istio.io/demo-gateway created
````

content of file:

```yaml
apiVersion: networking.istio.io/v1alpha3
kind: Gateway
metadata:
  name: demo-gateway
spec:
  selector:
    istio: ingressgateway
  servers:
  - hosts:
    - '*'
    port:
      name: http
      number: 80
      protocol: HTTP
 ```
 
Deployment of Istio virtual service:
 
```
kubectl apply -f istio/virtualservice.yml
virtualservice.networking.istio.io/demo-virtualservice created
````

content of file:

```yaml
apiVersion: networking.istio.io/v1alpha3
kind: VirtualService
metadata:
  name: demo-virtualservice
spec:
  hosts:
  - '*'
  gateways:
  - demo-gateway
  http:
  - match:
    - uri:
        prefix: /
    route:
    - destination:
        host: apiapp.default.svc.cluster.local
        port:
          number: 8080
```

The output of `kubectl get deployment,service,gateway,virtualservice`should look like this:

```
NAME                           READY   UP-TO-DATE   AVAILABLE   AGE
deployment.extensions/apiapp   1/1     1            1           18h

NAME                 TYPE        CLUSTER-IP     EXTERNAL-IP   PORT(S)    AGE
service/apiapp       ClusterIP   10.104.199.1   <none>        8080/TCP   2m43s
service/kubernetes   ClusterIP   10.96.0.1      <none>        443/TCP    18h

NAME                                       AGE
gateway.networking.istio.io/demo-gateway   33m

NAME                                                     GATEWAYS         HOSTS   AGE
virtualservice.networking.istio.io/demo-virtualservice   [demo-gateway]   [*]     20m
```

With this virtual service configuration all endpoints under / will be mapped.

To find out the IP/hostname of your Istio Ingress gateway execute 

'kubectl get svc istio-ingressgateway -n istio-system -o json'

At the end of the statement you should see

```json
"status": {
        "loadBalancer": {
            "ingress": [
                {
                    "hostname": "localhost"
                }
            ]
        }
    }
```

In case of Docker for Windows/Mac, it will be "localhost", otherwise you will get an IP address.
This should let you curl the endpoints without port specification.

```bash
curl localhost/matthias
saying Hello from: /matthias

curl localhost/test
this is static endpoint /test. api.endpoint is set to: /matthias
```






