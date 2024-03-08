package com.lambdatest;

import com.google.gson.JsonObject;
import com.microsoft.playwright.*;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlaywrightJavaUploadDownLoadFile {
    public static void main(String[] args) {
        try (Playwright playwright = Playwright.create()) {
            JsonObject capabilities = new JsonObject();
            JsonObject ltOptions = new JsonObject();

            String user = "shubhamr";
            String accessKey = "dl8Y8as59i1YyGZZUeLF897aCFvIDmaKkUU1e6RgBmlgMLIIhh";

            capabilities.addProperty("browserName", "Chrome");
            capabilities.addProperty("browserVersion", "latest");
            ltOptions.addProperty("platform", "Windows 10");
            ltOptions.addProperty("name", "Playwright Test");
            ltOptions.addProperty("build", "Playwright Java Build");
            ltOptions.addProperty("user", user);
            ltOptions.addProperty("accessKey", accessKey);
            capabilities.add("LT:Options", ltOptions);

            BrowserType chromium = playwright.chromium();
            String caps = URLEncoder.encode(capabilities.toString(), "UTF-8");
            String cdpUrl = "wss://cdp.lambdatest.com/playwright?capabilities=" + caps;
            Browser browser = chromium.connect(cdpUrl);

            // Set to automatically accept downloads
            Browser.NewContextOptions contextOptions = new Browser.NewContextOptions().setAcceptDownloads(true);
            BrowserContext context = browser.newContext(contextOptions);

            Page page = context.newPage();
            try {
                page.navigate("https://the-internet.herokuapp.com/download");

                // Trigger the download
                Download download = page.waitForDownload(() -> {
                    page.locator("//*[@id='content']/div/a[6]").click(); // Adjust the locator as necessary
                });


                Thread.sleep(12000);

                // Construct the path dynamically with original file name
                Path downloadDirectory = Paths.get("playwright-java");
                String originalFileName = download.suggestedFilename();
                Path filePath = downloadDirectory.resolve(originalFileName);

                // Save the file to the specified path
                download.saveAs(filePath);


                System.out.println("Downloaded file saved to: " +filePath);

//                setTestStatus("passed", "Download succeeded", page);

                URL url = new URL("https://api.lambdatest.com/automation/api/v1/user-files");

                // Open a connection to the URL
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                // Set the request method to GET
                connection.setRequestMethod("GET");

                // Set the request headers for accept type and authorization
                // Replace "YourUsername:YourAccessKey" with your actual LambdaTest credentials
                String encodedCredentials = Base64.getEncoder().encodeToString("shubhamr:dl8Y8as59i1YyGZZUeLF897aCFvIDmaKkUU1e6RgBmlgMLIIhh".getBytes());
                connection.setRequestProperty("accept", "application/json");
                connection.setRequestProperty("Authorization", "Basic " + encodedCredentials);

                // Get the response code
                int responseCode = connection.getResponseCode();
                System.out.println("Response Code: " + responseCode);

                // Read the response
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String inputLine;
                StringBuffer response = new StringBuffer();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                System.out.println(response);

                Pattern pattern = Pattern.compile("\"key\":\"([^\"]+)\"");
                Matcher matcher = pattern.matcher(response);

                while (matcher.find()) {
                    System.out.println(matcher.group(1));
                }

                sendFile(filePath);

                System.out.println("File Upload checked");

                page.navigate("https://the-internet.herokuapp.com/upload");

                Thread.sleep(12000);

                Locator locator = page.locator("//input[@id='file-upload']");
                locator.setInputFiles(Paths.get(filePath.toUri()));

                Thread.sleep(12000);

                System.out.println("Done Uploading");







            } catch (Exception err) {
                setTestStatus("failed", err.getMessage(), page);
                err.printStackTrace();
            } finally {
                System.out.println("Closing the browser...");
                browser.close();
                System.exit(0);
            }
        } catch (Exception err) {
            err.printStackTrace();
        }
    }

    public static void setTestStatus(String status, String remark, Page page) {
        page.evaluate("lambdatest_action => {}", String.format("{\"action\": \"setTestStatus\", \"arguments\": { \"status\": \"%s\", \"remark\": \"%s\"}}", status, remark));
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
                System.out.println("File already uploaded");
            } else {
                System.out.println("File not uploded uploading.....");
            }
        } catch (IOException e) {
            System.err.println("IOException occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
