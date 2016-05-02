package com.kbdunn.nimbus.web.util;

import java.io.File;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import org.vaadin.aceeditor.AceMode;

import com.kbdunn.nimbus.common.model.NimbusFile;
import com.kbdunn.vaadin.addons.fontawesome.FontAwesome;

public abstract class NimbusFileTypeResolver implements Serializable {

	private static final long serialVersionUID = 1L;
    static public String DEFAULT_MIME_TYPE = "application/octet-stream";
    
    static private String initialExtToMIMEMap = "application/cu-seeme                            csm cu,"
            + "application/dsptype                             tsp,"
            + "application/futuresplash                        spl,"
            + "application/mac-binhex40                        hqx,"
            + "application/msaccess                            mdb,"
            + "application/msword                              doc dot docx,"
            + "application/octet-stream                        bin,"
            + "application/oda                                 oda,"
            + "application/pdf                                 pdf,"
            + "application/pgp-signature                       pgp,"
            + "application/postscript                          ps ai eps,"
            + "application/rtf                                 rtf,"
            + "application/vnd.ms-excel                        xls xlb xlsx,"
            + "application/vnd.ms-powerpoint                   ppt pps pot pptx,"
            + "application/vnd.wap.wmlc                        wmlc,"
            + "application/vnd.wap.wmlscriptc                  wmlsc,"
            + "application/wordperfect5.1                      wp5,"
            + "application/zip                                 zip 7z,"
            + "application/x-123                               wk,"
            + "application/x-bcpio                             bcpio,"
            + "application/x-chess-pgn                         pgn,"
            + "application/x-cpio                              cpio,"
            + "application/x-debian-package                    deb,"
            + "application/x-director                          dcr dir dxr,"
            + "application/x-dms                               dms,"
            + "application/x-dvi                               dvi,"
            + "application/x-xfig                              fig,"
            + "application/x-font                              pfa pfb gsf pcf pcf.Z,"
            + "application/x-gnumeric                          gnumeric,"
            + "application/x-gtar                              gtar tgz taz,"
            + "application/x-hdf                               hdf,"
            + "application/x-httpd-php                         phtml pht php,"
            + "application/x-httpd-php3                        php3,"
            + "application/x-httpd-php3-source                 phps,"
            + "application/x-httpd-php3-preprocessed           php3p,"
            + "application/x-httpd-php4                        php4,"
            + "application/x-ica                               ica,"
            + "application/x-java-archive                      jar,"
            + "application/x-java-serialized-object            ser,"
            + "application/x-java-vm                           class,"
            + "application/x-javascript                        js,"
            + "application/x-kchart                            chrt,"
            + "application/x-killustrator                      kil,"
            + "application/x-kpresenter                        kpr kpt,"
            + "application/x-kspread                           ksp,"
            + "application/x-kword                             kwd kwt,"
            + "application/x-latex                             latex,"
            + "application/x-lha                               lha,"
            + "application/x-lzh                               lzh,"
            + "application/x-lzx                               lzx,"
            + "application/x-maker                             frm maker frame fm fb book fbdoc,"
            + "application/x-mif                               mif,"
            + "application/x-msdos-program                     com exe bat dll,"
            + "application/x-msi                               msi,"
            + "application/x-netcdf                            nc cdf,"
            + "application/x-ns-proxy-autoconfig               pac,"
            + "application/x-object                            o,"
            + "application/x-oz-application                    oza,"
            + "application/x-perl                              pl pm,"
            + "application/x-pkcs7-crl                         crl,"
            + "application/x-sql                               sql tsql plsql,"
            + "application/x-redhat-package-manager            rpm,"
            + "application/x-shar                              shar,"
            + "application/x-shockwave-flash                   swf swfl,"
            + "application/x-star-office                       sdd sda,"
            + "application/x-stuffit                           sit,"
            + "application/x-sv4cpio                           sv4cpio,"
            + "application/x-sv4crc                            sv4crc,"
            + "application/x-tar                               tar,"
            + "application/x-tex-gf                            gf,"
            + "application/x-tex-pk                            pk PK,"
            + "application/x-texinfo                           texinfo texi,"
            + "application/x-trash                             ~ % bak old sik,"
            + "application/x-troff                             t tr roff,"
            + "application/x-troff-man                         man,"
            + "application/x-troff-me                          me,"
            + "application/x-troff-ms                          ms,"
            + "application/x-ustar                             ustar,"
            + "application/x-wais-source                       src,"
            + "application/x-wingz                             wz,"
            + "application/x-x509-ca-cert                      crt,"
            + "audio/basic                                     au snd,"
            + "audio/midi                                      mid midi,"
            + "audio/mpeg                                      mpga mpega mp2 mp3,"
            + "audio/mpegurl                                   m3u,"
            + "audio/prs.sid                                   sid,"
            + "audio/x-aiff                                    aif aiff aifc,"
            + "audio/x-gsm                                     gsm,"
            + "audio/x-pn-realaudio                            ra rm ram,"
            + "audio/x-scpls                                   pls,"
            + "audio/x-wav                                     wav,"
            + "audio/ogg                                       ogg,"
            + "audio/mp4                                       m4a,"
            + "audio/x-aac                                     aac,"
            + "image/bitmap                                    bmp,"
            + "image/gif                                       gif,"
            + "image/ief                                       ief,"
            + "image/jpeg                                      jpeg jpg jpe,"
            + "image/pcx                                       pcx,"
            + "image/png                                       png,"
            + "image/svg+xml                                   svg svgz,"
            + "image/tiff                                      tiff tif,"
            + "image/vnd.wap.wbmp                              wbmp,"
            + "image/x-cmu-raster                              ras,"
            + "image/x-coreldraw                               cdr,"
            + "image/x-coreldrawpattern                        pat,"
            + "image/x-coreldrawtemplate                       cdt,"
            + "image/x-corelphotopaint                         cpt,"
            + "image/x-jng                                     jng,"
            + "image/x-portable-anymap                         pnm,"
            + "image/x-portable-bitmap                         pbm,"
            + "image/x-portable-graymap                        pgm,"
            + "image/x-portable-pixmap                         ppm,"
            + "image/x-rgb                                     rgb,"
            + "image/x-xbitmap                                 xbm,"
            + "image/x-xpixmap                                 xpm,"
            + "image/x-xwindowdump                             xwd,"
            + "text/comma-separated-values                     csv,"
            + "text/css                                        css scss sass,"
            + "text/html                                       htm html xhtml,"
            + "text/mathml                                     mml,"
            + "text/plain                                      txt text diff log properties,"
            + "text/richtext                                   rtx,"
            + "text/tab-separated-values                       tsv,"
            + "text/vnd.wap.wml                                wml,"
            + "text/vnd.wap.wmlscript                          wmls,"
            + "text/xml                                        xml,"
            + "text/x-c++hdr                                   h++ hpp hxx hh,"
            + "text/x-c++src                                   c++ cpp cxx cc,"
            + "text/x-chdr                                     h,"
            + "text/x-csh                                      csh,"
            + "text/x-csrc                                     c,"
            + "text/x-java                                     java,"
            + "text/x-moc                                      moc,"
            + "text/x-pascal                                   p pas,"
            + "text/x-setext                                   etx,"
            + "text/x-sh                                       sh script,"
            + "text/x-tcl                                      tcl tk,"
            + "text/x-tex                                      tex ltx sty cls,"
            + "text/x-vcalendar                                vcs,"
            + "text/x-vcard                                    vcf,"
            + "video/dl                                        dl,"
            + "video/fli                                       fli,"
            + "video/gl                                        gl,"
            + "video/mpeg                                      mpeg mpg mpe,"
            + "video/quicktime                                 qt mov,"
            + "video/x-mng                                     mng,"
            + "video/x-ms-asf                                  asf asx,"
            + "video/x-msvideo                                 avi,"
            + "video/x-sgi-movie                               movie,"
            + "video/ogg                                       ogv,"
            + "video/mp4                                       mp4,"
            + "video/x-ms-wmv                                  wmv,"
            + "audio/x-ms-wma                                  wma,"
            + "video/x-flv                                     flv,"
            + "video/webm                                      webm,"
            + "x-world/x-vrml                                  vrm vrml wrl,"
            + "audio/ac3                                       ac3,"
            + "audio/flac                                      flac,"
            + "video/x-matroska                                mkv,"
            + "audio/x-matroska                                mka";

