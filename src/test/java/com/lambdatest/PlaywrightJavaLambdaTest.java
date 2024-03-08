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

public class PlaywrightJavaLambdaTest {
    public static void main(String[] args) {
        try (Playwright playwright = Playwright.create()) {
            JsonObject capabilities = new JsonObject();
            JsonObject ltOptions = new JsonObject();

            String user = "ritamg";
            String accessKey = "acess_key";

            capabilities.addProperty("browserName", "Chrome");
            capabilities.addProperty("browserVersion", "latest");
            ltOptions.addProperty("platform", "Windows 10");
            ltOptions.addProperty("name", "Playwright Test");
            ltOptions.addProperty("build", "Playwright Upload Files");
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

                page.navigate("https://the-internet.herokuapp.com/upload");

                Thread.sleep(12000);

                Locator locator = page.locator("//input[@id='file-upload']");
                locator.setInputFiles(Paths.get(filePath.toUri()));

                Thread.sleep(12000);

                System.out.println("Done Uploading");


                setTestStatus("passed", "Download succeeded", page);







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
}
