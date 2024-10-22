package org.wordpress.android.fluxc.processor;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;

import org.wordpress.android.fluxc.annotations.AnnotationConfig;
import org.wordpress.android.fluxc.annotations.endpoint.EndpointNode;
import org.wordpress.android.fluxc.annotations.endpoint.EndpointTreeGenerator;
import org.wordpress.android.fluxc.annotations.endpoint.JPAPIEndpoint;
import org.wordpress.android.fluxc.annotations.endpoint.WCWPAPIEndpoint;
import org.wordpress.android.fluxc.annotations.endpoint.WPAPIEndpoint;
import org.wordpress.android.fluxc.annotations.endpoint.WPComEndpoint;
import org.wordpress.android.fluxc.annotations.endpoint.WPComV2Endpoint;
import org.wordpress.android.fluxc.annotations.endpoint.WPComV3Endpoint;
import org.wordpress.android.fluxc.annotations.endpoint.WPOrgAPIEndpoint;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.tools.StandardLocation;

import static javax.lang.model.SourceVersion.latestSupported;

@SuppressWarnings("unused")
@AutoService(Processor.class)
public class EndpointProcessor extends AbstractProcessor {
    private static final String WPCOMREST_ENDPOINT_FILE = "wp-com-endpoints.txt";
    private static final String WPCOMV2_ENDPOINT_FILE = "wp-com-v2-endpoints.txt";
    private static final String WPCOMV3_ENDPOINT_FILE = "wp-com-v3-endpoints.txt";
    private static final String XMLRPC_ENDPOINT_FILE = "xmlrpc-endpoints.txt";
    private static final String WPAPI_ENDPOINT_FILE = "wp-api-endpoints.txt";
    private static final String WPORG_API_ENDPOINT_FILE = "wporg-api-endpoints.txt";
    private static final String JPAPI_ENDPOINT_FILE = "jp-api-endpoints.txt";

    // Plugin endpoints
    private static final String WPORG_API_WC_ENDPOINT_FILE = "wc-wp-api-endpoints.txt";

    private static final Pattern WPCOMREST_VARIABLE_ENDPOINT_PATTERN = Pattern.compile("\\$");
    private static final Pattern WPAPI_VARIABLE_ENDPOINT_PATTERN = Pattern.compile("^<.*>");
    private static final Pattern WPORG_API_VARIABLE_ENDPOINT_PATTERN = Pattern.compile("^\\{.*\\}");
    private static final Pattern WCAPI_VARIABLE_ENDPOINT_PATTERN = WPAPI_VARIABLE_ENDPOINT_PATTERN;
    private static final Pattern JPAPI_VARIABLE_ENDPOINT_PATTERN = WPAPI_VARIABLE_ENDPOINT_PATTERN;

    private static final Map<String, List<String>> XML_RPC_ALIASES;

    static {
        XML_RPC_ALIASES = new HashMap<>();
        List<String> editPostAliases = new ArrayList<>();
        editPostAliases.add("EDIT_MEDIA");
        XML_RPC_ALIASES.put("wp.editPost", editPostAliases);

        List<String> deletePostAliases = new ArrayList<>();
        deletePostAliases.add("DELETE_MEDIA");
        XML_RPC_ALIASES.put("wp.deletePost", deletePostAliases);

        List<String> getUsersBlogsAliases = new ArrayList<>();
        getUsersBlogsAliases.add("GET_USERS_SITES");
        XML_RPC_ALIASES.put("wp.getUsersBlogs", getUsersBlogsAliases);
    }

    private Filer mFiler;