    private static HashMap<String, String> extToMIMEMap = new HashMap<String, String>();
    private static HashMap<String, FontAwesome> MIMEToIconMap = new HashMap<String, FontAwesome>();
    private static HashMap<String, AceMode> MIMEToAceMap = new HashMap<String, AceMode>();
    
    static {
        // Initialize extension to MIME map
        final StringTokenizer lines = new StringTokenizer(initialExtToMIMEMap,
                ",");
        while (lines.hasMoreTokens()) {
            final String line = lines.nextToken();
            final StringTokenizer exts = new StringTokenizer(line);
            final String type = exts.nextToken();
            while (exts.hasMoreTokens()) {
                final String ext = exts.nextToken();
                addExtension(ext, type);
            }
           
            // Some easily set icons
            if (type.startsWith("video")) addIcon(type, FontAwesome.FILE_VIDEO_O);
            else if (type.startsWith("image")) addIcon(type, FontAwesome.FILE_IMAGE_O);
            else if (type.startsWith("audio")) addIcon(type, FontAwesome.FILE_AUDIO_O);
            else if (type.startsWith("text")
            		&& type != "text/plain" && type != "text/richtext" 
            		&& type != "text/tab-separated-values" && type != "text/comma-separated-values") addIcon(type, FontAwesome.FILE_CODE_O);
            else if (type.startsWith("application/x-httpd")) addIcon(type, FontAwesome.FILE_CODE_O);
            else if (type.startsWith("application/x-java") && !type.endsWith("archive")) addIcon(type, FontAwesome.FILE_CODE_O);

            if (type.startsWith("text")) addAceMode(type, AceMode.text);
            // Some easily set Ace modes
            for (AceMode mode : AceMode.values()) {
                if (type.trim().endsWith(mode.name().toLowerCase())) {
                	addAceMode(type, mode);
                }
            }
        }

        // Individual icons
        addIcon("inode/drive", FontAwesome.FOLDER_OPEN);
        addIcon("inode/directory", FontAwesome.FOLDER_OPEN);
        addIcon("text/plain", FontAwesome.FILE_TEXT_O);
        addIcon("text/richtext", FontAwesome.FILE_TEXT_O);
        addIcon("text/comma-separated-values", FontAwesome.FILE_EXCEL_O);
        addIcon("text/tab-separated-values", FontAwesome.FILE_EXCEL_O);
        addIcon("application/pdf", FontAwesome.FILE_PDF_O);
        addIcon("application/msword", FontAwesome.FILE_WORD_O);
        addIcon("application/rtf", FontAwesome.FILE_WORD_O);
        addIcon("application/vnd.ms-excel", FontAwesome.FILE_EXCEL_O);
        addIcon("application/vnd.ms-powerpoint", FontAwesome.FILE_POWERPOINT_O);
        addIcon("application/zip", FontAwesome.FILE_ARCHIVE_O);
        addIcon("application/x-gtar", FontAwesome.FILE_ARCHIVE_O);
        addIcon("application/x-java-archive", FontAwesome.FILE_ARCHIVE_O);
        addIcon("application/x-perl", FontAwesome.FILE_CODE_O);
        addIcon("application/x-sql", FontAwesome.FILE_CODE_O);
        addIcon("application/x-tar", FontAwesome.FILE_ARCHIVE_O);
        
        // Individual Ace modes
        addAceMode("application/x-javascript", AceMode.javascript);
        addAceMode("application/x-javascript", AceMode.javascript);
        addAceMode("application/x-perl", AceMode.perl);
        addAceMode("application/x-sql", AceMode.sql);
        addAceMode("application/x-x509-ca-cert", AceMode.text);
        addAceMode("text/html", AceMode.html);
        addAceMode("text/x-c++src", AceMode.c_cpp);
        addAceMode("text/x-c++hdr", AceMode.c_cpp);
        addAceMode("text/x-csh", AceMode.sh);
        addAceMode("text/x-csrc", AceMode.c_cpp);
        addAceMode("text/x-java", AceMode.java);
        addAceMode("text/x-pascal", AceMode.pascal);
        addAceMode("text/x-sh", AceMode.sh);
        addAceMode("text/x-tcl", AceMode.tcl);
        addAceMode("text/x-tex", AceMode.tex);
    }

