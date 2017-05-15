package au.gov.nsw.transport.tappa.model;

public enum DependencyManagement {

    Ivy("ivy.xml"), Maven("pom.xml");

    private String configurationFile;

    public String getConfigurationFile() {
        return configurationFile;
    }

    DependencyManagement(String configurationFile) {
        this.configurationFile = configurationFile;
    }

    public static DependencyManagement getValue(String configurationFile) {
        for(DependencyManagement dependencyManagement : values()) {
            if(dependencyManagement.configurationFile.equalsIgnoreCase(configurationFile)) {
                return dependencyManagement;
            }
        }
        return null;
    }
}
