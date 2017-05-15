package au.gov.nsw.transport.tappa;

import au.gov.nsw.transport.tappa.model.RepositoryType;

class RepositoryCrawlerFactory {

    RepositoryCrawler create(RepositoryType repositoryType, String url) {
        switch (repositoryType) {
            case git: return new GitRepositoryCrawler(url);
            case svn: return new SubversionRepositoryCrawler(url);
            default: throw new RuntimeException("No RepositoryCrawler cannot be created for " + repositoryType);
        }
    }

}
