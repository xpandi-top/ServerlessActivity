package functions;

import basic.CredentialRetriever;
import basic.Request;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.google.cloud.functions.HttpFunction;
import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpResponse;
import com.google.gson.Gson;
import httpfaas.FaaSMain;
import org.jclouds.ContextBuilder;
import org.jclouds.blobstore.BlobStore;
import org.jclouds.blobstore.BlobStoreContext;
import org.jclouds.blobstore.domain.Blob;
import saaf.Inspector;
import saaf.Response;

import java.io.BufferedWriter;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

/*
 * Edit methods in this class for Section I ReadObject task.
 * Todo list:
 * [ ] modify variable objectName (Line 39) in method of readObjectProcedure
 * [ ] write code to read file from Google Cloud Storage after line 43 in method of readObjectProcedure
 * */

public class ReadObject implements RequestHandler<Request, HashMap<String, Object>>, HttpFunction {
    private static final Gson gson = new Gson();
    private void readObjectProcedure(Request request, Inspector inspector, String credentialFileName, String provider, String bucketName) throws Exception {
        String name = request.getObjectName();
        if (name==null) throw new Exception("the object Name cannot be null");
        inspector.addAttribute("message", "Hello " + name
                + "! This is Function is to Upload content to Google CLoud Storage");
        // Todo: Object Name for the file to upload. Replace `UWNetID` with your UW NetID(This is your uw email without “@uw.edu"). For example, My uw email is "dimo@uw.edu", so my UW NetID is “dimo”.// Todo: Object Name for the file to upload. Replace `UWNetID` with your UW NetID(This is your uw email without “@uw.edu"). For example, My uw email is "dimo@uw.edu", so my UW NetID is “dimo”.
        String objectName = "UWNetID/"+name + ".txt";
        String readContent = null;
        CredentialRetriever credentialRetriever = new CredentialRetriever(credentialFileName,provider);// read credential file
        // ************************* ADD CODE HERE - START ******************************
        //Todo: write code to read object from Google Cloud storage




        // ************************* ADD CODE HERE - END ******************************
        Response response = new Response();
        response.setValue("This is content read from ReadObject Function: "+ readContent);
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
            readObjectProcedure(request,inspector,credentialFileName,provider,bucketName);
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
            readObjectProcedure(request,inspector,credentialFileName,provider,bucketName);
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
        ReadObject readObject = new ReadObject();
        FaaSMain.output(args,readObject,gson);
    }
}
