package top.meansome.otabootimageextractor;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import org.apache.commons.compress.compressors.CompressorException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;

import top.meansome.otabootimageextractor.utils.PayloadDumper;
import top.meansome.otabootimageextractor.utils.UnzipUtil;

import static android.content.Intent.EXTRA_MIME_TYPES;
import static android.content.Intent.EXTRA_TITLE;

public class MainActivity extends AppCompatActivity {
    public static final String TAG = "MainActivity";
    public static final int ACTIVITY_REQUEST_CODE_SELECT_OTA_FILE = 0;
    public static final int ACTIVITY_REQUEST_CODE_SELECT_BOOT_SAVE_FILE = 1;

    public static final String TMP_FILE_NAME_OTA_BIN = "ota.bin";
    public static final String TMP_FILE_NAME_BOOT_IMG = "boot.img";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button btn_select = findViewById(R.id.btn_select);
        btn_select.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toast("请选择OTA包");
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.setType("*/*");
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                //intent.putExtra(EXTRA_MIME_TYPES,"application/zip");
                MainActivity.this.startActivityForResult(intent, ACTIVITY_REQUEST_CODE_SELECT_OTA_FILE);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ACTIVITY_REQUEST_CODE_SELECT_OTA_FILE) {
            if (resultCode == Activity.RESULT_OK) {
                assert data != null;
                Uri otaFileUri = data.getData();
                try {
                    InputStream is = getContentResolver().openInputStream(otaFileUri);
                    toast("OTA包打开成功");
                    OutputStream os = openFileOutput(TMP_FILE_NAME_OTA_BIN, Context.MODE_PRIVATE);
                    toast("开始解压OTA包...");
                    new Thread(){
                        @Override
                        public void run() {
                            try {
                                UnzipUtil.unzip(is, os);
                                is.close();
                                toast("解压OTA包完成, 开始 dump boot.img ...");
                                new Thread(){
                                    @Override
                                    public void run() {
                                        PayloadDumper payloadDumper = null;
                                        try {
                                            payloadDumper = new PayloadDumper(new File(getFilesDir(), TMP_FILE_NAME_OTA_BIN));
                                            payloadDumper.dumpImage("boot", new File(getFilesDir(), TMP_FILE_NAME_BOOT_IMG));
                                            toast("dump boot.img 结束, 请选择保存位置");
                                            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                                            intent.setType("*/*");
                                            intent.addCategory(Intent.CATEGORY_OPENABLE);
                                            intent.putExtra(EXTRA_TITLE,"boot.img");
                                            MainActivity.this.startActivityForResult(intent, ACTIVITY_REQUEST_CODE_SELECT_BOOT_SAVE_FILE);
                                        } catch (IOException | CompressorException e) {
                                            toast("dump boot.img 失败！");
                                            deleteFile(TMP_FILE_NAME_BOOT_IMG);
                                            e.printStackTrace();
                                        }finally{
                                            deleteFile(TMP_FILE_NAME_OTA_BIN);
                                        }
                                    }
                                }.start();
                            } catch (IOException e) {
                                toast("解压OTA包失败！");
                                e.printStackTrace();
                            }
                        }
                    }.start();
                }catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }else if (requestCode == ACTIVITY_REQUEST_CODE_SELECT_BOOT_SAVE_FILE) {
            if (resultCode == Activity.RESULT_OK) {
                assert data != null;
                Uri saveFileUri = data.getData();
                new Thread(){
                    @Override
                    public void run() {
                        try {
                            toast("boot.img 保存中...");
                            OutputStream os = getContentResolver().openOutputStream(saveFileUri);
                            FileInputStream fis =new FileInputStream(new File(getFilesDir(), TMP_FILE_NAME_BOOT_IMG));
                            byte[] buf = new byte[1024];
                            int readLen = 0;
                            while((readLen = fis.read(buf))!=-1){
                                os.write(buf,0,readLen);
                            }
                            toast("boot.img 保存成功");
                        } catch (IOException e) {
                            toast("boot.img 保存失败！");
                            e.printStackTrace();
                        } finally{
                            deleteFile(TMP_FILE_NAME_BOOT_IMG);
                        }
                    }
                }.start();
            }
        }
    }


    private void toast(String msg){
        runOnUiThread(() -> Toast.makeText(MainActivity.this, msg,Toast.LENGTH_SHORT).show());
    }
}