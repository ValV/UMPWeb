package valv.umpweb;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.os.ResultReceiver;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

/**
 * Created by ValV on 6/15/15.
 * TODO: Legacy download method (remove)
 */
public class DownloadService extends IntentService {

    public static final int UPDATE_PROGRESS = 8344;
    public static final String URL_ADDRESS = "urlAddr";
    public static final String FILE_NAME = "fileName";
    public static final String RESULT_RECEIVER = "rcvDlResult";
    public static final String DOWNLOAD_PROGRESS = "dlProgress";

    public DownloadService() {
        super("DownloadService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        //
        String strRemoteFile = intent.getStringExtra(URL_ADDRESS);
        String strLocalFile = intent.getStringExtra(FILE_NAME);
        ResultReceiver rcvDlResult = (ResultReceiver) intent.getParcelableExtra(RESULT_RECEIVER);
        try {
            // Get URL, open connection and connect
            URL urlUmpFile = new URL(strRemoteFile);
            URLConnection ucUmpConnection = urlUmpFile.openConnection();
            ucUmpConnection.connect();
            // Get total file length
            int iDlFileSize = ucUmpConnection.getContentLength();
            // Download the file streams
            InputStream inRemoteFile = new BufferedInputStream(ucUmpConnection.getInputStream());
            OutputStream outLocalFile = new FileOutputStream(strLocalFile);
            // Download by 1024 bytes
            byte bChunk[] = new byte[1024];
            long lTotalDl = 0;
            int iBytesCount;
            while ((iBytesCount = inRemoteFile.read(bChunk)) != -1) {
                // Got some bytes from InputStream
                lTotalDl += iBytesCount;
                // Report progress
                Bundle rsltData = new Bundle();
                rsltData.putInt(DOWNLOAD_PROGRESS, (int) (lTotalDl * 100 / iDlFileSize));
                rcvDlResult.send(UPDATE_PROGRESS, rsltData);
                // Write out the data chunk
                outLocalFile.write(bChunk, 0, iBytesCount);
            }
        } catch (IOException e) {
            //
            e.printStackTrace();
        }
        Bundle rsltData = new Bundle();
        rsltData.putInt(DOWNLOAD_PROGRESS, 100);
        rcvDlResult.send(UPDATE_PROGRESS, rsltData);
    }
}
