package au.gov.nsw.transport.tappa;

import au.gov.nsw.transport.tappa.model.Project;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

abstract class RepositoryCrawler {

    private Logger logger = LoggerFactory.getLogger(this.getClass());
    private Pattern turboProjectPattern = Pattern.compile(".*turbo\\-(?!mail\\-adaptor).*", Pattern.MULTILINE);

    String url;

    RepositoryCrawler(String url) {
        this.url = url;
    }

    abstract void crawl() throws Exception;

    void analyseProject(WebDriver driver, String fileUrl) {
        logger.info("analysing " + fileUrl);
        driver.get(fileUrl);
        String pageSource = driver.getPageSource();
        Matcher matcher = turboProjectPattern.matcher(pageSource);
        if (matcher.find()) {
            logger.info("Turbo project found");
            Project project = new ProjectAnalyser().analyse(fileUrl, pageSource);
            if (Application.get(project.getKey()) == null) {
                Application.put(project);
            }
        }
    }
}
