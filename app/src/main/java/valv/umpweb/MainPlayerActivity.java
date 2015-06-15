package valv.umpweb;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.ResultReceiver;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;


public class MainPlayerActivity extends Activity {

    /*/ TODO: Legacy download method (remove or replace DownloadManager)
    private class DownloadReceiver extends ResultReceiver {

        DownloadReceiver(Handler handler) {
            super(handler);
        }

        @Override
        protected void onReceiveResult(int rsltCode, Bundle rsltData) {
            super.onReceiveResult(rsltCode, rsltData);
            if (rsltCode == DownloadService.UPDATE_PROGRESS) {
                int iDlProgress = rsltData.getInt(DownloadService.DOWNLOAD_PROGRESS);
                if (dlTrack >= 0) {
                    // Set Progress in the Track Title
                    UmpWebTrack uwtTrack = (UmpWebTrack) llvTrackList.getChildAt(dlTrack);
                    if (uwtTrack != null) {
                        ((Button) uwtTrack.getChildAt(0)).setText(uwtTrack.getTrackName() + " ("
                                + String.valueOf(iDlProgress) + ")");
                    }
                }
            }
        }
    } /*/

    private final String PLAYLIST_URL =
            "http://dl-web.dropbox.com/get/UMPWeb/UMPWeb.list?_subject_uid=70215420&w=AAA884ddzWM4io97yDSA2JfFLWLfdRigF69QfWYnJ9LJUg&dl=1";
            //"http://plasmon.rghost.ru/download/65YZ6ZmsS/464166ce411cd40cd55389be05de3823fcf0404c/UMPWeb.list";
            //"https://www.dropbox.com/s/ivsw18bo669erwc/UMPWeb.list?dl=0";
    private final String PLAYLIST_NAME = "UMPWeb.list";
    private File SOURCE_DIR;
    //private int dlTrack = -1; // Supposed to work with legacy download method
    private Button btnFetchPlaylist;
    private LinearLayout llvTrackList;
    private TextView tvStatusMessage;

    private DownloadManager dmUfmDownloader = null;
    private long lLastDl = -1L;

