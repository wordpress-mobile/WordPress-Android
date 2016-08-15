package org.wordpress.android.fluxc.processor;

import com.google.auto.service.AutoService;

import org.wordpress.android.fluxc.annotations.endpoint.EndpointNode;
import org.wordpress.android.fluxc.annotations.endpoint.WPComEndpoint;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.TypeElement;

@AutoService(Processor.class)
public class EndpointProcessor extends AbstractProcessor {
    @Override
    public void init(ProcessingEnvironment processingEnv) {
        EndpointNode rootNode = new EndpointNode("/");

        EndpointNode sitesNode = new EndpointNode("sites/");
        EndpointNode usersNode = new EndpointNode("users/");

        EndpointNode newSiteNode = new EndpointNode("new/");

        EndpointNode siteNode = new EndpointNode("$site/");
        EndpointNode postsNode = new EndpointNode("posts/");
        EndpointNode newPostNode = new EndpointNode("new/");
        EndpointNode postNode = new EndpointNode("$post_ID/");
        EndpointNode deletePostNode = new EndpointNode("delete/");

        EndpointNode sitePostFormatsNode = new EndpointNode("post-formats/");

        EndpointNode newUserNode = new EndpointNode("new/");

        postNode.addChild(deletePostNode);
        postsNode.addChild(newPostNode);
        postsNode.addChild(postNode);
        siteNode.addChild(postsNode);
        siteNode.addChild(sitePostFormatsNode);
        sitesNode.addChild(siteNode);
        sitesNode.addChild(newSiteNode);
        usersNode.addChild(newUserNode);

        rootNode.addChild(sitesNode);
        rootNode.addChild(usersNode);

        EndpointNode userNode = new EndpointNode("$user/");
        EndpointNode testNode = new EndpointNode("$test/");

        EndpointNode testSubNode = new EndpointNode("sub/");
        testNode.addChild(testSubNode);

        usersNode.addChild(userNode);
        rootNode.addChild(testNode);

        RESTPoet.generate(rootNode, "WPCOMRESTGen", "org.wordpress.android.fluxc.network.rest.wpcom", WPComEndpoint.class);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        return true;
    }
}
