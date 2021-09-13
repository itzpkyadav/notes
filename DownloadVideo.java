
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.yunolearning.learn.R;
import com.yunolearning.learn.constants.Utils;
import com.yunolearning.learn.database.Download;
import com.yunolearning.learn.model.VideoList;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by Itzpkyadav (itzpkyadav@gmail.com) on 03-08-2021
 * Copyright (c) 2021 itzpkyadav@gmail.com
 */
public class DownloadVideo {
    private static final String TAG = "Download Video";
    private Context context;

    private String downloadUrl = "", downloadFileName = "";
    private ProgressDialog progressDialog;
    private VideoList details;

    public DownloadVideo(Context context, VideoList details, String downloadUrl, String FileName) {
        this.context = context;

        this.downloadUrl = downloadUrl;
        this.details = details;
        this.downloadFileName = FileName;

        Log.e(TAG, downloadFileName);

        //Start Downloading Task
        new DownloadingTask().execute();
    }

    private class DownloadingTask extends AsyncTask<Void, Void, Void> {
        File tempFile = null;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = new ProgressDialog(context);
            progressDialog.setMessage("Downloading");
            progressDialog.setCancelable(false);
            progressDialog.setIndeterminate(false);
            progressDialog.setMax(100);
            progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            progressDialog.show();
        }

        @Override
        protected void onPostExecute(Void result) {
            progressDialog.dismiss();
            try {
                if (tempFile != null) {

                    Toast.makeText(context, context.getResources().getString(R.string.file_downloaded_successfully), Toast.LENGTH_SHORT).show();

                    Download download = new Download();
                    download.setId(details.getId());
                    download.setTitle(details.getTitle());
                    download.setImage_url(details.getThumbnail());
                    download.setPublished(details.getPublishedAt());
                    download.setContent_type(Utils.CONTENT_VIDEO_TYPE);
                    download.setView_type(Utils.VIDEO_TYPE);
                    download.setFile_name(downloadFileName);
                    download.setShare_url(details.getUrl());
                    download.setDownload_url(details.getAndroidFile().getDownloading().get(0).getLink());
                    Utils.saveToLocalStorage(context, download);
                } else {
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                        }
                    }, 3000);

                    Log.e(TAG, "Download Failed");

                }
            } catch (Exception e) {
                e.printStackTrace();

                //Change button text if exception occurs
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                    }
                }, 3000);
                Log.e(TAG, "Download Failed with Exception - " + e.getLocalizedMessage());

            }
            super.onPostExecute(result);
        }

        @Override
        protected Void doInBackground(Void... arg0) {
            try {
                URL url = new URL(downloadUrl);//Create Download URl
                HttpURLConnection c = (HttpURLConnection) url.openConnection();//Open Url Connection
                c.setRequestMethod("GET");//Set Request Method to "GET" since we are grtting data
                c.connect();//connect the URL Connection

                //If Connection response is not OK then show Logs
                if (c.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    Log.e(TAG, "Server returned HTTP " + c.getResponseCode() + " " + c.getResponseMessage());
                }
                tempFile = new File(context.getApplicationContext().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), downloadFileName);
                //Create New File if not present
                if (!tempFile.exists()) {
                    tempFile.createNewFile();
                    Log.e(TAG, "File Created");
                }

                InputStream is = c.getInputStream();
                BufferedInputStream inStream = new BufferedInputStream(is, 1024 * 5);
                FileOutputStream outStream = new FileOutputStream(tempFile);
                byte[] buff = new byte[5 * 1024];

                //Read bytes (and store them) until there is nothing more to read(-1)
                int len = 0;
                // this will be useful so that you can show a tipical 0-100% progress bar
                int fileLength = c.getContentLength();
                long total = 0;
                while ((len = inStream.read(buff)) != -1) {
                    total += len;
                    // publishing the progress....
                    // After this onProgressUpdate will be called
                    progressDialog.setProgress((int) (total * 100 / fileLength));

                    // writing data to file
                    outStream.write(buff, 0, len);
                }

                //clean up
                outStream.flush();
                outStream.close();
                inStream.close();
            } catch (Exception e) {
                //Read exception if something went wrong
                e.printStackTrace();
                tempFile = null;
                Log.e(TAG, "Download Error Exception " + e.getMessage());
            }

            return null;
        }

        /**
         * Updating progress bar
         */
        protected void onProgressUpdate(String... progress) {
            // setting progress percentage
            progressDialog.setProgress(Integer.parseInt(progress[0]));
        }
    }
}
