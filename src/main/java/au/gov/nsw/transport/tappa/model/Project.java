package au.gov.nsw.transport.tappa.model;

import java.util.List;

public class Project extends Dependency {

    private Project parent;
    private Packaging packaging;
    private String dependencyDescriptionUrl;
    private DependencyManagement dependencyManagement;
    private List<Dependency> dependencies;


    public Project(String name, String group, Packaging packaging,
            String dependencyDescriptionUrl, DependencyManagement dependencyManagement) {
        this.name = name;
        this.group = group;
        this.packaging = packaging;
        this.dependencyDescriptionUrl = dependencyDescriptionUrl;
        this.dependencyManagement = dependencyManagement;
    }

    public Project getParent() {
        return parent;
    }

    public void setParent(Project parent) {
        this.parent = parent;
    }

    public String getName() {
        return name;
    }

    public Packaging getPackaging() {
        return packaging;
    }

    public String getDependencyDescriptionUrl() {
        return dependencyDescriptionUrl;
    }

    public DependencyManagement getDependencyManagement() {
        return dependencyManagement;
    }

    public void setDependencies(List<Dependency> dependencies) {
        this.dependencies = dependencies;
    }

    public List<Dependency> getDependencies() {
        return dependencies;
    }

    public String getDependencyVersion(String name, String group) {
        if(dependencies != null) {
            for (Dependency dependency : dependencies) {
                if(dependency.getGroup().equals(group) && dependency.getName().equals(name)) {
                    return dependency.getVersion();
                }
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return "Project{" +
                "name='" + name + '\'' +
                ", group='" + group + '\'' +
                ", packaging=" + packaging +
                ", dependencyDescriptionUrl='" + dependencyDescriptionUrl + '\'' +
                ", dependencyManagement=" + dependencyManagement +
                ", dependencies=" + dependencies +
                '}';
    }

    @Override
    public String getNodeName() {
        return name;
    }
}
