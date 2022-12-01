package functions;

import basic.CredentialRetriever;
import basic.Request;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.google.cloud.functions.HttpFunction;
import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpResponse;
import com.google.common.io.ByteSource;
import com.google.gson.Gson;
import httpfaas.FaaSMain;
import org.jclouds.ContextBuilder;
import org.jclouds.blobstore.BlobStore;
import org.jclouds.blobstore.BlobStoreContext;
import org.jclouds.blobstore.domain.Blob;
import saaf.Inspector;
import saaf.Response;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;

import static java.nio.charset.StandardCharsets.UTF_8;
/*
 * Edit methods in this class for Section I UploadObject task.
 * Todo list:
 * [ ] modify variable objectName (Line 40) in method of uploadObjectProcedure
 * */

public class UploadObject implements RequestHandler<Request, HashMap<String, Object>>, HttpFunction {
    private static final Gson gson = new Gson();
    private void uploadObjectProcedure(Request request, Inspector inspector, String credentialFileName, String provider, String bucketName) throws Exception {
        String name = request.getObjectName();
        if (name==null) throw new Exception("the object Name cannot be null");
        inspector.addAttribute("message", "Hello " + name
                + "! This is Function is to Upload content to Google CLoud Storage");
        String contents = "Hello " + name + "! This is content from GCP with Jclouds";// Contents to upload
        // Todo: Object Name for the file to upload. Replace `UWNetID` with your UWNetID(This is your uw email without “@uw.edu"). For example, My uw email is "dimo@uw.edu", so my UW NetID is “dimo”.
        String objectName = "UWNetID/"+name + ".txt";
        CredentialRetriever credentialRetriever = new CredentialRetriever(credentialFileName,provider);// read credential file
        BlobStoreContext blobContext = ContextBuilder.newBuilder(credentialRetriever.getProvider())
                .credentials(credentialRetriever.getIdentity(),credentialRetriever.getCredential())
                .buildApi(BlobStoreContext.class);
        BlobStore blobStore = blobContext.getBlobStore();
        ByteSource payload = ByteSource.wrap(contents.getBytes(UTF_8));// Call blobstore api to upload content
        try {
            // Create Object Information
            Blob blob = blobStore.blobBuilder(objectName)
                    .payload(payload)
                    .contentLength(payload.size())
                    .build();
            blobStore.putBlob(bucketName, blob);
        } catch (IOException e) {
            System.out.println("There is an error: "+ e.getMessage());
            throw new RuntimeException(e);
        }
        Response response = new Response();
        response.setValue(objectName+" is created");
        inspector.consumeResponse(response);

    }
    @Override
    public HashMap<String, Object> handleRequest(Request request, Context context) {
        Inspector inspector = new Inspector();
        boolean isMac = System.getProperty("os.name").contains("Mac");
        if (!isMac) {
            inspector.inspectAll();
        }
        //****************START FUNCTION IMPLEMENTATION*************************
        String credentialFileName="aws_credential.json";
        String provider="aws-s3";
        String bucketName = "jclouds.tutorial.562f22"; // The bucket name of Google Cloud Storage Name
        try {
            uploadObjectProcedure(request,inspector,credentialFileName,provider,bucketName);
        } catch (Exception e) {
            System.out.println("There is an error: " + e.getMessage());
            throw new RuntimeException(e);
        }
        //****************END FUNCTION IMPLEMENTATION***************************
        if (!isMac) {
            inspector.inspectAllDeltas();
        }
        return inspector.finish();
    }

    @Override
    public void service(HttpRequest httpRequest, HttpResponse httpResponse) throws Exception {
        Inspector inspector = new Inspector();
        boolean isMac = System.getProperty("os.name").contains("Mac");
        if (!isMac){
            inspector.inspectAll();
        }
        //****************START FUNCTION IMPLEMENTATION*************************
        Request request = gson.fromJson(httpRequest.getReader(), Request.class);
        String credentialFileName="gcp_credential.json";
        String provider="google-cloud-storage";
        String bucketName = "jclouds-tutorial-562f22"; // The bucket name of Google Cloud Storage Name
        try {
            uploadObjectProcedure(request,inspector,credentialFileName,provider,bucketName);
        } catch (Exception e) {
            System.out.println("There is an error: " + e.getMessage());
            throw new RuntimeException(e);
        }
        //****************END FUNCTION IMPLEMENTATION***************************
        if (!isMac){
            inspector.inspectAllDeltas();
        }
        BufferedWriter writer = httpResponse.getWriter();
        writer.write(gson.toJson(inspector.finish()));
    }

    public static void main(String[] args) throws Exception {
        UploadObject uploadObject = new UploadObject();
        FaaSMain.output(args,uploadObject,gson);
    }
}
