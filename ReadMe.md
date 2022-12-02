# Serverless Migration Activity
## SECTION I: Serverless Tutorial on Google Cloud Platform
### Objective
Google is an alternative cloud provider to AWS. AWS and Google Cloud both provide multiple cloud storage services and computing services. While AWS provides S3 as object storage, Google Cloud provides Google Cloud Storage as an object storage service.
Google Cloud Functions is an alternative serverless Function-as-a-Service (FaaS) platform to AWS Lambda.
In this tutorial, we introduce the Google Cloud Platform, Google Cloud Functions, and how to use the command line to upload functions to Google Cloud Functions (GCF), and then how to test a Google Cloud Function using the CLI.

Apache Jclouds is an open source multi-cloud toolkit for the Java platform that gives you the freedom to create applications that can be more portable across clouds.

We will practice how to interact with Google Cloud Storage using Google Cloud Functions to read and write a file in Google Cloud Storage with the BlobStore class provided by the Apache Jclouds library.

### Prerequisites
Please make sure you have installed the following tools on your computer.
- [ ] git
- [ ] Java 11
- [ ] maven
- [ ] gcloud
- [ ] AWS CLI Version 1 (optional)

The gcloud command line interface (CLI) is a set of tools for managing Google Cloud resources. We can configure the gcloud CLI on our personal computer to then perform tasks from the terminal or scripts.
Follow this [gcloud install tutorial](https://cloud.google.com/sdk/docs/install) to install gcloud CLI in your personal computer (e.g. desktop or laptop).
If you add the gcloud CLI to your system's `PATH` variable, then you can run gcloud commands directly from any directory. If not, you need to remember where gcloud has been installed, and specify full qualified pathnames to execute gcloud CLI commands. 
After installation, use gcloud to activate the service account provided.

You will receive two JSON files that contains a Google Cloud service account key (gcp_credential.json) and AWS Credential file (aws_credential.json). Please store them safely.
Replace the KEYFILE below with the file path of the provided gcp_credential.json file stored on your computer. Replace `SERVICE-ACCOUNT` with the client_email in the json file. Replace `GPROJECT_ID`with `project_id` in the provided json file.
```bash
gcloud auth activate-service-account SERVICE-ACCOUNT --key-file=KEYFILE  --project=GPROJECT_ID
```
You can use this command to check whether your gcloud configuration is activated.
```bash
gcloud config configurations list
```

if you see the Account is the service account and the project is the project_id in json file. You are ready to go.

### Use git to download the codes
```bash
git clone https://github.com/xpandi-top/ServerlessActivity.git
```
Open the downloaded folder. Put the provided gcp_credential json file in to the `src/main/resources` folder. You may need to create the resources folder under main. Rename this file to `gcp_credential.json`.

Use any IDE you like or Editor to open the project folder (the folder with pom.xml). Now we are going to Write a function to upload a text file to Google cloud storage.

Google Cloud Storage is similar to AWS S3. We can upload, update, read, and delete files in Cloud Storage. The bucket has already been created in advance in the Google cloud project.

We are going to directly interact with the bucket in this project without worrying about how to create a bucket in the cloud.

### Upload a file to cloud storage
We are going to deploy an UploadObject function to upload a file to Google Object Storage. The Java class you will work with is `src/main/java/functions/UploadObject.java`.
The `UploadObject` class implements the `HttpFunction` interface to support deployment to Google Cloud Functions, and `RequestHandler<Request, HashMap<String, Object>>` for supporting deployment to AWS lambda. In this class, the method `handleRequest` is the handler method for AWS Lambda. The method `service` is the handler method for Google Cloud Functions.
The `uploadObjectProcedure` method centralizes common code and logic for the UploadObject serverless function relevant to both clouds.

Modify the variable`objectName`(line 40) in `UploadObject.java`. change `UWNetID` with your UW NetID.
```java
// following code is line 43-46, src/main/java/functions/UploadObjectGCP; Line 38-41, src/main/java/functionsUploadObjectAWS
// PROVIDER is 'aws-s3' for AWS, "google-cloud-storage" for Google CLoud Platform;
// IDENTITY is AWS Access Key for AWS, service account for Google Cloud Platform;
// CREDENTIAL is AWS secrete Access Key for AWS, credential for Google Cloud Platform.
BlobStoreContext blobContext = ContextBuilder.newBuilder(PROVIDER)
        .credentials(IDENTITY,CREDENTIAL)
        .buildApi(BlobStoreContext.class);
BlobStore blobStore = blobContext.getBlobStore();

// upload Object ,
ByteSource payload = ByteSource.wrap(content.getBytes(UTF_8));
Blob blob = blobStore.blobBuilder(objectName)
        .payload(payload)
        .contentLength(payload.size())
        .build();
blobStore.putBlob(containerName, blob);
```
Now you can try to compile and run to test it.
Make sure you are in the project directory.
```bash
mvn clean -f pom.xml
mvn verify -f pom.xml
```
Then execute your function from the command line to test it locally. Change the name `Alice` to some other name.
```bash
java -cp target/ServerlessActivity-1.0-SNAPSHOT-jar-with-dependencies.jar functions.UploadObject GCP "{\"objectName\":\"Alice\"}"
```
The output will appear as below:
```bash
Running function for GCP
cmd-line json input is: {"objectName":"Alice"}
function result:
{"runtime":6362,"startTime":1669770949496,"endTime":1669770955858,"lang":"java","message":"Hello Alice! This is Function is to Upload content to google-cloud-storage","version":0.5,"value":"UWNetID/Alice.txt is created"}
```
### Check the files you uploaded to Google Cloud Storage
Replace `UWNetID` with your UWNetID(This is your uw email without “@uw.edu"). For example, My uw email is "dimo@uw.edu", so my UW NetID is “dimo”.
```bash
gcloud storage ls  gs://jclouds-tutorial-562f22/UWNetID
```
For example, I run the command `gcloud storage ls  gs://gcp-tutorial-562f22/dimo`. This will list the files I created. `test1.txt` and `test.txt` are two files I created.
```bash
gs://jclouds-tutorial-562f22/dimo/test.txt
gs://jclouds-tutorial-562f22/dimo/test1.txt
```

#### Optional: Test function locally for AWS
Open the downloaded folder. Put the provided `aws_credential.json`  file in to the src/main/resources folder.

Now you can try to compile and run to test it.
Make sure you are in the project directory.
```bash
mvn clean -f pom.xml
mvn verify -f pom.xml
```
The UploadObject class can also upload data to the Amazon Simple Storage Service (S3). Now test running the function locally to upload data to Amazon S3:
```bash
# test the function for uploading to AWS Lambda
java -cp target/ServerlessActivity-1.0-SNAPSHOT-jar-with-dependencies.jar functions.UploadObject AWS "{\"objectName\":\"Alice\"}"
```

check the file you uploaded in AWS S3. Replace `UWNetID` with your UW NetID.
```bash
aws s3 ls jclouds.tutorial.562f22/UWNetID/
```

### Deploy functions to create file in Cloud Storage
Go to the project directory where you have cloned the project source code. Deploy the function from the terminal. Replace the `FUNCTIONNAME`with `UploadObjectUWNetID`. Replace `UWNetID` with yourUW NetID(This is your uw email without “@uw.edu"). For example, My uw email is "dimo@uw.edu", so my UW NetID is “dimo” and my function name should be `UploadObjectdimo`.

Run the following command to use gcloud to upload function to Cloud Functions.
```bash
gcloud functions deploy FUNCTIONNAME \
--entry-point functions.UploadObject \
--timeout 300 \
--runtime java11 --trigger-http 
```

It will take several minutes to upload. After you finish, you can try to use the gcloud CLI to call your newly uploaded function.
Replace the `FUNCTIONNAME` with your function name. you can replace the name `test` with some other name value.

```bash
gcloud functions call FUNCTIONNAME --data '{"objectName":"test"}'
```

The result will look similar to that shown below. This means your function is working. Your file is uploaded to the Google Cloud Storage bucket.
```bash
executionId: 8ihy01xau0t6
result: '{"cpuType":"unknown","cpuNiceDelta":0,"vmuptime":1669860310,"cpuModel":"85","linuxVersion":"Linux
  localhost 4.4.0 #1 SMP Sun Jan 10 15:06:54 PST 2016 x86_64 x86_64 x86_64 GNU/Linux","cpuSoftIrqDelta":0,"cpuUsrDelta":0,"uuid":"44b9b660-73ae-495a-9b8b-70c0f44bed8b","platform":"Unknown
  Platform","contextSwitches":0,"cpuKrn":0,"cpuIdleDelta":0,"cpuIowaitDelta":0,"newcontainer":0,"cpuNice":0,"startTime":1669860324003,"lang":"java","cpuUsr":0,"freeMemory":"150760","value":"dimoJ/test.txt
  is created","frameworkRuntime":7,"contextSwitchesDelta":0,"frameworkRuntimeDeltas":0,"vmcpusteal":0,"cpuKrnDelta":0,"cpuIdle":0,"runtime":2780,"message":"Hello
  test! This is Function is to Upload content to Google CLoud Storage","version":0.5,"cpuIrqDelta":0,"cpuIrq":0,"totalMemory":"262144","cpuCores":"0\nv","cpuSoftIrq":0,"cpuIowait":0,"endTime":1669860326783,"vmcpustealDelta":0,"userRuntime":2773}'
```

### Practice: Deploy a ReadObject function
#### Deploy a function to read object from Cloud Storage
Please edit `src/main/java/functions/ReadObject.java` to read a file from Google Cloud Storage. Modify the variables `objectName`(line 39). Write your code after Line 43.

Compile and test it locally before uploading to Google Cloud Functions.

Name your function as `ReadObjectUWNetID` when upload your function to Google Cloud Functions, Replace `UWNetID` with your UW NetID(This is your uw email without “@uw.edu"). For example, My uw email is "dimo@uw.edu", so my UW NetID is “dimo”.
Below is code example of reading contents of an object.
```java
// PROVIDER is 'aws-s3' for AWS, "google-cloud-storage" for Google CLoud Platform;
        BlobStoreContext blobContext = ContextBuilder.newBuilder(provider)
                .credentials(credentialRetriever.getIdentity(),credentialRetriever.getCredential())
                .buildApi(BlobStoreContext.class);
        BlobStore blobStore = blobContext.getBlobStore();

        Blob downloadBlob = blobStore.getBlob(bucketName, objectName);
        InputStream inputStream = downloadBlob.getPayload().openStream();
        readContent = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
```

The input for the function should be
```json
{"objectName": OBJECTNAME}
```

The output fot the function should be
```json
{...,
  "value": "This is content read from ReadObject Function: " + OBJECTCONTENTS,
  "message": "Hello Alice! This is Function is to read content from google-cloud-storage",
  ...
}
```
### Optional 
The ReadObject class can also be used to read content from Amazon S3. You can test the function locally to read data from Amazon S3
```bash
# test the function for uploading to AWS Lambda
java -cp target/ServerlessActivity-1.0-SNAPSHOT-jar-with-dependencies.jar functions.ReadObject AWS "{\"objectName\":\"Alice\"}"
```
#### set your AWS CLI and deploy function to AWS Lambda
##### Back up your own AWS credentials
Deploying function to AWS in this section, you are using provided the aws credential. It is important to back up your own AWS credentials before going to next step.
You can run the following command for back up.
```bash
# backup AWS credentials
cp ~/.aws/credentials ~/.aws/credentials-backup
```
At the conclusion of the tutorial and activity, You can restore credentials with the commands:
```bash
# backup the tutorial 8 credentials by renaming the file
mv ~/.aws/credentials ~/.aws/credentials.tutorial8

# restore the original account credentials
mv ~/.aws/credentials-backup ~/.aws/credentials
```
##### AWS CLI set up
After back up your own aws credentials. Open your terminal, run the following command.
```bash
aws configure
```
There will be prompts to ask you to enter the Access Key ID and Secret Access Key.
Enter the Access Key ID with the `identity` in `aws_credential.json` file.
Enter the Secret Access Key with `credential` in `aws_credential.json` file.

#### Deploy function to AWS Lambda with AWS CLI
Replace the `FUNCTIONNAME`with `UploadObjectUWNetID`. Replace `UWNetID` with your UW NetID(This is your uw email without “@uw.edu"). For example, My uw email is "dimo@uw.edu", so my UW NetID is “dimo”.
Replace the `ROLE_NAME` with `role_name` in provided json file. The role has been created in advance and granted S3 Read and Write permission.
```bash
aws lambda create-function --function-name FUNCTIONNAME \
--zip-file fileb://target/ServerlessActivity-1.0-SNAPSHOT-jar-with-dependencies.jar \
--handler functions.UploadObject::handleRequest --runtime java11 \
--role ROLE_NAME \
--timeout 300 \
--memory 512
```
It will take seconds to minutes to upload the lambda. Once it is finished. You can test it with AWS CLI. You can try different payload.You can check [aws cli invoke reference](https://docs.aws.amazon.com/cli/latest/reference/lambda/invoke.html) for more options.
```bash
aws lambda invoke --function-name FUNCTIONNAME --payload '{"objectName":"test"}' /dev/stdout
```
If you want to update codes to existing lambda. Use the following command to update your lambda. You can check [update function code](https://docs.aws.amazon.com/cli/latest/reference/lambda/update-function-code.html) for more options.
```bash
aws lambda update-function-code --function-name FUNCTIONNAME --zip-file fileb://target/ServerlessActivity-1.0-SNAPSHOT-jar-with-dependencies.jar --handler functions.ReadObject::handleRequest
```
If you made some modification and wanted to update the function, run the following command. You can check [aws cli update function reference](https://docs.aws.amazon.com/cli/latest/reference/lambda/update-function-configuration.html) for more options.
Repalce Modified handler with the handler you want to change. TimeOutNumber with the seconds you want to change.
```bash
aws lambda update-function-configuration --function-name FUNCTIONNAME --handler ModifiedHandler --timeout TimeOutNumber
```
## SECTION II: Migration Activity
### Objective
For the activity, migrate code for an image processing function written for AWS Lambda so it can be uploaded and made operational under Google Cloud Functions.
There are already CSV files uploaded in AWS S3 that contain /(i.e. point to/) the image URLs we want to use.
The size of the original images is large. Our function will get the image from the object store URL, resize the image, and then upload the processed \(shrunk\) image as a new image file in AWS S3.
The image processing function will process 3 images for each call. In Google Cloud Storage, there are already identical CSV files that also point to the image files in Google Cloud Storage.
### Completing code migration
You are going to complete the Google Cloud Function implementation in this file: `src/main/java/functions/ImageProcessing.java`. This `ImageProcessing` class is already working for AWS Lambda. Your task is to make the same code work in Google Cloud functions.
Only changes to the ImageProcessing class are required. Write your code in the `service` method, after line 137.
Replace `UWNetID` with your UWNetID
#### Input for the function
`startIndex` the start line of the csv to process.
```json
{
  "objectName": "CSVFILENAME",
  "studentID": "UWNetID",
  "startIndex": 0
}
```
#### Results format.
```json
{successList=[true, true, true], startIndex=0, runtime=8130, startTime=1668555310558, endTime=1668555318688, lang=java, version=0.5, value=This Function is for Processing Image,....}
```

#### Buckets and filenames
Please set the provider,credentialFileName, csvBucket and imageBucket as below.
```java
        String credentialFileName="gcp_credential.json";
        String provider="google-cloud-storage";
        String csvBucket = "image-gcp-processing-csv"; // this is the bucket storing the csv file in Google Cloud Storage
        String imageBucket ="image-gcp-processing-images"; // this is the bucket storing the processed image in Google Cloud Storage
```
After completing your code, you can build and test your code locally. If it works, you can deploy your function to the cloud and test it with gcloud.
### Local test
```bash
java -cp target/ServerlessActivity-1.0-SNAPSHOT-jar-with-dependencies.jar functions.ImageProcessing GCP "{\"objectName\":\"photo.csv\",\"studentID\":\"UWNETID\",\"startIndex\":0}" 
```
### Deploy function to Cloud Storage
We are going to deploy a function to upload image
Go to project directory. Deploy function from terminal. Replace the `FUNCTIONNAME`with `ImageProcessingUWNetID`. Replace `UWNetID` with your UW NetID(This is your uw email without “@uw.edu"). For example, My uw email is "dimo@uw.edu", so my UW NetID is “dimo”. Then the function name should be `ImageProcessingdimo`
Run the following command to use gcloud to upload function to Cloud Functions
```bash
gcloud functions deploy FUNCTIONNAME \
--entry-point functions.ImageProcessing \
--timeout 300 \
--runtime java11 --trigger-http 
```
It will take several minutes to upload. After you finish. you can try to use gcloud to call the uploaded function.
Replace the `FUNCTIONNAME` with your function name. you can change the `test` with some other name value.
Replace the `UWNetID` with your UWNetID.
```bash
gcloud functions call FUNCTIONNAME --data '{"objectName": "CSVFILENAME", "studentID": "UWNetID","startIndex": 0}'
```
### Check the resized image
Replace `UWNetID` with your UW NetID(This is your uw email without “@uw.edu"). For example, My uw email is "dimo@uw.edu", so my UW NetID is “dimo”.
```bash
gcloud storage ls  gs://image-gcp-processing-images/UWNetID/
```
### Check the csv files
Replace `UWNetID` with your UW NetID(This is your uw email without “@uw.edu"). For example, My uw email is "dimo@uw.edu", so my UW NetID is “dimo”.
```bash
gcloud storage ls  gs://image-gcp-processing-csv/
```
If you call the function successfully and see the files created in your folder, Congratulations you have successfully completed the migration task !!
