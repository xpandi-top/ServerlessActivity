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

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.*;
/*
 * Edit methods in this class for Section II Migration task.
 * Todo list:
 * [ ] write code after line 137 in method of service as the handler for Google CLoud Function.
 * */

public class ImageProcessing implements RequestHandler<Request, HashMap<String, Object>>, HttpFunction {
    private static final Gson gson = new Gson();
    private byte[] processImage(String photoURL) throws IOException {
        /*
         * This method take input of photoURL,
         * fetch the image from the url,
         * return the byte array of resized image
         * */
        URL url = new URL(photoURL);
        BufferedImage originImage = ImageIO.read(url);
        int resizedWidth = originImage.getWidth()/10;
        int resizedHeight = originImage.getHeight()/10;
        Image resizedImage = originImage.getScaledInstance(resizedWidth,resizedHeight, Image.SCALE_DEFAULT);
        BufferedImage resizedBufferedImage = new BufferedImage(resizedWidth,resizedHeight, BufferedImage.TYPE_INT_RGB);
        resizedBufferedImage.getGraphics().drawImage(resizedImage,0,0,null);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(resizedBufferedImage,"jpg",outputStream);
        return outputStream.toByteArray();
    }
    private void processImageProcedure(Request request, Inspector inspector, String credentialFileName, String provider, String csvBucket, String imageBucket) throws Exception {
        /*
         * This method is the procedure to read csv file,
         * get list of photo urls from the csv file,
         * process the images get from the photo urls.
         * put the processed images to S3/Google
         * */
        String name = request.getObjectName();
        if (name==null) throw new Exception("the object Name cannot be null");
        // Contents to upload
        String readContent = null;
        CredentialRetriever credentialRetriever = new CredentialRetriever(credentialFileName,provider);// read credential file
        BlobStoreContext blobContext = ContextBuilder.newBuilder(credentialRetriever.getProvider())
                .credentials(credentialRetriever.getIdentity(),credentialRetriever.getCredential())
                .buildApi(BlobStoreContext.class);
        BlobStore blobStore = blobContext.getBlobStore();

        String UWNetID = request.getStudentID();
        String objectName = request.getObjectName();
        int startIndex = request.getStartIndex();
        // Get csv file from S3
        Blob downloadBlob = blobStore.getBlob(csvBucket, objectName);
        InputStream csvFile;
        try {
            csvFile = downloadBlob.getPayload().openStream();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        // Read the dataset file scanning data line by line and process line by line
        // add photo URLs to a list, add photo name to a list
        Scanner scanner = new Scanner(csvFile);
        List<String> photoURLs = new ArrayList<>();
        List<String> photoNames = new ArrayList<>();
        int currLine = Math.max(startIndex, 0);
        while (scanner.hasNext()){
            String line = scanner.nextLine();
            String[] vals = line.split("\\t");
            if (currLine>startIndex && currLine<=startIndex+3){
                photoURLs.add(vals[2]);
                photoNames.add(vals[0]);
            }else if (currLine>startIndex+3){
                break;
            }
            currLine++;
        }
        scanner.close();
        // Process each photoURL to get IMAGE, resize the image to upload to S3
        boolean[] successes = new boolean[photoURLs.size()];
        for (int i = 0; i< photoURLs.size();i++){
            String photoURL= photoURLs.get(i);
            String photoName = photoNames.get(i);
            try {
                byte[] bytes = processImage(photoURL);
                ByteSource payload = ByteSource.wrap(bytes);
                // Add Blob
                Blob blob = blobStore.blobBuilder(UWNetID+"/"+photoName+".jpg")
                        .payload(payload)
                        .contentLength(payload.size())
                        .contentType("image/jpeg")
                        .build();
                // Save processed Image to S3
                blobStore.putBlob(imageBucket, blob);
                successes[i]=true;
            } catch (IOException e) {
                System.out.println(i+"th photo fails processing with error: "+e.getMessage());
            }

        }
        Response response = new Response();
        response.setValue("This Function is for Processing Image with "+ provider);
        inspector.addAttribute("successList", Arrays.toString(successes));
        inspector.addAttribute("startIndex",startIndex);
        inspector.consumeResponse(response);
    }

    @Override
    public void service(HttpRequest httpRequest, HttpResponse httpResponse) throws Exception {
        Inspector inspector = new Inspector();
        boolean isMac = System.getProperty("os.name").contains("Mac");
        if (!isMac){
            inspector.inspectAll();
        }
        //****************START FUNCTION IMPLEMENTATION*************************
        //Todo:  Migrate your code here





        //****************END FUNCTION IMPLEMENTATION***************************
        if (!isMac){
            inspector.inspectAllDeltas();
        }
        BufferedWriter writer = httpResponse.getWriter();
        writer.write(gson.toJson(inspector.finish()));
    }

    @Override
    public HashMap<String, Object> handleRequest(Request request, Context context) {
        Inspector inspector = new Inspector();
        boolean isMac = System.getProperty("os.name").contains("Mac");
        if (!isMac){
            inspector.inspectAll();
        }
        //****************START FUNCTION IMPLEMENTATION*************************
        String credentialFileName="aws_credential.json";
        String csvBucket = "image.processing.aws.csv"; // this is the container storing the csv file
        String imageBucket = "image.processing.aws.images"; // this is the container storing the processed image
        String provider = "aws-s3";
        try {
            processImageProcedure(request,inspector,credentialFileName,provider,csvBucket,imageBucket);
        } catch (Exception e) {
            System.out.println("There is an error: "+ e.getMessage());
            throw new RuntimeException(e);
        }
        //****************END FUNCTION IMPLEMENTATION***************************
        if (!isMac){
            inspector.inspectAllDeltas();
        }
        return inspector.finish();
    }
    public static void main(String[] args) throws Exception {
        ImageProcessing imageProcessing = new ImageProcessing();
        FaaSMain.output(args,imageProcessing,gson);
    }
}
