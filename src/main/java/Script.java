import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;

public class Script {

    private static String username;
    private static String password;

    public static void main(String[] args) throws IOException, ParseException {

        WebDriver driver;

        JSONParser parser = new JSONParser();
        Object object = parser.parse(new FileReader("src/main/resources/credentials.json"));
        JSONObject jsonObject = (JSONObject) object;
        username = (String) jsonObject.get("username");
        password = (String) jsonObject.get("password");

        ObjectMapper mapper = new ObjectMapper();
        LinkedHashMap map = mapper.readValue(new File("src/main/resources/spaces.json"), LinkedHashMap.class);

        WebDriverManager.chromedriver().setup();

        Set<String> keys = map.keySet();

        for (String key : keys) {
            System.out.println("### Starting process for space: " + key);
            String fileDirectory = System.getProperty("user.dir") + File.separator + "downloadFiles" + File.separator + key;
            String pathToFile = fileDirectory + File.separator + "Confluence-export-space-" + key.toLowerCase() + ".zip";

            File file = new File(pathToFile);
            if (file.exists()) {
                Files.deleteIfExists(Path.of(pathToFile));
            }

            Map<String, Object> prefs = new HashMap<String, Object>();
            prefs.put("download.default_directory", fileDirectory);

            ChromeOptions options = new ChromeOptions();
            options.setExperimentalOption("prefs", prefs);

            driver = new ChromeDriver(options);
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
            WebDriverWait wait60 = new WebDriverWait(driver, Duration.ofSeconds(60));

            ;
            for (int attempt = 1; attempt <= 3; attempt++) {
                try {
                    System.out.println("### Opening page for space " + key + ". Attempt: " + attempt);
                    driver.get((String) map.get(key));
                    wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("username"))).sendKeys(username);
                    wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("login-submit"))).click();
                    wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("password"))).sendKeys(password);
                    wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("login-submit"))).click();
                    wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(".//input[@value='Export']"))).click();
                    wait60.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(".//span[@id='percentComplete' and text()='100']")));
                    wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(".//a[@class='space-export-download-path']"))).click();
                } catch (Exception e) {
                    System.out.println("### Some error occurred, please check the stack trace:");
                    e.printStackTrace();
                    continue;
                }

                int waitInSeconds = 5;
                int waitSteps = 120;
                for (int i = 0; i < waitSteps; i++) {
                    System.out.println("Downloading file for space '" + key + "' Process will be aborted in " + String.valueOf(waitInSeconds * waitSteps - i * waitInSeconds) + " seconds");
                    try {
                        Thread.sleep(waitInSeconds * 1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (file.exists()) {
                        System.out.println("### File for space '" + key + "' downloaded");
                        driver.quit();
                        break;
                    }
                }
                if (!file.exists()) {
                    System.out.println("WARNING! Download time too long. Downloading process for space '" + key + "' aborted.");
                    driver.quit();
                } else {
                    break;
                }
            }
            driver.quit();
        }
    }
}
