package in.tombo.github2pdf;

import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jgit.api.Git;

import twitter4j.Status;
import twitter4j.StatusUpdate;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.TwitterStream;
import twitter4j.TwitterStreamFactory;
import twitter4j.UserStreamAdapter;

public class Github2PdfTwitter {

  private static final class UserStreamProcessor extends UserStreamAdapter {
    private static Twitter twitter = TwitterFactory.getSingleton();
    private static Pattern p = Pattern.compile("([^/ ]+)/([^/ ]+)");

    @Override
    public void onStatus(Status status) {
      if (status.isRetweet()) {
        return;
      }
      if (status.getUser().getScreenName().equals("github2pdf")) {
        return;
      }

      Matcher m = p.matcher(status.getText());
      if (!m.find()) {
        return;
      }
      String owner = m.group(1);
      String name = m.group(2);
      GithubDownloader downloader = new GithubDownloader();
      try {
        downloader.getGitUrl(owner, name);
      } catch (IOException e1) {
        StatusUpdate statusUpdate = new StatusUpdate(String.format(
            "@%s Oh %s/%s is not found. :-(", status.getUser().getScreenName(),
            owner, name));
        statusUpdate.setInReplyToStatusId(status.getId());
        try {
          twitter.updateStatus(statusUpdate);
        } catch (TwitterException e) {
          e.printStackTrace();
        }
        return;
      }

      StatusUpdate firstReplay = new StatusUpdate(String.format(
          "@%s %s/%s is accepted.", status.getUser().getScreenName(), owner,
          name));
      firstReplay.setInReplyToStatusId(status.getId());
      try {
        twitter.updateStatus(firstReplay);
      } catch (TwitterException e1) {
        e1.printStackTrace();
      }

      try {
        Git git = downloader.openOrClone(owner, name);

        String dir = "./target/pdf/" + owner;
        new File(dir).mkdirs();
        new PDFGenerator().generatePDF(git.getRepository().getWorkTree(), name,
            dir + "/" + name + ".pdf");
      } catch (Exception e) {
        e.printStackTrace();
      }

      StatusUpdate lastReplay = new StatusUpdate(
          String.format("@%s %s/%s is hear %s.", status.getUser()
              .getScreenName(), owner, name, "http://github2pdf.tombo.in/pdf/"
              + owner + "/" + name + ".pdf"));
      lastReplay.setInReplyToStatusId(status.getId());
      try {
        twitter.updateStatus(lastReplay);
      } catch (TwitterException e1) {
        e1.printStackTrace();
      }
    }
  }

  public static void main(String[] args) {
    TwitterStream twitterStream = TwitterStreamFactory.getSingleton();
    twitterStream.addListener(new UserStreamProcessor());
    twitterStream.user();
  }
}
