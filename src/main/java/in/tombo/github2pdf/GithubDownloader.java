package in.tombo.github2pdf;

import java.io.File;
import java.io.IOException;

import org.eclipse.egit.github.core.client.RequestException;
import org.eclipse.egit.github.core.service.RepositoryService;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.TransportException;

public class GithubDownloader {

  public Git openOrClone(String owner, String name) throws IOException,
      InvalidRemoteException, TransportException, GitAPIException {
    String gitUrl = getGitUrl(owner, name);
    CloneCommand cloneRepository = Git.cloneRepository();
    cloneRepository.setURI(gitUrl);
    File file = new File(makeStoredFilePath(owner, name));
    if (file.exists()) {
      return Git.open(file);
    }
    cloneRepository.setDirectory(file);
    return cloneRepository.call();
  }

  public String makeStoredFilePath(String owner, String name) {
    return "./target/git/" + owner + "/" + name + "/";
  }

  public String getGitUrl(String owner, String name) throws IOException {
    try {
      return new RepositoryService().getRepository(owner, name).getGitUrl();
    } catch (RequestException e) {
      throw e;
    }
  }
}
