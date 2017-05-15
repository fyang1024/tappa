package au.gov.nsw.transport.tappa;

import au.gov.nsw.transport.tappa.model.DependencyManagement;
import com.gargoylesoftware.htmlunit.BrowserVersion;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;

public class SubversionRepositoryCrawler extends RepositoryCrawler {

    SubversionRepositoryCrawler(String url) {
        super(url);
    }

    public void crawl() throws Exception {
        WebDriver driver = new HtmlUnitDriver(BrowserVersion.CHROME);
        doCrawl(driver, url);
        driver.close();
    }

    private void doCrawl(WebDriver driver, String targetUrl) throws ParserConfigurationException, SAXException, IOException {
        if (!targetUrl.contains("/sandbox/") && !targetUrl.contains("/stms_old/")) {
            driver.get(targetUrl);
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new ByteArrayInputStream(driver.getPageSource().getBytes()));
            NodeList files = document.getElementsByTagName("file");
            for (int i = 0; i < files.getLength(); i++) {
                Element file = (Element) files.item(i);
                String fileName = file.getAttribute("name");
//                if(fileName.matches("angular.js") || fileName.matches("angular[\\-|\\\\.]\\d.*.js")
//                        || fileName.matches("kendo.core.js") || fileName.matches("bootstrap.js")) {
//                    System.out.println(targetUrl + file.getAttribute("href"));
//                }
                if (fileName.equals(DependencyManagement.Maven.getConfigurationFile())
                        || fileName.equals(DependencyManagement.Ivy.getConfigurationFile())) {
                    String fileUrl = targetUrl + file.getAttribute("href");
                    analyseProject(driver, fileUrl);
                }
            }
            boolean trunkExists = false;
            NodeList dirs = document.getElementsByTagName("dir");
            for (int i = 0; i < dirs.getLength(); i++) {
                Element dir = (Element) dirs.item(i);
                String dirName = dir.getAttribute("name");
                if (dirName.equals("trunk")) {
                    trunkExists = true;
                    doCrawl(driver, targetUrl + dir.getAttribute("href"));
                    break;
                } else if (!dirName.equals("tags") && !dirName.equals("branches") && !dirName.equals("branch")) {
                    doCrawl(driver, targetUrl + dir.getAttribute("href"));
                }
            }
            boolean tagsExists = false;
            if (!trunkExists && !targetUrl.contains("trunk") && !targetUrl.contains("tags") && !targetUrl.contains("branch")) {
                for (int i = 0; i < dirs.getLength(); i++) {
                    Element dir = (Element) dirs.item(i);
                    String dirName = dir.getAttribute("name");
                    if (dirName.equals("tags")) {
                        driver.get(targetUrl + dir.getAttribute("href"));
                        DocumentBuilderFactory factory2 = DocumentBuilderFactory.newInstance();
                        DocumentBuilder builder2 = factory2.newDocumentBuilder();
                        Document document2 = builder2.parse(new ByteArrayInputStream(driver.getPageSource().getBytes()));
                        NodeList tags = document2.getElementsByTagName("dir");
                        Element latestTag = null;
                        for (int j = 0; j < tags.getLength(); j++) {
                            Element tag = (Element) tags.item(j);
                            if (tag.getAttribute("name").startsWith("REL")) {
                                if (latestTag == null || latestTag.getAttribute("name").compareTo(tag.getAttribute("name")) < 0) {
                                    latestTag = tag;
                                }
                            } else {
                                latestTag = null;
                                break;
                            }
                        }
                        if (latestTag != null) {
                            tagsExists = true;
                            doCrawl(driver, targetUrl + dir.getAttribute("href") + latestTag.getAttribute("href"));
                            break;
                        }
                    }
                }
            }
            if (!trunkExists && !tagsExists && !targetUrl.contains("trunk") && !targetUrl.contains("tags") && !targetUrl.contains("branch")) {
                for (int i = 0; i < dirs.getLength(); i++) {
                    Element dir = (Element) dirs.item(i);
                    String dirName = dir.getAttribute("name");
                    if (dirName.equals("branches") || dirName.equals("branch")) {
                        driver.get(targetUrl + dir.getAttribute("href"));
                        DocumentBuilderFactory factory2 = DocumentBuilderFactory.newInstance();
                        DocumentBuilder builder2 = factory2.newDocumentBuilder();
                        Document document2 = builder2.parse(new ByteArrayInputStream(driver.getPageSource().getBytes()));
                        NodeList branches = document2.getElementsByTagName("dir");
                        Element latestBranch = null;
                        for (int j = 0; j < branches.getLength(); j++) {
                            Element branch = (Element) branches.item(j);
                            if (latestBranch == null || latestBranch.getAttribute("name").compareTo(branch.getAttribute("name")) < 0) {
                                latestBranch = branch;
                            }
                        }
                        if (latestBranch != null) {
                            doCrawl(driver, targetUrl + dir.getAttribute("href") + latestBranch.getAttribute("href"));
                        }
                    }
                }
            }
        }
    }
}
