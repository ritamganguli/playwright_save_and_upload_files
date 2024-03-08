package com.lambdatest;

import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.MediaType;
import java.io.File;
import java.io.IOException;

public class SingleApi {

    public static void main(String[] args) {
        SingleApi api = new SingleApi();
        api.sendFile();
        System.out.println("Program completed.");
    }

    public void sendFile() {
        OkHttpClient client = new OkHttpClient().newBuilder().build();
        MediaType mediaType = MediaType.parse("application/octet-stream");
        File file = new File("/C:/Users/ritamg/Desktop/New folder (2)/playwright-sample-main/playwright-java/playwright-java/download.jpeg");

        RequestBody fileBody = RequestBody.create(mediaType, file);
        RequestBody body = new MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("files", file.getName(), fileBody)
                .build();

        Request request = new Request.Builder()
                .url("https://api.lambdatest.com/automation/api/v1/user-files")
                .method("POST", body)
                .addHeader("accept", "application/json")
                .addHeader("Authorization", "Basic c2h1YmhhbXI6ZGw4WThhczU5aTFZeUdaWlVlTEY4OTdhQ0Z2SURtYUtrVVUxZTZSZ0JtbGdNTElJaGg=")
                .build();

        try (Response response = client.newCall(request).execute()) {
            String responseBody  = response.body() != null ? response.body().string() : ""; // .string() can be called only once
            System.out.println("Response: " + responseBody);
            if (responseBody.contains("File have been uploaded successfully to our lambda storage")) {
                System.out.println("File uploaded successfully. Returning...");
                // The response body is automatically closed here due to the try-with-resources statement
            } else {
                System.out.println("File not uploaded. Check response or error.");
            }
            System.out.println("Ritam");
        } catch (IOException e) {
            System.err.println("IOException occurred: " + e.getMessage());
            e.printStackTrace();
        }
        System.exit(0);
        // The connection and the response body will be closed here if an exception is thrown
        // or after the if-else block is executed.
    }

}
