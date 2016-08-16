package org.wordpress.android.fluxc.annotations.endpoint;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class EndpointTreeGenerator {
    public static EndpointNode process(File file) throws IOException {
        BufferedReader in = new BufferedReader(new FileReader(file));

        EndpointNode endpointTree = new EndpointNode("/");

        String strLine;

        while ((strLine = in.readLine()) != null) {
            if (strLine.length() == 0) {
                continue;
            }
            EndpointNode endpoint = new EndpointNode("");
            boolean firstTime = true;
            for (String str : strLine.replaceAll("^/|/$", "").split("/")) {
                if (firstTime) {
                    endpoint.setLocalEndpoint(str + "/");
                    firstTime = false;
                    continue;
                }
                endpoint.addChild(new EndpointNode(str + "/"));
                endpoint = endpoint.getChildren().get(0);
            }
            insertNodeInNode(endpoint.getRoot(), endpointTree);
        }

        in.close();

        return endpointTree;
    }

    private static void insertNodeInNode(EndpointNode endpointNodeToInsert, EndpointNode endpointTree) {
        if (endpointTree.hasChildren()) {
            for (EndpointNode endpoint : endpointTree.getChildren()) {
                if (endpoint.getLocalEndpoint().equals(endpointNodeToInsert.getLocalEndpoint())) {
                    if (endpointNodeToInsert.hasChildren()) {
                        insertNodeInNode(endpointNodeToInsert.getChildren().get(0), endpoint);
                    }
                    return;
                }
            }
        }
        endpointTree.addChild(endpointNodeToInsert);
    }
}
