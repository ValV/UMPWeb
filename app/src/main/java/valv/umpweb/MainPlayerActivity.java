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
    /* TODO: Legacy download method (remove)
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
                    // Set Progress in the Track Title (todo)
                    UmpWebTrack uwtTrack = (UmpWebTrack) llvTrackList.getChildAt(dlTrack);
                    if (uwtTrack != null) {
                        ((Button) uwtTrack.getChildAt(0)).setText(uwtTrack.getTrackName() + " ("
                                + String.valueOf(iDlProgress) + ")");
                    }
                }
            }
        }
    }*/

    private final String PLAYLIST_URL =
            "https://www.dropbox.com/s/ivsw18bo669erwc/UMPWeb.list?dl=0";
    private final String PLAYLIST_NAME = "UMPWeb.list";
    private File SOURCE_DIR;
    //private int dlTrack = -1;
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
        dmUfmDownloader = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        registerReceiver(onComplete,
                new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        registerReceiver(onNotificationClick,
                new IntentFilter(DownloadManager.ACTION_NOTIFICATION_CLICKED));
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

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        // Release MediaPlayer resources on Activity destruction
        if (umpMediaPlayer.isPlaying()) umpMediaPlayer.stop();

        unregisterReceiver(onComplete);
        unregisterReceiver(onNotificationClick);

        umpMediaPlayer.release();
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
            return;
        } else if (isExternalStorageAvailable() && forceReload) {
            // SD available read-write - download Playlist
            if (!SOURCE_DIR.exists()) {
                if (SOURCE_DIR.mkdirs())
                    tvStatusMessage.setText("Created: " + SOURCE_DIR.getAbsolutePath());
                else tvStatusMessage.setText("Directory is Ok");
            }
            // Download PLAYLIST_URL as SOURCE_DIR.getAbsolutePath() + "/" + PLAYLIST_NAME
            fetchTrack(PLAYLIST_URL, /*SOURCE_DIR.getAbsolutePath() + "/" +*/PLAYLIST_NAME);
            // TODO: File download queue (add)
            // rebuildTrackList(SOURCE_DIR.getAbsolutePath() + "/UMPWeb.list");
        } else {
            // SD available read-only - read existing Playlist
            if (forceReload) tvStatusMessage.setText("SD is read-only. Reading Playlist...");
                // Read Playlist in spite of how SD is mounted
            else tvStatusMessage.setText("Reading Playlist...");
            if (SOURCE_DIR.exists()) {
                File fTrackList = new File(SOURCE_DIR.getAbsolutePath() + "/" + PLAYLIST_NAME);
                if (fTrackList.exists()) {
                    tvStatusMessage.setText("Playlist does exist. Creating Track List...");
                    rebuildTrackList(fTrackList.getAbsolutePath());
                } else tvStatusMessage.setText("Playlist does not exist!");
            } else {
                tvStatusMessage.setText("Directory doesn't exist and SD is read-only");
                rebuildTrackList(null);
            }
        }
    }

    protected void rebuildTrackList(String trackListPath) {
        // Build TrackList based on Playlist
        LineNumberReader lnrTracks = null;
        try {
            if (trackListPath != null) {
                lnrTracks = new LineNumberReader(new FileReader(trackListPath));
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
            } catch (IOException e) {
                tvStatusMessage.setText(e.getMessage());
            }
        }
    }

    public void fetchTrack(String urlAddress, String fileName) {
        fetchTrack(urlAddress, fileName, -1);
    }

    public void fetchTrack(String urlAddress, String fileName, int trackIndex) {
        /* TODO: Legacy download method (remove)
        Intent intent = new Intent(this, DownloadService.class);
        intent.putExtra(DownloadService.URL_ADDRESS, urlAddress);
        intent.putExtra(DownloadService.FILE_NAME, filePath);
        intent.putExtra(DownloadService.RESULT_RECEIVER, new DownloadReceiver(new Handler()));
        startService(intent); */
        // Enqueue with DownloadManager
        Uri uriRemoteFile = Uri.parse(urlAddress);
        lLastDl = dmUfmDownloader.enqueue(new DownloadManager.Request(uriRemoteFile)
                .setAllowedNetworkTypes(DownloadManager.Request.NETWORK_MOBILE
                        | DownloadManager.Request.NETWORK_WIFI)
                .setAllowedOverRoaming(false)
                .setTitle(String.valueOf(trackIndex))
                .setDescription("Track number in TrackList")
                .setDestinationInExternalPublicDir(SOURCE_DIR.getAbsolutePath(), fileName));
    }

    private void fetchTagsFiles() {
        if (llvTrackList != null) {
            int iTrackNum = llvTrackList.getChildCount();
            for (int i = 0; i < iTrackNum; i ++) {
                UmpWebTrack uwtTrack = (UmpWebTrack) llvTrackList.getChildAt(i);
                File fTrack = new File(SOURCE_DIR.getAbsolutePath() + "/" + uwtTrack.getTrackName());
                if (fTrack.exists()) {
                    // File already downloaded - get mp3 tags
                    MediaMetadataRetriever mmrMp3TagInfo = new MediaMetadataRetriever();
                    StringBuilder strTrackTitle = new StringBuilder("");
                    mmrMp3TagInfo.setDataSource(fTrack.getAbsolutePath());
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
                    if (strTrackTitle.capacity() != 0) uwtTrack.setTitle(strTrackTitle.toString());
                    uwtTrack.setLoaded(true);
                } else {
                    // TODO: Perform Web download (modify, add callbacks)
                    uwtTrack.setLoaded(false);
                    fetchTrack(uwtTrack.getTrackUrl(), uwtTrack.getTrackName(), i);
                }
            }
        }
    }

    public void umpPlay(String fileName) {
        // Idle -> Initialized -> Prepared States
        try {
            umpMediaPlayer.setDataSource(SOURCE_DIR + "/" + fileName);
            umpMediaPlayer.setLooping(true);
            umpMediaPlayer.prepareAsync();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void umpStop() {
        // Playing -> Idle States
        if (umpMediaPlayer.isPlaying()) umpMediaPlayer.stop();
        umpMediaPlayer.reset();
    }


    BroadcastReceiver onComplete = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            // TODO: Notify that file retrieved (modify)
            tvStatusMessage.setText("Download is complete!");
        }
    };

    BroadcastReceiver onNotificationClick = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            // TODO: Display progress (modify)
            tvStatusMessage.setText("Dl is in progress...");
        }
    };
}
