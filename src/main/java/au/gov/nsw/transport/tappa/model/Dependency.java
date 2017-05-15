package au.gov.nsw.transport.tappa.model;

import au.gov.nsw.transport.tappa.Application;

public class Dependency {
    String name;
    String group;
    private String version;

    Dependency() {
    }

    public Dependency(String name, String group, String version) {
        this.name = name;
        this.group = group;
        this.version = version;
    }

    public String getName() {
        return name;
    }

    public String getGroup() {
        return group;
    }

    String getVersion() {
        return version;
    }

    public String getKey() {
        return group + ":" + name;
    }

    @Override
    public String toString() {
        return "\nDependency{" +
                "name='" + name + '\'' +
                ", group='" + group + '\'' +
                ", version='" + version + '\'' +
                '}';
    }

    public String getNodeName() {
        return name  + (!hasSpecificVersion() ? "" : "-" + version);
    }

    public boolean hasSpecificVersion() {
        return version != null && !version.equals("latest");
    }

}
