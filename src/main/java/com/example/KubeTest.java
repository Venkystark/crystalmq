package com.example;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.models.V1Container;
import io.kubernetes.client.openapi.models.V1ContainerPort;
import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.openapi.models.V1DeploymentSpec;
import io.kubernetes.client.openapi.models.V1LabelSelector;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1PersistentVolumeClaimVolumeSource;
import io.kubernetes.client.openapi.models.V1PodSpec;
import io.kubernetes.client.openapi.models.V1PodTemplateSpec;
import io.kubernetes.client.openapi.models.V1Volume;
import io.kubernetes.client.openapi.models.V1VolumeMount;

public class KubeTest {
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
    // templateMetadata.setAnnotations(Collections.singletonMap("nodeSelector", "node-name=node1"));
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

    V1ContainerPort containerPort4=new V1ContainerPort();
    containerPort4.setContainerPort(10443);
    containerPorts.add(containerPort4);

    container.setPorts(containerPorts);

    // **Added lines for volume mounts**
    V1VolumeMount volumeMount = new V1VolumeMount();
    volumeMount.setName("mqtt-storage");
    volumeMount.setMountPath("/MQTTRoute/data");
    volumeMount.setSubPath(deploymentName+"-data");
    container.setVolumeMounts(Collections.singletonList(volumeMount));

    V1PodSpec podSpec = new V1PodSpec();
    podSpec.setContainers(Collections.singletonList(container));

    // **Added lines for volumes**
    V1Volume volume = new V1Volume();
    volume.setName("mqtt-storage");
    V1PersistentVolumeClaimVolumeSource pvcVolumeSource = new V1PersistentVolumeClaimVolumeSource();
    pvcVolumeSource.setClaimName("mqttroute-pvc");
    volume.setPersistentVolumeClaim(pvcVolumeSource);
    podSpec.setVolumes(Collections.singletonList(volume));
    
    //setting node for deployment
    podSpec.setNodeName("kube-bupmz-default-worker-g4hlp-frt4j");


    templateSpec.setSpec(podSpec);

    spec.setTemplate(templateSpec);
    deployment.setSpec(spec);

    // Create deployment
    AppsV1Api appsApi = new AppsV1Api();
    appsApi.createNamespacedDeployment(namespace, deployment, null, null, null, null);

    System.out.println(deploymentName + " success");
}

}
