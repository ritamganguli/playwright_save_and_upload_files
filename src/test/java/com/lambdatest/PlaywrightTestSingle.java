package com.lambdatest;

import com.microsoft.playwright.*;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class PlaywrightTestSingle {

    public static void main(String[] args) {
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(false)); // Launch in non-headless mode

            // Configure options to accept downloads
            Browser.NewContextOptions contextOptions = new Browser.NewContextOptions().setAcceptDownloads(true);
            String userHomePath = System.getProperty("user.home");
            Path downloadsPath = Paths.get(userHomePath, "Downloads");

            // Create a new browser context with the options
            BrowserContext context = browser.newContext(contextOptions);
            Page page = context.newPage();
            page.navigate("https://the-internet.herokuapp.com/download");

            // Start waiting for the download event before clicking the download link
            Download download = page.waitForDownload(() -> {
                // Click the download link identified by the XPath. Adjust if necessary.
                page.locator("//*[@id='content']/div/a[6]").click();
            });

            // Determine the full path for the download and save the file
            Path downloadFullPath = downloadsPath.resolve(download.suggestedFilename());
            download.saveAs(downloadFullPath);

            // Print the path to confirm where the file was saved
            System.out.println("File downloaded to: " + downloadFullPath);

            // Upload the file
            sendFile(downloadFullPath);

            // Cleanup
            context.close(); // Close the context
            browser.close(); // Close the browser
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void sendFile(Path filePath) {
        OkHttpClient client = new OkHttpClient().newBuilder().build();
        MediaType mediaType = MediaType.parse("application/octet-stream");
        File file = filePath.toFile();

        RequestBody fileBody = RequestBody.create(mediaType, file);
        RequestBody body = new MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("files", file.getName(), fileBody)
                .build();

        okhttp3.Request request = new okhttp3.Request.Builder()
                .url("https://api.lambdatest.com/automation/api/v1/user-files")
                .method("POST", body)
                .addHeader("accept", "application/json")
                .addHeader("Authorization", "Basic c2h1YmhhbXI6ZGw4WThhczU5aTFZeUdaWlVlTEY4OTdhQ0Z2SURtYUtrVVUxZTZSZ0JtbGdNTElJaGg=") // Replace YOUR_AUTH_TOKEN_HERE with your actual auth token
                .build();

        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : ""; // .string() can be called only once
            System.out.println("Response: " + responseBody);
            if (responseBody.contains("File have been uploaded successfully to our lambda storage")) {
                System.out.println("File uploaded successfully.");
            } else {
                System.out.println("File not uploaded. Check response or error.");
            }
        } catch (IOException e) {
            System.err.println("IOException occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