    protected MediaPlayer umpMediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_player);
        // DownloadManager and Receivers initialization
        dmUfmDownloader = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        registerReceiver(onComplete,
                new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        registerReceiver(onNotificationClick,
                new IntentFilter(DownloadManager.ACTION_NOTIFICATION_CLICKED));
        //
        SOURCE_DIR = Environment
                .getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC + "/UMPWeb");
        btnFetchPlaylist = (Button) findViewById(R.id.btnFetchPlaylist);
        llvTrackList = (LinearLayout) findViewById(R.id.llvTrackList);
        tvStatusMessage = (TextView) findViewById(R.id.tvStatusMessage);
        // MediaPlayer initialization
        umpMediaPlayer = new MediaPlayer();
        umpMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                mediaPlayer.start();
            }
        });
        // Set callbacks
        btnFetchPlaylist.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                fetchPlaylist(true);
            }
        });
        btnFetchPlaylist.setTextSize(TypedValue.COMPLEX_UNIT_SP, 32);
        fetchPlaylist(false);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main_player, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        // noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        // Release MediaPlayer resources on Activity destruction
        if ((umpMediaPlayer != null) && (umpMediaPlayer.isPlaying()))
            umpMediaPlayer.stop();

        // Unregister receivers if DownloadManager is used
        unregisterReceiver(onComplete);
        unregisterReceiver(onNotificationClick);
        //

        if (umpMediaPlayer != null) umpMediaPlayer.release();
        super.onDestroy();
    }

    protected boolean isExternalStorageAvailable(boolean isReadOnly) {
        // Check External Storage availability (read-write or read-only)
        String exState = Environment.getExternalStorageState();
        return (Environment.MEDIA_MOUNTED.equals(exState)
                || (Environment.MEDIA_MOUNTED_READ_ONLY.equals(exState) && isReadOnly));
    }

    protected boolean isExternalStorageAvailable() {
        // Override: default - available read-write
        return isExternalStorageAvailable(false);
    }

    protected void fetchPlaylist(boolean forceReload) {
        if (!isExternalStorageAvailable(true) || (SOURCE_DIR == null)) {
            // If SD is not mounted at all - report
            if (SOURCE_DIR == null)
                tvStatusMessage.setText("SOURCE_DIR is NULL");
            else
                tvStatusMessage.setText("SD Card is not available");
        } else if (isExternalStorageAvailable() && forceReload) {
            // SD available read-write - download Playlist
            if (!SOURCE_DIR.exists()) {
                if (SOURCE_DIR.mkdirs())
                    tvStatusMessage.setText("Created: " + SOURCE_DIR.getAbsolutePath());
                else tvStatusMessage.setText("Directory is Ok");
            }
            // Download Playlist
            fetchFile(PLAYLIST_URL, /*SOURCE_DIR.getAbsolutePath() + "/" +*/PLAYLIST_NAME);
            // TODO: File download queue (add)
            // rebuildTrackList(SOURCE_DIR.getAbsolutePath() + "/" + PLAYLIST_NAME);
        } else {
            // SD available read-only - read existing Playlist
            if (forceReload) tvStatusMessage.setText("SD is read-only. Reading Playlist...");
                // Read Playlist in spite of how SD is mounted
            else tvStatusMessage.setText("Reading Playlist...");
            if (SOURCE_DIR.exists()) {
                File fTrackList = new File(SOURCE_DIR.getAbsolutePath() + "/" + PLAYLIST_NAME);
                if (fTrackList.exists()) {
                    tvStatusMessage.setText("Playlist exists: " + fTrackList.getAbsoluteFile());
                    rebuildTrackList(fTrackList.getAbsolutePath());
                } else tvStatusMessage.setText("Playlist does not exist!");
            } else {
                tvStatusMessage.setText("Directory doesn't exist and SD is read-only");
                // Show examples
                rebuildTrackList(null);
            }
        }
    }

    protected void rebuildTrackList(String trackListPath) {
        // Build TrackList based on Playlist
        FileReader frTracks = null;
        LineNumberReader lnrTracks = null;
        try {
            if (trackListPath != null) {
                frTracks = new FileReader(trackListPath);
                lnrTracks = new LineNumberReader(frTracks);
                if (llvTrackList != null) {
                    llvTrackList.removeAllViews();
                    String strTrackLine;
                    while ((strTrackLine = lnrTracks.readLine()) != null) {
                        UmpWebTrack uwtTrack = new UmpWebTrack(this);
                        String[] strUrlName = strTrackLine.trim().split("\t");
                        llvTrackList.addView(uwtTrack);
                        if (strUrlName.length > 1) {
                            uwtTrack.setTrackUrl(strUrlName[0]);
                            uwtTrack.setTrackName(strUrlName[1]);
                        } else uwtTrack.setTrackName(strUrlName[0]);
                        uwtTrack.setTitle();
                    }
                    fetchTagsFiles();
                }
            } else {
                // This may be removed (not mandatory)
                tvStatusMessage.setText("Playlist Path is NULL. Perform sample");
                if (llvTrackList != null) {
                    llvTrackList.removeAllViews();
                    for (int i = 0; i < 5; i++) {
                        UmpWebTrack uwtTrack = new UmpWebTrack(this);
                        llvTrackList.addView(uwtTrack);
                        uwtTrack.setTitle("Unknown Track " + String.valueOf(i));
                    }
                }
            }
        } catch (IOException e) {
            tvStatusMessage.setText(e.getMessage());
        } finally {
            try {
                if (lnrTracks != null) lnrTracks.close();
                if (frTracks != null) frTracks.close();
            } catch (IOException e) {
                tvStatusMessage.setText(e.getMessage());
            }
        }
    }

    public void fetchFile(String urlAddress, String fileName) {
        fetchFile(urlAddress, fileName, -1);
    }

    public void fetchFile(String urlAddress, String fileName, int trackIndex) {
        /*/ TODO: Legacy download method (remove or replace DownloadManager)
        Intent intent = new Intent(this, DownloadService.class);
        intent.putExtra(DownloadService.URL_ADDRESS, urlAddress);
        intent.putExtra(DownloadService.FILE_NAME, SOURCE_DIR.getAbsolutePath() + "/" + fileName);
        intent.putExtra(DownloadService.RESULT_RECEIVER, new DownloadReceiver(new Handler()));
        startService(intent); /*/
        // Enqueue with DownloadManager
        Uri uriRemoteFile = Uri.parse(urlAddress);
        lLastDl = dmUfmDownloader.enqueue(new DownloadManager.Request(uriRemoteFile)
                .setAllowedNetworkTypes(DownloadManager.Request.NETWORK_MOBILE
                        | DownloadManager.Request.NETWORK_WIFI)
                .setAllowedOverRoaming(false)
                .setTitle(fileName)
                .setDescription(urlAddress)
                .setDestinationInExternalPublicDir(SOURCE_DIR.getAbsolutePath(), fileName));
        //
    }

    private void fetchTagsFiles() {
        if (llvTrackList != null) {
            int iTrackNum = llvTrackList.getChildCount();
            for (int i = 0; i < iTrackNum; i ++) {
                UmpWebTrack uwtTrack = (UmpWebTrack) llvTrackList.getChildAt(i);
                File fTrack = new File(SOURCE_DIR.getAbsolutePath() + "/" + uwtTrack.getTrackName());
                if (fTrack.exists()) {
                    // File already downloaded - get mp3 tags
                    uwtTrack.setTitle(fetchTagInfo(fTrack.getAbsolutePath()));
                    uwtTrack.setLoaded(true);
                } else {
                    // TODO: Perform Web download (modify, add callbacks)
                    uwtTrack.setLoaded(false);
                    fetchFile(uwtTrack.getTrackUrl(), uwtTrack.getTrackName(), i);
                }
            }
        }
    }

    private String fetchTagInfo(String filePath) {
        // Return Title string from MP3 tags or null if there's no tags
        MediaMetadataRetriever mmrMp3TagInfo = new MediaMetadataRetriever();
        StringBuilder strTrackTitle = new StringBuilder("");
        mmrMp3TagInfo.setDataSource(filePath);
        String strTagArtist =
                mmrMp3TagInfo.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
        String strTagTitle =
                mmrMp3TagInfo.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
        String strTagAlbum =
                mmrMp3TagInfo.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM);
        if (strTagTitle != null) {
            strTrackTitle.append(strTagTitle);
            if (strTagArtist != null) strTrackTitle.insert(0, strTagArtist + " - ");
            if (strTagAlbum != null)
                strTrackTitle.append(" (").append(strTagAlbum).append(")");
        }
        if (strTrackTitle.capacity() != 0) return strTrackTitle.toString();
        else return null;
    }

    public void umpPlay(String fileName) { // TODO: Change scope if not used outside
        // Idle -> Initialized -> Prepared States
        try {
            umpMediaPlayer.setDataSource(SOURCE_DIR + "/" + fileName);
            umpMediaPlayer.setLooping(true);
            umpMediaPlayer.prepareAsync();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void umpStop() { // TODO: Change scope if not used outside
        // Playing -> Idle States
        if (umpMediaPlayer.isPlaying()) umpMediaPlayer.stop();
        umpMediaPlayer.reset();
    }


    BroadcastReceiver onComplete = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            // TODO: Handle on file retrieved (modify)
            tvStatusMessage.setText("Download is complete!");
        }
    };

    BroadcastReceiver onNotificationClick = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            // TODO: Display progress on notification clicked (modify)
            tvStatusMessage.setText("Download is in progress...");
        }
    };
}