    @Override
    public void init(ProcessingEnvironment processingEnv) {
        mFiler = processingEnv.getFiler();

        try {
            String outputPath = mFiler.getResource(StandardLocation.CLASS_OUTPUT, "", "tmp").getName();
            String fs = File.separator;
            if (outputPath.contains(fs + "fluxc" + fs + "build")) {
                generateWPCOMRESTEndpointFile();
                generateWPCOMV2EndpointFile();
                generateWPCOMV3EndpointFile();
                generateXMLRPCEndpointFile();
                generateWPAPIEndpointFile();
                generateWPORGAPIEndpointFile();
                generateJPAPIEndpointFile();
            } else if (outputPath.contains(fs + "plugins" + fs + "woocommerce" + fs + "build" + fs)) {
                generateWCWPAPIPluginEndpointFile();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        return true;
    }

    private void generateWPCOMRESTEndpointFile() throws IOException {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(WPCOMREST_ENDPOINT_FILE);
        EndpointNode rootNode = EndpointTreeGenerator.process(inputStream);

        TypeSpec endpointClass = RESTPoet.generate(rootNode, "WPCOMREST", WPComEndpoint.class,
                WPCOMREST_VARIABLE_ENDPOINT_PATTERN);
        writeEndpointClassToFile(endpointClass);
    }

    private void generateWPCOMV2EndpointFile() throws IOException {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(WPCOMV2_ENDPOINT_FILE);
        EndpointNode rootNode = EndpointTreeGenerator.process(inputStream);

        TypeSpec endpointClass = RESTPoet.generate(rootNode, "WPCOMV2", WPComV2Endpoint.class,
                WPCOMREST_VARIABLE_ENDPOINT_PATTERN);
        writeEndpointClassToFile(endpointClass);
    }

    private void generateWPCOMV3EndpointFile() throws IOException {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(WPCOMV3_ENDPOINT_FILE);
        EndpointNode rootNode = EndpointTreeGenerator.process(inputStream);

        TypeSpec endpointClass = RESTPoet.generate(rootNode, "WPCOMV3", WPComV3Endpoint.class,
                WPCOMREST_VARIABLE_ENDPOINT_PATTERN);
        writeEndpointClassToFile(endpointClass);
    }

    private void generateXMLRPCEndpointFile() throws IOException {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(XMLRPC_ENDPOINT_FILE);
        // read inputStream into byte array since we will have to use it twice
        byte[] fileContent = new byte[inputStream.available()];
        inputStream.read(fileContent);

        EndpointNode rootNode = EndpointTreeGenerator.process(new ByteArrayInputStream(fileContent));
        TypeSpec endpointClass = XMLRPCPoet.generate(new ByteArrayInputStream(fileContent), "XMLRPC", XML_RPC_ALIASES);
        writeEndpointClassToFile(endpointClass);
    }

    private void generateWPAPIEndpointFile() throws IOException {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(WPAPI_ENDPOINT_FILE);
        EndpointNode rootNode = EndpointTreeGenerator.process(inputStream);

        TypeSpec endpointClass = RESTPoet.generate(rootNode, "WPAPI", WPAPIEndpoint.class,
                WPAPI_VARIABLE_ENDPOINT_PATTERN);
        writeEndpointClassToFile(endpointClass);
    }

    private void generateWCWPAPIPluginEndpointFile() throws IOException {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(WPORG_API_WC_ENDPOINT_FILE);
        EndpointNode rootNode = EndpointTreeGenerator.process(inputStream);

        TypeSpec endpointClass = RESTPoet.generate(rootNode, "WOOCOMMERCE", WCWPAPIEndpoint.class,
                WCAPI_VARIABLE_ENDPOINT_PATTERN);
        writeEndpointClassToFile(endpointClass);
    }

    private void generateWPORGAPIEndpointFile() throws IOException {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(WPORG_API_ENDPOINT_FILE);
        EndpointNode rootNode = EndpointTreeGenerator.process(inputStream);

        TypeSpec endpointClass = RESTPoet.generate(rootNode, "WPORGAPI", WPOrgAPIEndpoint.class,
                WPORG_API_VARIABLE_ENDPOINT_PATTERN);
        writeEndpointClassToFile(endpointClass);
    }

    private void generateJPAPIEndpointFile() throws IOException {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(JPAPI_ENDPOINT_FILE);
        EndpointNode rootNode = EndpointTreeGenerator.process(inputStream);

        TypeSpec endpointClass = RESTPoet.generate(rootNode, "JPAPI", JPAPIEndpoint.class,
                JPAPI_VARIABLE_ENDPOINT_PATTERN);
        writeEndpointClassToFile(endpointClass);
    }

    private void writeEndpointClassToFile(TypeSpec endpointClass) throws IOException {
        JavaFile javaFile = JavaFile.builder(AnnotationConfig.PACKAGE_ENDPOINTS, endpointClass)
                                    .indent("    ")
                                    .build();

        javaFile.writeTo(mFiler);
    }
}
