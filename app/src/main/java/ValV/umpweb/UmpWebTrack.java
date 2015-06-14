package ValV.umpweb;

import android.content.Context;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Created by ValV on 6/13/15.
 */
public class UmpWebTrack extends LinearLayout {
    private String trackUrl = "";
    private String trackName = "";
    private boolean mp3TagSet = false;
    private boolean mp3Loaded = false;
    private boolean nowPlaying = false;

    public UmpWebTrack(Context context) {
        super(context);
        setOrientation(LinearLayout.HORIZONTAL);
        Button btnTrackControl = new Button(context);
        TextView tvTrackInfo = new TextView(context);
        addView(btnTrackControl);
        addView(tvTrackInfo);
        btnTrackControl.setText(" * ");
        btnTrackControl.setTextSize(TypedValue.COMPLEX_UNIT_SP, 32);
        btnTrackControl.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                // Get TrackList and reset all Buttons' States
                LinearLayout llvParent = (LinearLayout) view.getParent().getParent();
                UmpWebTrack uwtTrack = null;
                int listTrackCount = llvParent.getChildCount();
                for (int i = 0; i < listTrackCount; i ++) {
                    uwtTrack = (UmpWebTrack) llvParent.getChildAt(i);
                    // For each other Button reset State
                    if (uwtTrack.getLoaded()) uwtTrack.setLoaded(true);
                    else uwtTrack.setLoaded(false);
                    if ((uwtTrack.getChildAt(0) != view)) {
                        if (uwtTrack.getPlaying()) {
                            // Stop playback
                            uwtTrack.setPlaying(false);
                            ((MainPlayerActivity) getContext()).umpStop();
                        }
                    } else {
                        // For the same button
                        if (uwtTrack.getPlaying()) {
                            // TODO Stub: Stop playing
                            uwtTrack.setPlaying(false);
                            ((MainPlayerActivity) getContext()).umpStop();
                        } else {
                            // TODO Stub: Start playing playFile(uwtTrack.getTrackName())
                            ((Button) view).setText(" ▪ ");
                            uwtTrack.setPlaying(true);
                            ((MainPlayerActivity) getContext()).umpPlay(uwtTrack.getTrackName());
                        }
                    }
                }
            }
        });
        tvTrackInfo.setText("New Track");
    }

    public void setTrackName(String name) {
        this.trackName = name;
    }

    public String getTrackName() {
        return this.trackName;
    }

    public void setTrackUrl(String url) {
        this.trackUrl = url;
    }

    public String getTrackUrl() {
        return this.trackUrl;
    }

    public void setTitle(String title) {
        if (title == null) ((TextView) this.getChildAt(1)).setText(this.trackName);
        else ((TextView) this.getChildAt(1)).setText(title);
    }

    public void setTitle() {
        this.setTitle(null);
    }

    public void setChecked(boolean isMp3TagSet) {
        this.mp3TagSet = isMp3TagSet;
    }

    public boolean getChecked() {
        return this.mp3TagSet;
    }

    public void setLoaded(boolean isMp3Loaded) {
        this.mp3Loaded = isMp3Loaded;
        Button btnTrackControl = (Button) this.getChildAt(0);
        if (isMp3Loaded) btnTrackControl.setText(" ▶ ");
        else btnTrackControl.setText(" * ");
    }

    public boolean getLoaded() {
        return this.mp3Loaded;
    }

    public void setPlaying(boolean isNowPlaying) {
        this.nowPlaying = isNowPlaying;
    }

    public boolean getPlaying() {
        return this.nowPlaying;
    }
}
