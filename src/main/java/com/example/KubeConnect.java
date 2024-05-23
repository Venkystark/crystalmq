package com.example;

import java.io.FileReader;
import java.io.IOException;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.apis.NetworkingV1Api;
import io.kubernetes.client.util.ClientBuilder;
import io.kubernetes.client.util.KubeConfig;

public class KubeConnect {

    static String kubeConfigPath = "server-kubeconfig.yaml";
    

    public static AppsV1Api getAPIInstance() throws IOException, ApiException {
        
        ApiClient client = ClientBuilder.kubeconfig(KubeConfig.loadKubeConfig(new FileReader(kubeConfigPath)))
                .build();

        Configuration.setDefaultApiClient(client);

        AppsV1Api api = new AppsV1Api(client);

        // Check connection for AppsV1Api-DeploymentApi
        try {
            // Send a test request to check if the connection is successful
            api.listDeploymentForAllNamespaces(false, null, null, null, 10, null, null, null, 30, false);
            System.out.println("Connection Success with Deployment Api :)");
        } catch (ApiException e) {
            // Print connection failure details if there's an exception
            System.err.println("AppsV1Api(deployment Api) connection failed! " + e.getResponseBody());
        }

        return api;
    }

    public static CoreV1Api getCoreV1APIInstance() throws IOException, ApiException {

        ApiClient client = ClientBuilder.kubeconfig(KubeConfig.loadKubeConfig(new FileReader(kubeConfigPath)))
                .build();

        Configuration.setDefaultApiClient(client);
        

        CoreV1Api api = new CoreV1Api(client);

        //check connection for coreApi-service api
        try {
            // Send a test request to check if the connection is successful
            api.listServiceForAllNamespaces(null, null, null, null, 10, null, null, null, 30, false);
            System.out.println("Connection Success with Service Api :)");
        } catch (ApiException e) {
            // Print connection failure details if there's an exception
            System.err.println("CoreV1Api(Service Api) Connection faliled! " + e.getResponseBody());
        }
        return api;
    }
    public static NetworkingV1Api getNetworkingV1APIInstance() throws IOException, ApiException {
        ApiClient client = ClientBuilder.kubeconfig(KubeConfig.loadKubeConfig(new FileReader(kubeConfigPath)))
                .build();
        Configuration.setDefaultApiClient(client);
        NetworkingV1Api api = new NetworkingV1Api(client);

        // Check connection for NetworkingV1Api-IngressApi
        try {
            api.listIngressForAllNamespaces(null, null, null, null, 10, null, null, null, 30, false);
            System.out.println("Connection Success with Ingress Api :)");
        } catch (ApiException e) {
            System.err.println("NetworkingV1Api(Ingress Api) Connection faliled! " + e.getResponseBody());
        }

        return api;
    }


}
