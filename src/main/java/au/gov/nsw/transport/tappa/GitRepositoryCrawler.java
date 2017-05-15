package au.gov.nsw.transport.tappa;


import au.gov.nsw.transport.tappa.model.DependencyManagement;
import com.gargoylesoftware.htmlunit.BrowserVersion;
import org.json.JSONArray;
import org.json.JSONObject;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.logging.Level;

class GitRepositoryCrawler extends RepositoryCrawler {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    GitRepositoryCrawler(String url) {
        super(url);
    }

    void crawl() {
        java.util.logging.Logger.getLogger("com.gargoylesoftware.htmlunit").setLevel(Level.OFF);
        WebDriver driver = new HtmlUnitDriver(BrowserVersion.CHROME);
        login(driver);
        int projectsStart = 0;
        boolean isLastProjectsPage;
        do {
            String actualProjectsUrl = url + "?start=" + projectsStart;
            driver.get(actualProjectsUrl);
            JSONObject projectsJSON = new JSONObject(driver.getPageSource());
            JSONArray projectsJSONArray = projectsJSON.getJSONArray("values");
            for (int i = 0; i < projectsJSONArray.length(); i++) {
                JSONObject projectJSON = projectsJSONArray.getJSONObject(i);
                logger.info("analysing project: " + projectJSON.getString("name"));
                String reposUrl = url + "/" + projectJSON.getString("key") + "/repos";
                int reposStart = 0;
                boolean isLastReposPage;
                do {
                    String actualReposUrl = reposUrl + "?start=" + reposStart;
                    driver.get(actualReposUrl);
                    JSONObject reposJSON = new JSONObject(driver.getPageSource());
                    JSONArray reposJSONArray = reposJSON.getJSONArray("values");
                    for (int j = 0; j < reposJSONArray.length(); j++) {
                        JSONObject repoJSON = reposJSONArray.getJSONObject(j);
                        String repoFilesUrl = reposUrl + "/" + repoJSON.getString("slug") + "/files";
                        int filesStart = 0;
                        boolean isLastFilesPage;
                        do {
                            String pagedRepoFilesUrl = repoFilesUrl + "?limit=1000&start=" + filesStart;
                            driver.get(pagedRepoFilesUrl);
                            JSONObject filesJSON = new JSONObject(driver.getPageSource());
                            if (!filesJSON.isNull("values")) {
                                JSONArray filesJSONArray = filesJSON.getJSONArray("values");
                                for (int k = 0; k < filesJSONArray.length(); k++) {
                                    String filePath = filesJSONArray.getString(k);
//                                    int lastSlash = filePath.lastIndexOf('/');
//                                    String fileName = lastSlash == -1 ? filePath : filePath.substring(lastSlash + 1);
//                                    if(fileName.endsWith("angular.js") || fileName.matches("angular[\\-|\\\\.]\\d.*.js")
//                                            || fileName.matches("kendo.core.js") || fileName.matches("bootstrap.js")) {
//                                        System.out.println(repoFilesUrl.replace("/rest/api/1.0", "").replace("files", "browse") + "/" + filePath + "?raw");
//                                    }
                                    if (filePath.endsWith(DependencyManagement.Ivy.getConfigurationFile()) ||
                                            filePath.endsWith(DependencyManagement.Maven.getConfigurationFile())) {
                                        String fileUrl = repoFilesUrl.replace("/rest/api/1.0", "").replace("files", "browse") + "/" + filePath + "?raw";
                                        analyseProject(driver, fileUrl);
                                    }
                                }
                                isLastFilesPage = filesJSON.getBoolean("isLastPage");
                                if (!isLastFilesPage) {
                                    filesStart = filesJSON.getInt("nextPageStart");
                                }
                            } else {
                                isLastFilesPage = true;
                            }
                        } while (!isLastFilesPage);
                    }
                    isLastReposPage = reposJSON.getBoolean("isLastPage");
                    if (!isLastReposPage) {
                        reposStart = reposJSON.getInt("nextPageStart");
                    }
                } while (!isLastReposPage);
            }
            isLastProjectsPage = projectsJSON.getBoolean("isLastPage");
            if (!isLastProjectsPage) {
                projectsStart = projectsJSON.getInt("nextPageStart");
            }
        } while (!isLastProjectsPage);
        driver.close();
    }

    private void login(WebDriver driver) {
        driver.get(getLoginUrl());
        driver.findElement(By.id("j_username")).sendKeys("yangf");
        driver.findElement(By.id("j_password")).sendKeys("password6^");
        driver.findElement(By.id("submit")).click();
    }

    private String getLoginUrl() {
        return url.substring(0, url.indexOf('/', url.indexOf("//") + "//".length())) + "/login";
    }
}
