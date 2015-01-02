package uk.co.unclealex.sync.devicesynchroniser.main;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import uk.co.unclealex.sync.devicesynchroniser.changes.Changelog;
import uk.co.unclealex.sync.devicesynchroniser.changes.ChangelogItem;
import uk.co.unclealex.sync.devicesynchroniser.sync.R;
import uk.co.unclealex.sync.devicesynchroniser.tags.Tags;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.SimpleDateFormat;

/**
 * Created by alex on 24/12/14.
 */
public class ChangelogItemAdapter extends RecyclerView.Adapter<ChangelogItemAdapter.AlbumViewHolder> {

    private Changelog changelog;
    private final MainPresenter mainPresenter;

    public ChangelogItemAdapter(Changelog changelog, MainPresenter mainPresenter) {
        this.changelog = changelog;
        this.mainPresenter = mainPresenter;
    }

    @Override
    public int getItemCount() {
        return changelog.getChangelogItems().size();
    }

    @Override
    public void onBindViewHolder(final AlbumViewHolder albumViewHolder, int i) {
        final ChangelogItem ci = changelog.getChangelogItems().get(i);
        AsyncTask<Void, Void, Tags> task = new AsyncTask<Void, Void, Tags>() {
            @Override
            protected Tags doInBackground(Void... params) {
                try {
                    return mainPresenter.loadTags(ci.getRelativePath());
                } catch (IOException e) {
                    throw new RuntimeException("Could not load tags", e);
                }
            }

            @Override
            protected void onPostExecute(final Tags tags) {
                albumViewHolder.vWhen.setText(new SimpleDateFormat("d MMM yyyy, HH:mm").format(ci.getAt()));
                albumViewHolder.vArtist.setText(tags.getAlbumArtist());
                albumViewHolder.vAlbum.setText(tags.getAlbum());
                AsyncTask<Void, Void, Bitmap> task = new AsyncTask<Void, Void, Bitmap>() {
                    @Override
                    protected Bitmap doInBackground(Void... params) {
                        InputStream in = null;
                        try {
                            in = new URL(tags.getCoverArt().toString()).openStream();
                            Bitmap unscaledBitmap = BitmapFactory.decodeStream(in);
                            float factor = Math.max(
                                    albumViewHolder.vCoverArt.getWidth() / (float) unscaledBitmap.getWidth(),
                                    albumViewHolder.vCoverArt.getHeight() / (float) unscaledBitmap.getHeight());
                            return Bitmap.createScaledBitmap(unscaledBitmap, (int) (unscaledBitmap.getWidth() * factor), (int) (unscaledBitmap.getHeight() * factor), true);

                        } catch (IOException e) {
                            throw new RuntimeException("Cannot load picture.", e);
                        } finally {
                            if (in != null) {
                                try {
                                    in.close();
                                } catch (IOException e) {
                                    // Ignore.
                                }
                            }
                        }
                    }

                    @Override
                    protected void onPostExecute(Bitmap bitmap) {
                        albumViewHolder.vCoverArt.setImageBitmap(bitmap);
                    }
                };
                task.execute();

            }
        };
        task.execute();
    }

    @Override
    public AlbumViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View itemView = LayoutInflater.
                from(viewGroup.getContext()).
                inflate(R.layout.album_layout, viewGroup, false);

        return new AlbumViewHolder(itemView);
    }

    public static class AlbumViewHolder extends RecyclerView.ViewHolder {
        protected TextView vWhen;
        protected TextView vAlbum;
        protected TextView vArtist;
        protected ImageView vCoverArt;

        public AlbumViewHolder(View v) {
            super(v);
            vWhen = (TextView) v.findViewById(R.id.time_text_view);
            vAlbum = (TextView) v.findViewById(R.id.album_text_view);
            vArtist = (TextView) v.findViewById(R.id.artist_text_view);
            vCoverArt = (ImageView) v.findViewById(R.id.album_cover_view);
        }
    }
}