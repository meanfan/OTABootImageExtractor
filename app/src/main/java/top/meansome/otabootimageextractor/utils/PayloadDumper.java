package top.meansome.otabootimageextractor.utils;

import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

import top.meansome.otabootimageextractor.utils.UpdateMetadata.*;

public class PayloadDumper {
    public static final int[] MAGIC_NUMBER = {'C', 'r', 'A', 'U'};
    public static final int FILE_FORMAT_VERSION_BYTES_LENGTH = 8;
    public static final int MANIFEST_SIZE_BYTES_LENGTH = 8;
    public static final int METADATA_SIGNATURE_SIZE_BYTES_LENGTH = 4;
    public static final int SUPPORTED_FILE_FORMAT_VERSION = 2;

    private RandomAccessFile mInputFile;

    PayloadDumper(File inputFile) throws IOException {
        mInputFile = new RandomAccessFile(inputFile, "r");
        checkMagic();
        checkFileFormatVersion();
    }

    @Override
    protected void finalize() throws Throwable {
        if(mInputFile != null){
            mInputFile.close();
        }
    }

    private int u64(byte[] b){
        if(b.length != 8){
            throw new RuntimeException("u64() input bytes length is not 8");
        }
        int rst = 0;
        for(int i=0;i<8;i++){
            int cInt = (b[i]>= 0 ? b[i] : b[i] + 256);  // unsigned integer byte to Java int
            rst += cInt<<(7-i)*8;
        }
        return rst;
    }

    private int u32(byte[] b){
        if(b.length != 4){
            throw new RuntimeException("u32() input bytes length is not 4");
        }
        int rst = 0;
        for(int i=0;i<4;i++){
            int cInt = (b[i]>= 0 ? b[i] : b[i] + 256);  // unsigned integer byte to Java int
            rst += cInt<<(3-i)*8;
        }
        return rst;
    }

    private byte[] un(byte[] b){
        if(b.length == 0){
            throw new RuntimeException("un() input bytes length should not be 0");
        }
        for(int i=0;i<b.length;i++){
            if(b[i]>= 0){
                b[i]+=256;
            }
        }
        return b;
    }

    public boolean checkMagic() throws IOException {
        byte[] magicBytes = new byte[MAGIC_NUMBER.length];
        mInputFile.seek(0);
        if(mInputFile.read(magicBytes) == MAGIC_NUMBER.length){
            boolean isMatch = true;
            for(int i=0;i<MAGIC_NUMBER.length;i++){
                if(magicBytes[i] != MAGIC_NUMBER[i]){
                    isMatch = false;
                    break;
                }
            }
            return isMatch;
        }else{
            return false;
        }
    }

    public boolean checkFileFormatVersion() throws IOException {
        return getFileFormatVersion() == SUPPORTED_FILE_FORMAT_VERSION;
    }

    public int getFileFormatVersion() throws IOException {
        byte[] fileFormatVersionBytes = new byte[FILE_FORMAT_VERSION_BYTES_LENGTH];
        mInputFile.seek(MAGIC_NUMBER.length);
        if(mInputFile.read(fileFormatVersionBytes) == FILE_FORMAT_VERSION_BYTES_LENGTH){
            return u64(fileFormatVersionBytes);
        }else{
            return -1;
        }
    }

    public int getManifestSize() throws IOException {
        byte[] manifestSizeBytes = new byte[MANIFEST_SIZE_BYTES_LENGTH];
        mInputFile.seek(MAGIC_NUMBER.length + FILE_FORMAT_VERSION_BYTES_LENGTH);
        if(mInputFile.read(manifestSizeBytes) == MANIFEST_SIZE_BYTES_LENGTH){
            return u64(manifestSizeBytes);
        }else{
            return -1;
        }
    }

    public int getMetadataSignatureSize() throws IOException {
        if(getFileFormatVersion()<=1){
            return 0;
        }
        byte[] metadataSignatureSizeBytes = new byte[METADATA_SIGNATURE_SIZE_BYTES_LENGTH];
        mInputFile.seek(MAGIC_NUMBER.length + FILE_FORMAT_VERSION_BYTES_LENGTH + MANIFEST_SIZE_BYTES_LENGTH);
        if(mInputFile.read(metadataSignatureSizeBytes) == METADATA_SIGNATURE_SIZE_BYTES_LENGTH){
            return u32(metadataSignatureSizeBytes);
        }else{
            return -1;
        }
    }

