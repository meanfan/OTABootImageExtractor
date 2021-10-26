package top.meansome.otabootimageextractor.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class UnzipUtil
{

    public static void unzip(InputStream zipFile, OutputStream unzippedFile) throws IOException {
        ZipInputStream zin = new ZipInputStream(zipFile);
        ZipEntry ze = null;
        while ((ze = zin.getNextEntry()) != null) {
            if(!ze.isDirectory() && ze.getName().endsWith(".bin")){
                byte[] buffer = new byte[8192];
                int len;
                while ((len = zin.read(buffer)) != -1)
                {
                    unzippedFile.write(buffer, 0, len);
                }
                break;
            }else{
                zin.skip(ze.getSize());
            }
        }
        zin.close();

    }
}
