package au.gov.nsw.transport.tappa;

import au.gov.nsw.transport.tappa.model.Dependency;
import au.gov.nsw.transport.tappa.model.Project;
import au.gov.nsw.transport.tappa.model.RepositoryType;
import org.neo4j.driver.v1.AuthTokens;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.GraphDatabase;
import org.neo4j.driver.v1.Session;

import java.io.IOException;
import java.util.*;

import static org.neo4j.driver.v1.Values.parameters;

public class Application {

    private static Map<String, Project> projectMap = new HashMap<String, Project>();

    public static void main(String[] args) throws Exception {
        Properties properties = new Properties();
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        properties.load(loader.getResourceAsStream("repository.properties"));
        RepositoryCrawlerFactory factory = new RepositoryCrawlerFactory();
        Enumeration<?> propertyNames = properties.propertyNames();
        while (propertyNames.hasMoreElements()) {
            String type = (String) propertyNames.nextElement();
            String url = properties.getProperty(type);
            RepositoryCrawler crawler = factory.create(RepositoryType.valueOf(type), url);
            crawler.crawl();
        }
        save(projectMap.values());
    }

    private static void save(Collection<Project> projects) throws IOException {
        Properties properties = new Properties();
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        properties.load(loader.getResourceAsStream("neo4j.properties"));
        String uri = "bolt://" + properties.getProperty("host") + ":" + properties.getProperty("port");
        Driver driver = GraphDatabase.driver(uri, AuthTokens.basic(properties.getProperty("username"), properties.getProperty("password")));
        Session session = driver.session();
        session.run("MATCH (n) DETACH DELETE n");
        saveNodes(session, projects);
        saveEdges(session, projects);
        session.close();
        driver.close();
    }

    private static void saveNodes(Session session, Collection<Project> projects) {
        for (Project project : projects) {
            saveProject(session, project);
        }
    }

    private static void saveProject(Session session, Project project) {
        session.run("CREATE (p:Project {name: {name}, group: {group}, url: {url}, packaging: {packaging}, " +
                        "dependencyManagement: {dependencyManagement}})",
                parameters("name", project.getName(),
                        "group", project.getGroup(),
                        "url", project.getDependencyDescriptionUrl(),
                        "packaging", project.getPackaging() != null ? project.getPackaging().name() : "",
                        "dependencyManagement", project.getDependencyManagement().name()));
        saveDependencies(session, project.getDependencies());
    }

    private static void saveDependencies(Session session, Collection<Dependency> dependencies) {
        for (Dependency dependency : dependencies) {
            saveDependency(session, dependency);
        }
    }

    private static boolean isProject(Dependency dependency) {
        return get(dependency.getKey()) != null && !dependency.hasSpecificVersion();
    }

    private static void saveDependency(Session session, Dependency dependency) {
        if(!isProject(dependency)) {
            session.run("MERGE (d:Dependency{name: {name}, group: {group}})",
                    parameters("name", dependency.getNodeName(), "group", dependency.getGroup()));
        }
    }

    private static void saveEdges(Session session, Collection<Project> projects) {
        for (Project project : projects) {
            if(project.getParent() != null) {
                session.run("MATCH (pp:Project{name: {ppName}, group: {ppGroup}}), (p:Project{name: {pName}, group: {pGroup}}) " +
                        "CREATE (pp)-[:IS_PARENT_OF]->(p)",
                        parameters("ppName", project.getParent().getName(),
                                "ppGroup", project.getParent().getGroup(),
                                "pName", project.getName(),
                                "pGroup", project.getGroup()));
            }
            for (Dependency dependency : project.getDependencies()) {
                session.run("MATCH (p:Project{name:{pName}, group:{pGroup}}), (d:" +
                                (isProject(dependency) ? "Project" : "Dependency")+ "{name:{dName}, group:{dGroup}}) " +
                                "CREATE (p)-[:DEPENDS_ON]->(d)",
                        parameters("pName", project.getName(),
                                "pGroup", project.getGroup(),
                                "dName", dependency.getNodeName(),
                                "dGroup", dependency.getGroup()));
            }
        }
    }

    static void put(Project project) {
        projectMap.put(project.getKey(), project);
    }

    static Project get(String key) {
        return projectMap.get(key);
    }
}
