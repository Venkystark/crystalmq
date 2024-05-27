package com.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import io.kubernetes.client.custom.IntOrString;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.apis.NetworkingV1Api;
import io.kubernetes.client.openapi.models.*;




public class KubeManager {

	static AppsV1Api appsApi = null;
	static CoreV1Api coreApi=null;
	static NetworkingV1Api networkApi=null;

	// String deploymentName = "deployment-1";
	// String imageName = "nginx";
	// String namespace = "default";

	public KubeManager() throws IOException, ApiException  { 

		appsApi = KubeConnect.getAPIInstance();
		coreApi=KubeConnect.getCoreV1APIInstance();
		networkApi=KubeConnect.getNetworkingV1APIInstance();
	}

	//added missing labels
	public void createDeployment(String deploymentName, String namespace, String imageName) throws ApiException {

    V1Deployment deployment = new V1Deployment();
    deployment.setApiVersion("apps/v1");
    deployment.setKind("Deployment");

    V1DeploymentSpec spec = new V1DeploymentSpec();
    spec.setReplicas(1);

    V1ObjectMeta metadata = new V1ObjectMeta();
    metadata.setName(deploymentName);
    metadata.setNamespace(namespace);
    deployment.setMetadata(metadata);

    // Set selector
    V1LabelSelector labelSelector = new V1LabelSelector();
    labelSelector.putMatchLabelsItem("app", deploymentName);
    spec.setSelector(labelSelector);

    // Set template labels
    V1ObjectMeta templateMetadata = new V1ObjectMeta();
    templateMetadata.setLabels(Collections.singletonMap("app", deploymentName));
    V1PodTemplateSpec templateSpec = new V1PodTemplateSpec();
    templateSpec.setMetadata(templateMetadata);

    // Set container
    V1Container container = new V1Container();
    container.setImage(imageName);
    container.setName(deploymentName);
	
    // Set the ports for the container to run
    List<V1ContainerPort> containerPorts = new ArrayList<>();

    V1ContainerPort containerPort1 = new V1ContainerPort();
    containerPort1.setContainerPort(8080); // Port 8080
    containerPorts.add(containerPort1);

    V1ContainerPort containerPort2 = new V1ContainerPort();
    containerPort2.setContainerPort(8081); // Port 8081
    containerPorts.add(containerPort2);

    V1ContainerPort containerPort3 = new V1ContainerPort();
    containerPort3.setContainerPort(1883); // Port 1883
    containerPorts.add(containerPort3);

    container.setPorts(containerPorts);

    V1PodSpec podSpec = new V1PodSpec();
    podSpec.setContainers(Collections.singletonList(container));
    templateSpec.setSpec(podSpec);

    spec.setTemplate(templateSpec);
    deployment.setSpec(spec);

    // Create deployment
    appsApi.createNamespacedDeployment(namespace, deployment, null, null, null, null);

    System.out.println(deploymentName+" success");
}
//creating service
public void createService(String serviceName, String deploymentName, String namespace,int serviceport) throws ApiException {
    // Create a Service object
	String servicetype="";
	int TargetPort=0;
	int NodePort=30008;
	if(serviceName.contains("tcp-service") || serviceName.contains("ws-mqtt-service")) {
	KubePortFinder kpf=new KubePortFinder();
	servicetype="NodePort";
	NodePort=kpf.findFreeNodePort(coreApi);
    if(serviceName.contains("tcp-service"))
	TargetPort=1883;
    else if(serviceName.contains("ws-mqtt-service"))
    TargetPort=10433;
	}
	else if(serviceName.contains("ui-service")) {
	servicetype="ClusterIP";
	TargetPort=8080;
	}
	else if(serviceName.contains("ws-service")) {
		servicetype="ClusterIP";
		TargetPort=8081;
	}
    V1Service service = new V1Service();
    service.setApiVersion("v1");
    service.setKind("Service");

    // Set metadata
    V1ObjectMeta metadata = new V1ObjectMeta();
    metadata.setName(serviceName);
    metadata.setNamespace(namespace);
    service.setMetadata(metadata);

    // Set service spec
    V1ServiceSpec spec = new V1ServiceSpec();
    spec.setType(servicetype);

    // Set service port
    V1ServicePort servicePort = new V1ServicePort();
    servicePort.setPort(serviceport); // The port on which the service will be exposed
    servicePort.setTargetPort(new IntOrString(TargetPort)); // The port on the pod to which traffic will be forwarded
	if(servicetype.equals("NodePort"))
    servicePort.setNodePort(NodePort); // Specify the desired NodePort

    // Add the port to the service spec
    spec.setPorts(Collections.singletonList(servicePort));
    spec.setSelector(Collections.singletonMap("app", deploymentName)); // Selector to match pods with the label 'app=<deploymentName>'

    service.setSpec(spec);

    // Create the service in the specified namespace
    coreApi.createNamespacedService(namespace, service, null, null, null, "Strict");

    // Print success message
    System.out.println(serviceName+"Service creation success");
}
public void addNewHostToIngress(String ingressName, String namespace, String newHost, String wsServiceName, int wsServicePort, String httpServiceName, int httpServicePort,String tlsSecretName) {
    try {
        // Use NetworkingV1Api for Ingress resources
        V1Ingress ingress = networkApi.readNamespacedIngress(ingressName, namespace, null);

        // Create a new IngressRule for the new host
        V1IngressRule newIngressRule = new V1IngressRule();
        newIngressRule.setHost(newHost);

        // Create a list of paths for the new IngressRule
        List<V1HTTPIngressPath> paths = new ArrayList<>();

        // Define the path for the ws service
        V1HTTPIngressPath wsPath = new V1HTTPIngressPath();
        wsPath.setPath("/ws");
        wsPath.setPathType("Prefix");

        V1IngressBackend wsBackend = new V1IngressBackend();
        V1IngressServiceBackend wsServiceBackend = new V1IngressServiceBackend();
        wsServiceBackend.setName(wsServiceName);

        V1ServiceBackendPort wsServiceBackendPort = new V1ServiceBackendPort();
        wsServiceBackendPort.setNumber(wsServicePort);
        wsServiceBackend.setPort(wsServiceBackendPort);

        wsBackend.setService(wsServiceBackend);
        wsPath.setBackend(wsBackend);
        paths.add(wsPath);

        // Define the path for the http service
        V1HTTPIngressPath httpPath = new V1HTTPIngressPath();
        httpPath.setPath("/");
        httpPath.setPathType("Prefix");

        V1IngressBackend httpBackend = new V1IngressBackend();
        V1IngressServiceBackend httpServiceBackend = new V1IngressServiceBackend();
        httpServiceBackend.setName(httpServiceName);

        V1ServiceBackendPort httpServiceBackendPort = new V1ServiceBackendPort();
        httpServiceBackendPort.setNumber(httpServicePort);
        httpServiceBackend.setPort(httpServiceBackendPort);

        httpBackend.setService(httpServiceBackend);
        httpPath.setBackend(httpBackend);
        paths.add(httpPath);

        V1HTTPIngressRuleValue httpIngressRuleValue = new V1HTTPIngressRuleValue();
        httpIngressRuleValue.setPaths(paths);
        newIngressRule.setHttp(httpIngressRuleValue);

        // Add the new IngressRule to the existing list of rules
        if (ingress.getSpec().getRules() == null) {
            ingress.getSpec().setRules(new ArrayList<V1IngressRule>());
        }
        ingress.getSpec().getRules().add(newIngressRule);

        // Create a new TLS configuration for the new host
        V1IngressTLS newTls = new V1IngressTLS();
        newTls.setHosts(new ArrayList<String>() {{ add(newHost); }});
        newTls.setSecretName(tlsSecretName);

        // Add the new TLS configuration to the existing list of tls configurations
        if (ingress.getSpec().getTls() == null) {
            ingress.getSpec().setTls(new ArrayList<V1IngressTLS>());
        }
        ingress.getSpec().getTls().add(newTls);

        // Update the Ingress resource
        networkApi.replaceNamespacedIngress(ingressName, namespace, ingress, null, null, null, null);

        System.out.println("Ingress updated successfully.");
    } catch (Exception e) {
        System.err.println("Error occurred while updating Ingress: " + e.getMessage());
        e.printStackTrace();
    }
}

