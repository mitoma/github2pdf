package in.tombo.github2pdf;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.itextpdf.text.BadElementException;
import com.itextpdf.text.Chapter;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Font;
import com.itextpdf.text.Font.FontFamily;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Section;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfWriter;

public class PDFGenerator {

  Font sourceFontJapan;
  Font sourceFontAscii;
  int FONT_SIZE = 7;
  int LEADING_SIZE = FONT_SIZE + 1;

  public PDFGenerator() throws DocumentException, IOException {
    BaseFont bf = BaseFont.createFont("KozMinPro-Regular", "UniJIS-UCS2-H",
        false);
    sourceFontJapan = new Font(bf, FONT_SIZE);
    sourceFontAscii = FontFactory.getFont(FontFamily.COURIER.name(), FONT_SIZE);
  }

  public void generatePDF(File repo, String title, String path)
      throws FileNotFoundException, DocumentException {
    Document document = new Document(PageSize.A4);
    PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(
        path));
    try {
      document.open();
      Paragraph paragraph = new Paragraph(new Chunk(title, new Font(
          sourceFontAscii.getBaseFont(), 50)));
      paragraph.setAlignment(Paragraph.ALIGN_CENTER);
      paragraph.setSpacingBefore(100);

      document.add(paragraph);
      document.newPage();

      Chapter root = new Chapter(title, 1);
      writeSection(root, repo);
      document.add(root);
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      document.close();
      writer.close();
    }
  }

  private void writeSection(Section section, File path) {
    for (File f : path.listFiles(new IsFileFilter())) {
      Section child = section.addSection(f.getName());
      child.add(generateSourcePhrase(f));
      section.newPage();
    }
    for (File f : path.listFiles(new IsDirectoryFilter())) {
      if (f.getName().equals(".git")) {
        continue;
      }
      Section child = section.addSection(f.getName());
      writeSection(child, f);
    }
  }

  private Phrase generateSourcePhrase(File file) {
    if (!isTextFile(file)) {
      return generateImagePhrase(file);
    }

    try {
      InputStream in = new FileInputStream(file);
      String str = convertStreamToString(in);

      boolean isAscii = false;

      StringBuilder buf = new StringBuilder();
      List<Chunk> chankArray = new ArrayList<Chunk>();
      for (int i = 0; i < str.length(); i++) {
        char c = str.charAt(i);
        char[] cs = { c };
        if (StringUtils.isAsciiPrintable(new String(cs))) {
          if (isAscii) {
            buf.append(c);
          } else {
            chankArray.add(new Chunk(buf.toString(), sourceFontJapan));
            buf = new StringBuilder();
            buf.append(c);
          }
          isAscii = true;
        } else {
          if (!isAscii) {
            buf.append(c);
          } else {
            chankArray.add(new Chunk(buf.toString(), sourceFontAscii));
            buf = new StringBuilder();
            buf.append(c);
          }
          isAscii = false;
        }
      }
      if (isAscii) {
        chankArray.add(new Chunk(buf.toString(), sourceFontAscii));
      } else {
        chankArray.add(new Chunk(buf.toString(), sourceFontJapan));
      }
      Phrase phrase = new Phrase();

      phrase.setLeading(LEADING_SIZE);
      phrase.addAll(chankArray);
      return phrase;
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }

  private Phrase generateImagePhrase(File file) {
    try {
      System.out.println(file.getPath() + " is binary.");
      Phrase phrase = new Phrase();
      Image image;
      image = Image.getInstance(file.getPath());
      phrase.add(new Chunk("Image File\n\n", sourceFontAscii));
      phrase.add(new Chunk(image, 0, 0, true));

      phrase.setLeading(FONT_SIZE + 1);
      return phrase;
    } catch (BadElementException e) {
      return generateBinaryPhrase(file);
    } catch (MalformedURLException e) {
      return generateBinaryPhrase(file);
    } catch (IOException e) {
      return generateBinaryPhrase(file);
    }
  }

  private Phrase generateBinaryPhrase(File file) {
    Phrase phrase = new Phrase();
    phrase.add(new Chunk("UnSupported Binary", sourceFontAscii));
    phrase.setLeading(FONT_SIZE + 1);
    return phrase;
  }

  public String convertStreamToString(InputStream is) throws IOException {
    if (is != null) {
      Writer writer = new StringWriter();

      char[] buffer = new char[1024];
      try {
        Reader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
        int n;
        while ((n = reader.read(buffer)) != -1) {
          writer.write(buffer, 0, n);
        }
      } finally {
        is.close();
      }
      return writer.toString();
    } else {
      return "";
    }
  }

  /**
   * テキストファイルかどうかを判定する
   * 
   * @param filePath
   *          テキストファイル
   * @return trueならテキストファイル falseならバイナリファイル
   */
  public boolean isTextFile(File file) {
    FileInputStream in = null;
    try {
      in = new FileInputStream(file);

      byte[] b = new byte[1];
      while (in.read(b, 0, 1) > 0) {
        if (b[0] == 0) {
          return false;
        }
      }
      return true;
    } catch (IOException e) {
      e.printStackTrace();
      return false;
    } finally {
      if (in != null) {
        try {
          in.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
        in = null;
      }
    }
  }

}
