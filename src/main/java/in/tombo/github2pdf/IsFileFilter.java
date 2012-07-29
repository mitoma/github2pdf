package in.tombo.github2pdf;

import java.io.File;
import java.io.FileFilter;

final class IsFileFilter implements FileFilter {
  @Override
  public boolean accept(File pathname) {
    return pathname.isFile();
  }
}