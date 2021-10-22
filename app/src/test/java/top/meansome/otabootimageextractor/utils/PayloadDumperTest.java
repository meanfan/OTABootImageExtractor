package top.meansome.otabootimageextractor.utils;

import org.apache.commons.compress.compressors.CompressorException;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import static org.junit.Assert.*;

public class PayloadDumperTest {
    public static final String SAMPLE_PAYLOAD_PATH = "D:\\WorkFiles\\AndroidWork\\AndroidStudioProjects\\OTABootImageExtractor\\samples\\payload.bin";
    public static final String SAMPLE_OUTPUT_PATH = "D:\\WorkFiles\\AndroidWork\\AndroidStudioProjects\\OTABootImageExtractor\\samples\\boot.img";

    @Test
    public void checkMagic() throws IOException {
        File file = new File(SAMPLE_PAYLOAD_PATH);
        PayloadDumper payloadDumper = new PayloadDumper(file);
        assertTrue(payloadDumper.checkMagic());
    }

    @Test
    public void getFileFormatVersion() throws IOException {
        File file = new File(SAMPLE_PAYLOAD_PATH);
        PayloadDumper payloadDumper = new PayloadDumper(file);
        assertEquals(2, payloadDumper.getFileFormatVersion());
    }

    @Test
    public void checkFileFormatVersion() throws IOException {
        File file = new File(SAMPLE_PAYLOAD_PATH);
        PayloadDumper payloadDumper = new PayloadDumper(file);
        assertTrue(payloadDumper.checkFileFormatVersion());
    }

    @Test
    public void getManifestSize() throws IOException {
        File file = new File(SAMPLE_PAYLOAD_PATH);
        PayloadDumper payloadDumper = new PayloadDumper(file);
        assertEquals(0x0002DD16, payloadDumper.getManifestSize());
    }

    @Test
    public void getMetadataSignatureSize() throws IOException {
        File file = new File(SAMPLE_PAYLOAD_PATH);
        PayloadDumper payloadDumper = new PayloadDumper(file);
        assertEquals(0x0000010B, payloadDumper.getMetadataSignatureSize());
    }

    @Test
    public void getManifest() throws IOException {
        File file = new File(SAMPLE_PAYLOAD_PATH);
        PayloadDumper payloadDumper = new PayloadDumper(file);
        byte[] manifest = payloadDumper.getManifest();
        assertEquals(payloadDumper.getManifestSize(), manifest.length);
    }

    @Test
    public void getMetadataSignature() throws IOException {
        File file = new File(SAMPLE_PAYLOAD_PATH);
        PayloadDumper payloadDumper = new PayloadDumper(file);
        assertEquals(payloadDumper.getMetadataSignatureSize(), payloadDumper.getMetadataSignature().length);
    }

    @Test
    public void dumpImage() throws IOException, CompressorException {
        File file = new File(SAMPLE_PAYLOAD_PATH);
        PayloadDumper payloadDumper = new PayloadDumper(file);
        File outputFile = new File(SAMPLE_OUTPUT_PATH);
        payloadDumper.dumpImage("boot", outputFile);
        //TODO check outputFile
    }


}