    /**
     * Gets the mime-type of a file. Currently the mime-type is resolved based
     * only on the file name extension.
     * 
     * @param filename
     *            the name of the file whose mime-type is requested.
     * @return mime-type <code>String</code> for the given filename
     */
    public static String getMIMEType(String filename) {

        // Checks for nulls
        if (filename == null) {
            throw new NullPointerException("Filename can not be null");
        }

        String ext = getExtension(filename);
        
        // Return type from extension map, if found
        final String type = extToMIMEMap.get(ext.toLowerCase());
        if (type != null) {
            return type;
        }

        return DEFAULT_MIME_TYPE;
    }
    
    // Calculates the extension of the file
    public static String getExtension(String filename) {
		String ext = "";
		int dotIndex = filename.indexOf(".");
        while (dotIndex >= 0 && filename.indexOf(".", dotIndex + 1) >= 0) {
            dotIndex = filename.indexOf(".", dotIndex + 1);
        }
        dotIndex++;

        if (filename.length() > dotIndex) {
            ext = filename.substring(dotIndex);

            // Ignore any query parameters
            int queryStringStart = ext.indexOf('?');
            if (queryStringStart > 0) {
                ext = ext.substring(0, queryStringStart);
            }
        }
        return ext;
	}

    /**
     * Gets the descriptive icon representing file, based on the filename. First
     * the mime-type for the given filename is resolved, and then the
     * corresponding icon is fetched from the internal icon storage. If it is
     * not found the default icon is returned.
     * 
     * @param fileName
     *            the name of the file whose icon is requested.
     * @return the icon corresponding to the given file
     */
    public static FontAwesome getIcon(String fileName) {
        return getIconByMimeType(getMIMEType(fileName));
    }

