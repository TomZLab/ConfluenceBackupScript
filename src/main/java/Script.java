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
import java.time.Duration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class Script {

    private static String username;
    private static String password;

    public static void main(String[] args) throws IOException, ParseException {

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
            String filedirectory = System.getProperty("user.dir") + File.separator + "downloadFiles" + File.separator + key;
            System.out.println(key + " : " + map.get(key));

            Map<String, Object> prefs = new HashMap<String, Object>();
            prefs.put("download.default_directory", filedirectory);

            ChromeOptions options = new ChromeOptions();
            options.setExperimentalOption("prefs", prefs);

            WebDriver driver = new ChromeDriver(options);
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
            driver.manage().window().maximize();

            driver.get((String) map.get(key));
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(".//span[@data-testid='app-navigation-login']//a"))).click();
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("username"))).sendKeys(username);
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("login-submit"))).click();
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("password"))).sendKeys(password);
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("login-submit"))).click();
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(".//span[text()='Space Settings']"))).click();
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(".//span[text()='Export space']"))).click();
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(".//input[@value='export-format-xml']"))).click();
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(".//input[@value='Next >>']"))).click();
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(".//input[@value='Export']"))).click();
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(".//span[@id='percentComplete' and text()='100']")));
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(".//a[@class='space-export-download-path']"))).click();

            File file = new File(filedirectory + File.separator + "Confluence-export-space-sfs.zip");

            for (int i = 0; i < 30; i++) {
                System.out.println("waiting for file " + i);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (file.exists()) {
                    if (driver != null) {
                        driver.quit();
                    }
                    break;
                }
            }
        }
    }
}
