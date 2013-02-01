package mollusc.linguasubtitle;

import java.io.File;

/**
 * Filter subtitles
 * @author mollusc <MolluscLab@gmail.com>
 */
public class SubtitleFilter extends javax.swing.filechooser.FileFilter {

    String ext, description;

    public String getDescription() {
        return description;
    }

    SubtitleFilter() {
        this.ext = ".srt";
        description = "Subtitles SRT";
    }

    public boolean accept(File f) {
        if (f != null) {
            if (f.isDirectory()) {
                return true;
            }
            return f.toString().endsWith(ext);
        }
        return false;
    }
}