    private static FontAwesome getIconByMimeType(String mimeType) {
        final FontAwesome icon = MIMEToIconMap.get(mimeType);
        if (icon != null) {
            return icon;
        }

        // If nothing is known about the file-type, general file
        // icon is used
        return FontAwesome.FILE_O;
    }

    /**
     * Gets the descriptive icon representing a file. First the mime-type for
     * the given file name is resolved, and then the corresponding icon is
     * fetched from the internal icon storage. If it is not found the default
     * icon is returned.
     * 
     * @param file
     *            the file whose icon is requested.
     * @return the icon corresponding to the given file
     */
    public static FontAwesome getIcon(NimbusFile file) {
        return getIconByMimeType(getMIMEType(file));
    }
    
    /**
     * Gets the Ace Editor mode for a particular file type. First the mime-type for
     * the given file name is resolved, and then the corresponding <code>AceMode</code> is
     * fetched. If it is not found null is returned.
     * 
     * @param file
     *            the file whose icon is requested.
     * @return the icon corresponding to the given file, or null if one isn't found
     */
    public static AceMode getAceMode(NimbusFile file) {
    	// Get more specific than MIME type, if possible
    	String ext = getExtension(file.getName());
    	AceMode extMode = null;
    	try {
    		extMode = AceMode.valueOf(ext.toLowerCase());
    	} catch (IllegalArgumentException e) {
    		// Ignore
    	}
    	if (extMode != null) return extMode;
    	else return MIMEToAceMap.get(getMIMEType(file));
    }
    
    public static boolean isPlainTextFile(NimbusFile file) {
    	return getAceMode(file) != null;
    }

    /**
     * Gets the mime-type for a file. Currently the returned file type is
     * resolved by the filename extension only.
     * 
     * @param file
     *            the file whose mime-type is requested.
     * @return the files mime-type <code>String</code>
     */
    private static String getMIMEType(NimbusFile file) {

        // Checks for nulls
        if (file == null) {
            throw new NullPointerException("File can not be null");
        }
        
        // Directories
        if (file.isDirectory()) {
            // Drives
            if (new File(file.getPath()).getParent() == null) {
                return "inode/drive";
            } else {
                return "inode/directory";
            }
        }

        // Return type from extension
        return getMIMEType(file.getName());
    }

    /**
     * Adds a mime-type mapping for the given filename extension. If the
     * extension is already in the internal mapping it is overwritten.
     * 
     * @param extension
     *            the filename extension to be associated with
     *            <code>MIMEType</code>.
     * @param MIMEType
     *            the new mime-type for <code>extension</code>.
     */
    private static void addExtension(String extension, String MIMEType) {
        extToMIMEMap.put(extension.toLowerCase(), MIMEType);
    }

    /**
     * Adds a icon for the given mime-type. If the mime-type also has a
     * corresponding icon, it is replaced with the new icon.
     * 
     * @param MIMEType
     *            the mime-type whose icon is to be changed.
     * @param icon
     *            the new icon to be associated with <code>MIMEType</code>.
     */
    private static void addIcon(String MIMEType, FontAwesome icon) {
        MIMEToIconMap.put(MIMEType, icon);
    }
    
    /**
     * Adds an AceMode for the given mime-type. If the mime-type also has a
     * corresponding AceMode, it is replaced with the new icon.
     * 
     * @param MIMEType
     *            the mime-type whose AceMode is to be changed.
     * @param aceMode
     *            the new <code>AceMode</code> to be associated with <code>MIMEType</code>.
     */
    private static void addAceMode(String MIMEType, AceMode aceMode) {
        MIMEToAceMap.put(MIMEType, aceMode);
    }

    /**
     * Gets the internal file extension to mime-type mapping.
     * 
     * @return unmodifiable map containing the current file extension to
     *         mime-type mapping
     */
    public static Map<String, String> getExtensionToMIMETypeMapping() {
        return Collections.unmodifiableMap(extToMIMEMap);
    }

    /**
     * Gets the internal mime-type to icon mapping.
     * 
     * @return unmodifiable map containing the current mime-type to icon mapping
     */
    public static Map<String, FontAwesome> getMIMETypeToIconMapping() {
        return Collections.unmodifiableMap(MIMEToIconMap);
    }
}