    public byte[] getManifest() throws IOException {
        int manifestLength = getManifestSize();
        if(manifestLength <=0){
            return new byte[0];
        }
        byte[] manifestBytes = new byte[manifestLength];
        mInputFile.seek(MAGIC_NUMBER.length + FILE_FORMAT_VERSION_BYTES_LENGTH + MANIFEST_SIZE_BYTES_LENGTH +
                (getFileFormatVersion()<=1? 0: METADATA_SIGNATURE_SIZE_BYTES_LENGTH));
        if(mInputFile.read(manifestBytes) == manifestLength){
            return manifestBytes;
        }else{
            return new byte[0];
        }
    }

    public byte[] getMetadataSignature() throws IOException {
        int metadataSignatureLength = getMetadataSignatureSize();
        if(metadataSignatureLength <=0){
            return new byte[0];
        }
        byte[] metadataSignatureBytes = new byte[metadataSignatureLength];
        mInputFile.seek(MAGIC_NUMBER.length + FILE_FORMAT_VERSION_BYTES_LENGTH + MANIFEST_SIZE_BYTES_LENGTH +
                METADATA_SIGNATURE_SIZE_BYTES_LENGTH + getManifestSize());
        if(mInputFile.read(metadataSignatureBytes) == metadataSignatureLength){
            return metadataSignatureBytes;
        }else{
            return new byte[0];
        }
    }

    public void dumpImage(String partitionName, File outputFile) throws IOException, CompressorException {
        byte[] manifestBytes = getManifest();
        DeltaArchiveManifest dam = DeltaArchiveManifest.parseFrom(manifestBytes);
        int blockSize = dam.getBlockSize();
        for(PartitionUpdate pu : dam.getPartitionsList()){
            if(pu.getPartitionName().equalsIgnoreCase(partitionName)){
                for(InstallOperation op : pu.getOperationsList()){
                    dataForOperation(op, outputFile, blockSize);
                }
            }
        }
    }

    private void dataForOperation(InstallOperation op, File outputFile, int blockSize) throws IOException, CompressorException {
        long dataOffset = MAGIC_NUMBER.length + FILE_FORMAT_VERSION_BYTES_LENGTH + MANIFEST_SIZE_BYTES_LENGTH +
                METADATA_SIGNATURE_SIZE_BYTES_LENGTH + getManifestSize() + getMetadataSignatureSize();
        int dataLength = (int) op.getDataLength(); // TODO cast long to int may cause problem
        mInputFile.seek(dataOffset + op.getDataOffset());
        byte[] dataBytes = new byte[dataLength];
        outputFile.createNewFile();
        RandomAccessFile of = new RandomAccessFile(outputFile, "rw");
        InputStream is = new ByteArrayInputStream(dataBytes);
        if(mInputFile.read(dataBytes)==dataLength){
            if(op.getType() == InstallOperation.Type.REPLACE_XZ){
                CompressorInputStream cis = new CompressorStreamFactory().createCompressorInputStream(is);
                of.seek(op.getDstExtents(0).getStartBlock()* blockSize);
                byte[] buf = new byte[1024];
                int readLen = 0;
                while((readLen = cis.read(buf))!=-1){
                    of.write(buf,0,readLen);
                }
            }else if(op.getType() == InstallOperation.Type.REPLACE_BZ){
                CompressorInputStream cis = new CompressorStreamFactory().createCompressorInputStream(is);
                of.seek(op.getDstExtents(0).getStartBlock()* blockSize);
                byte[] buf = new byte[1024];
                int readLen = 0;
                while((readLen = cis.read(buf))!=-1){
                    of.write(buf,0,readLen);
                }
            }else if(op.getType() == InstallOperation.Type.REPLACE){
                of.seek(op.getDstExtents(0).getStartBlock()* blockSize);
                byte[] buf = new byte[1024];
                int readLen = 0;
                while((readLen = is.read(buf))!=-1){
                    of.write(buf,0,readLen);
                }
            }else if(op.getType() == InstallOperation.Type.SOURCE_COPY){
                throw new RuntimeException("SOURCE_COPY supported only for differential OTA");
            }else if(op.getType() == InstallOperation.Type.SOURCE_BSDIFF){
                throw new RuntimeException("SOURCE_BSDIFF supported only for differential OTA");
            }else if(op.getType() == InstallOperation.Type.ZERO) {
                for (Extent ex : op.getDstExtentsList()){
                    of.seek(ex.getStartBlock() * blockSize);
                    long zeroNum = ex.getNumBlocks() * blockSize;
                    while (zeroNum-- != 0) {
                        of.write(0x00);
                    }
                }
            }else{
                throw new RuntimeException(String.format("Unsupported type = %d", op.getType().getNumber()));
            }
        }
    }

}
