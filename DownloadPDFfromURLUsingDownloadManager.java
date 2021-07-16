/*-------------------------------------------------------------------------------------------------------------------------*/             
/*1. download pdf from internet using download manager*/
 public void downloadPdf() {

        final File tempFile = new File(getApplicationContext().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "file_name.pdf");


        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(getResources().getString(R.string.pdf_downloading));
        progressDialog.setCancelable(false);
        progressDialog.show();
        String imageUrl = "";
        if (details.getFormats().size() > 0) {
            imageUrl = PDF_URL;
        }
        DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(imageUrl));

        request.allowScanningByMediaScanner();
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED); //Notify client once download is completed!
        request.setDestinationInExternalFilesDir(getApplicationContext(), Environment.DIRECTORY_DOWNLOADS, details.getPdf());
        request.addRequestHeader("Accept", Utils.PDF_MIME_TYPE);
        //Enqueue a new download and same the referenceId
        downloadReference = dm.enqueue(request);

        IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                long reference = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                progressDialog.dismiss();
                if (downloadReference == reference) {
                    // Do something with downloaded file.
                    DialogBox.dialogBox(DocumentDetailsActivity.this, false, getResources().getString(R.string.successful), getResources().getColor(R.color.color_1d1d1f), getResources().getString(R.string.file_downloaded_successfully), getResources().getString(R.string.ok), R.raw.download_icon);
                }
                btn_download.setEnabled(true);
            }
        };
        registerReceiver(receiver, filter);
    }
    /*-------------------------------------------------------------------------------------------------------------------------*/             
    
    
    
    /*-------------------------------------------------------------------------------------------------------------------------*/             
    /*2. download pdf from internet using download manager and open it in pdf viewer*/

                if (Utils.isPDFSupported(this)) {
                    downloadAndOpenPDF(this,PDF_URL);
                } else {
                    askToOpenPDFThroughGoogleDrive(this, PDF_URL);
                }
                
                
    public static boolean isPDFSupported(Context context) {
        Intent i = new Intent(Intent.ACTION_VIEW);
        final File tempFile = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "test.pdf");
        i.setDataAndType(Uri.fromFile(tempFile), PDF_MIME_TYPE);
        return context.getPackageManager().queryIntentActivities(i, PackageManager.MATCH_DEFAULT_ONLY).size() > 0;
    }
                
               
   public void downloadAndOpenPDF(final Context context, final String pdfUrl) {
        // Get filename
        String filename ="file_name.pdf";
        // The place where the downloaded PDF file will be put
        final File tempFile = new File(getApplicationContext().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), filename);

        if (tempFile.exists()) {
            // If we have downloaded the file before, just go ahead and show it.
            Uri apkURI = FileProvider.getUriForFile(getApplicationContext(), context.getApplicationContext().getPackageName() + ".fileprovider", tempFile);
            Utils.openPDF(context, apkURI);
            return;
        }

        // Show progress dialog while downloading
        ProgressDialog progress = new ProgressDialog(this);
        progress.setMessage(getResources().getString(R.string.pdf_downloading));
        progress.setCancelable(false);
        progress.show();

        // Create the download request
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(pdfUrl));
        request.allowScanningByMediaScanner();
        request.setDestinationInExternalFilesDir(getApplicationContext(), Environment.DIRECTORY_DOWNLOADS, filename);
        request.addRequestHeader("Accept", Utils.PDF_MIME_TYPE);
        final DownloadManager dm = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        BroadcastReceiver onComplete = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (!progress.isShowing()) {
                    return;
                }
                context.unregisterReceiver(this);

                progress.dismiss();
                long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                Cursor c = dm.query(new DownloadManager.Query().setFilterById(downloadId));

                if (c.moveToFirst()) {
                    int status = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS));
                    if (status == DownloadManager.STATUS_SUCCESSFUL) {
                        Uri apkURI = FileProvider.getUriForFile(context, context.getApplicationContext().getPackageName() + ".fileprovider", tempFile);
                        Utils.openPDF(context, apkURI);
                    }
                }
                c.close();
            }
        };
        context.registerReceiver(onComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));

        // Enqueue the request
        dm.enqueue(request);
    }


    public void askToOpenPDFThroughGoogleDrive(final Context context, final String pdfUrl) {
        new AlertDialog.Builder(context)
                .setTitle(getResources().getString(R.string.alert))
                .setMessage(getResources().getString(R.string.do_you_want_to_open_in_google_drive))
                .setNegativeButton(getResources().getString(R.string.no), null)
                .setPositiveButton(getResources().getString(R.string.yes), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        openPDFThroughGoogleDrive(context, pdfUrl);
                    }
                })
                .show();
    }

    public void openPDFThroughGoogleDrive(final Context context, final String pdfUrl) {
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setDataAndType(Uri.parse(Utils.GOOGLE_DRIVE_PDF_READER_PREFIX + pdfUrl), Utils.HTML_MIME_TYPE);
        context.startActivity(i);
    }
   /*-------------------------------------------------------------------------------------------------------------------------*/     
