package com.example;

import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Service;
import io.kubernetes.client.openapi.models.V1ServiceList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class KubePortFinder {

    private static final int NODE_PORT_MIN = 30000;
    private static final int NODE_PORT_MAX = 32767;
    private static final int SERVICE_PORT_MIN = 1;
    private static final int SERVICE_PORT_MAX = 65535;

    public int findFreeNodePort(CoreV1Api coreV1Api) throws ApiException {
        Set<Integer> usedPorts = getUsedPorts(coreV1Api);

        for (int port = NODE_PORT_MIN; port <= NODE_PORT_MAX; port++) {
            if (!usedPorts.contains(port)) {
                return port;
            }
        }
        throw new RuntimeException("No free NodePort available in the range " + NODE_PORT_MIN + "-" + NODE_PORT_MAX);
    }

    public int findFreeServicePort(CoreV1Api coreV1Api) throws ApiException {
        Set<Integer> usedPorts = getUsedPorts(coreV1Api);

        for (int port = SERVICE_PORT_MIN; port <= SERVICE_PORT_MAX; port++) {
            if (!usedPorts.contains(port)) {
                return port;
            }
        }
        throw new RuntimeException("No free ServicePort available in the range " + SERVICE_PORT_MIN + "-" + SERVICE_PORT_MAX);
    }

    private Set<Integer> getUsedPorts(CoreV1Api coreV1Api) throws ApiException {
        Set<Integer> usedPorts = new HashSet<>();
        V1ServiceList serviceList = coreV1Api.listServiceForAllNamespaces(null, null, null, null, null, null, null, null, null, null);
        List<V1Service> services = serviceList.getItems();

        for (V1Service service : services) {
            if (service.getSpec() != null && service.getSpec().getPorts() != null) {
                service.getSpec().getPorts().forEach(port -> {
                    if (port.getNodePort() != null) {
                        usedPorts.add(port.getNodePort());
                    }
                    if (port.getPort() != null) {
                        usedPorts.add(port.getPort());
                    }
                });
            }
        }
        return usedPorts;
    }
}
