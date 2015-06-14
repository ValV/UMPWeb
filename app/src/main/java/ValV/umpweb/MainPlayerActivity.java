package ValV.umpweb;

import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.ActionBarActivity;
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


public class MainPlayerActivity extends ActionBarActivity {

    private final String PLAYLIST_URL =
            "https://www.dropbox.com/s/ivsw18bo669erwc/UMPWeb.list?dl=0";
    private File SOURCE_DIR;
    private Button btnFetchPlaylist;
    private LinearLayout llvTrackList;
    private TextView tvStatusMessage;

    protected MediaPlayer umpMediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_player);
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
        umpMediaPlayer.release();
        super.onDestroy();
    }

    protected boolean isExternalStorageAvailable(boolean isReadOnly) {
        // Check External Storage availability (read-write or read-only)
        String exState = Environment.getExternalStorageState();
        return (Environment.MEDIA_MOUNTED.equals(exState)
                || (Environment.MEDIA_MOUNTED_READ_ONLY.equals(exState) && isReadOnly)) /*{
            return true;
        }
        return false*/;
    }

    protected boolean isExternalStorageAvailable() {
        // Override: default - available read-write
        return isExternalStorageAvailable(false);
    }

    protected void fetchPlaylist(boolean forceReload) {
        forceReload = false; // TODO Stub (remove): No connection - only do read existing file
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
            // TODO Stub: Download PLAYLIST_URL as SOURCE_DIR.getAbsolutePath() + "/UMPWeb.list"
            // TODO Stub: rebuildTrackList(SOURCE_DIR.getAbsolutePath() + "/UMPWeb.list");
        } else {
            // SD available read-only - read existing Playlist
            if (forceReload) tvStatusMessage.setText("SD is read-only. Reading Playlist...");
                // Read Playlist in spite of how SD is mounted
            else tvStatusMessage.setText("Reading Playlist...");
            if (SOURCE_DIR.exists()) {
                File fTrackList = new File(SOURCE_DIR.getAbsolutePath() + "/UMPWeb.list");
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
                // This may be removed (not critical)
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

    public boolean fetchTrack(String url, String name) {
        // TODO Stub: download url as name
        return false;
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
                    // TODO Stub: Perform Web download
                    fetchTrack(null, null);
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
}
