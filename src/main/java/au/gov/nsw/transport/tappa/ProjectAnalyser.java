package au.gov.nsw.transport.tappa;

import au.gov.nsw.transport.tappa.model.Dependency;
import au.gov.nsw.transport.tappa.model.DependencyManagement;
import au.gov.nsw.transport.tappa.model.Packaging;
import au.gov.nsw.transport.tappa.model.Project;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class ProjectAnalyser {

    Project analyse(String dependencyDescriptionUrl, String content) {
        DependencyManagement dependencyManagement = getDependencyManagement(dependencyDescriptionUrl);
        if (dependencyManagement == null) {
            throw new RuntimeException("No dependency management found for " + dependencyDescriptionUrl);
        }
        switch (dependencyManagement) {
            case Ivy:
                try {
                    return analyseIvy(dependencyDescriptionUrl, content);
                } catch (Exception e) {
                    throw new RuntimeException("Unable to analyse " + dependencyDescriptionUrl, e);
                }
            case Maven:
                try {
                    return analyseMaven(dependencyDescriptionUrl, content);
                } catch (Exception e) {
                    throw new RuntimeException("Unable to analyse " + dependencyDescriptionUrl, e);
                }
            default:
                throw new RuntimeException("Unable to analyse " + dependencyDescriptionUrl);
        }
    }

    private Project analyseMaven(String dependencyDescriptionUrl, String content) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(new ByteArrayInputStream(content.getBytes("UTF-8")));
        String projectName = null;
        NodeList artifactIdNodeList = document.getElementsByTagName("artifactId");
        for(int i = 0; i < artifactIdNodeList.getLength(); i++) {
            Node node = artifactIdNodeList.item(i);
            if(node.getParentNode().getNodeName().equals("project")) {
                projectName = node.getTextContent().trim();
            }
        }
        String projectGroup = null;
        NodeList groupIdNodeList = document.getElementsByTagName("groupId");
        for(int i=0; i < groupIdNodeList.getLength(); i++) {
            Node node = groupIdNodeList.item(i);
            if(node.getParentNode().getNodeName().equals("project")) {
                projectGroup = node.getTextContent().trim();
            }
        }
        Packaging packaging = Packaging.jar;
        NodeList packagingNodes = document.getElementsByTagName("packaging");
        if(packagingNodes != null && packagingNodes.getLength() > 0) {
            packaging = Packaging.valueOf(packagingNodes.item(0).getTextContent().trim());
        }
        Project project = new Project(projectName, projectGroup, packaging, dependencyDescriptionUrl, DependencyManagement.Maven);
        NodeList parentProjectNodes = document.getElementsByTagName("parent");
        if(parentProjectNodes != null && parentProjectNodes.getLength() > 0) {
            Element parentProjectNode = (Element) parentProjectNodes.item(0);
            String parentProjectName = parentProjectNode.getElementsByTagName("artifactId").item(0).getTextContent().trim();
            String parentProjectGroup = parentProjectNode.getElementsByTagName("groupId").item(0).getTextContent().trim();
            Project parentProject = Application.get(parentProjectGroup + ":" + parentProjectName);
            project.setParent(parentProject);
        }
        Map<String, String> properties = new HashMap<String, String>();
        NodeList propertiesNodes = document.getElementsByTagName("properties");
        for (int i = 0; i < propertiesNodes.getLength(); i++) {
            NodeList propertyNodes = propertiesNodes.item(i).getChildNodes();
            for(int j = 0; j < propertyNodes.getLength(); j++) {
                Node propertyNode = propertyNodes.item(j);
                if (propertyNode.getNodeType() == Node.ELEMENT_NODE) {
                    properties.put("${" + propertyNode.getNodeName() + "}", propertyNode.getTextContent().trim());
                }
            }
        }
        Node dependenciesNode = null;
        NodeList dependenciesNodeList = document.getElementsByTagName("dependencies");
        for (int i = 0; i < dependenciesNodeList.getLength(); i++) {
            Node node = dependenciesNodeList.item(i);
            if (node.getParentNode().getNodeName().equals("project") || node.getParentNode().getNodeName().equals("dependencyManagement")) {
                dependenciesNode = node;
            }
        }
        List<Dependency> dependencies = new ArrayList<Dependency>();
        if (dependenciesNode != null) {
            NodeList dependencyNodes = ((Element) dependenciesNode).getElementsByTagName("dependency");
            for (int i = 0; i < dependencyNodes.getLength(); i++) {
                Element dependencyElement = (Element) dependencyNodes.item(i);
                Node artifactIdNode = dependencyElement.getElementsByTagName("artifactId").item(0);
                String name = artifactIdNode.getTextContent().trim();
                Node groupIdNode = dependencyElement.getElementsByTagName("groupId").item(0);
                String group = groupIdNode.getTextContent().trim();
                String version = null;
                NodeList versionNodes = dependencyElement.getElementsByTagName("version");
                if(versionNodes != null && versionNodes.getLength() > 0) {
                    Node versionNode = dependencyElement.getElementsByTagName("version").item(0);
                    version = versionNode.getTextContent().trim();
                    if(version.startsWith("$")) {
                        version = properties.get(version);
                        if(version == null) version = "latest";
                    }
                } else if(project.getParent() != null) {
                    version = project.getParent().getDependencyVersion(name, group);
                }
                Dependency dependency = new Dependency(name, group, version);
                dependencies.add(dependency);
            }
        }
        project.setDependencies(dependencies);
        return project;
    }

    private Project analyseIvy(String dependencyDescriptionUrl, String content) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(new ByteArrayInputStream(content.getBytes("UTF-8")));
        Node info = document.getElementsByTagName("info").item(0);
        String projectName = info.getAttributes().getNamedItem("module").getNodeValue();
        String projectGroup = info.getAttributes().getNamedItem("organisation").getNodeValue();
        Packaging packaging = null;
        NodeList artifactList = document.getElementsByTagName("artifact");
        for (int i = 0; i < artifactList.getLength(); i++) {
            Element artifact = (Element) artifactList.item(i);
            if (artifact.getAttribute("conf") != null && artifact.getAttribute("conf").equals("runtime")) {
                String ext = artifact.getAttribute("ext");
                if(ext != null && ext.trim().length() > 0) {
                    packaging = Packaging.valueOf(ext);
                }
                break;
            }
        }
        Project project = new Project(projectName, projectGroup, packaging, dependencyDescriptionUrl, DependencyManagement.Ivy);
        Node dependenciesNode = document.getElementsByTagName("dependencies").item(0);
        NodeList dependencyNodes = ((Element) dependenciesNode).getElementsByTagName("dependency");
        List<Dependency> dependencies = new ArrayList<Dependency>(dependencyNodes.getLength());
        for (int i = 0; i < dependencyNodes.getLength(); i++) {
            Element dependencyElement = (Element) dependencyNodes.item(i);
            String name = dependencyElement.getAttribute(
                    "name");
            String group = dependencyElement.getAttribute("org");
            String version = dependencyElement.getAttribute("rev");
            if(version.contains("latest")) version = "latest";
            Dependency dependency = new Dependency(name, group, version);
            dependencies.add(dependency);
        }
        project.setDependencies(dependencies);
        return project;
    }

    private DependencyManagement getDependencyManagement(String dependencyDescriptionUrl) {
        return DependencyManagement.getValue(dependencyDescriptionUrl.substring(dependencyDescriptionUrl.lastIndexOf('/') + 1).replaceAll("\\?.+", ""));
    }
}