	public void listDetails() 
	{

	}

	public boolean destroyDeployment(String deploymentName) 
	{



		return false;

	}



	// For testing purpose. 

	public static void main(String[] args)  
	{
		try { 
			KubeManager kmr = new KubeManager();
			//KubePortFinder kpf=new KubePortFinder();
			BufferedReader br=new BufferedReader(new InputStreamReader(System.in));
			System.out.println("Enter Domain Name");
			String company=br.readLine();
			String Deployment_name=company+"-deployment";
			// String Image_name=br.readLine();
			// kmr.createDeployment(Deployment_name, "ingress-nginx","venkystark/route:latest");
            KubeTest kt=new KubeTest();
            kt.createDeployment(Deployment_name,"ingress-nginx","venkystark/route:latest");
			String ui_service=Deployment_name+"-ui-service";
			String ws_service=Deployment_name+"-ws-service";
			String tcp_service=Deployment_name+"-tcp-service";
            String ws_mqtt_service=Deployment_name+"-ws-mqtt-service";
			// int ui_port=kpf.findFreeServicePort(coreApi);
			// int ws_port=kpf.findFreeServicePort(coreApi);
			// int tcp_port=kpf.findFreeServicePort(coreApi);
            // int ws_mqtt_port=kpf.findFreeServicePort(coreApi);
            int ui_port=5;
			int ws_port=6;
			int tcp_port=7;
            int ws_mqtt_port=8;
			kmr.createService(ui_service,Deployment_name,"ingress-nginx",ui_port);
			kmr.createService(ws_service, Deployment_name,"ingress-nginx", ws_port);
			kmr.createService(tcp_service, Deployment_name,"ingress-nginx", tcp_port);
            kmr.createService(ws_mqtt_service, Deployment_name, "ingress-nginx", ws_mqtt_port);
			kmr.addNewHostToIngress("broker-ingress", "ingress-nginx", company+".sfo.mqttserver.com", ws_service, ws_port, ui_service, ui_port,"tls-secret"); //ssl
		}
		catch(ApiException e) { 
			System.out.println(e.getResponseBody());
			e.printStackTrace();
		}
		catch(Exception exp) { 
			exp.printStackTrace();
		}

	}

}
