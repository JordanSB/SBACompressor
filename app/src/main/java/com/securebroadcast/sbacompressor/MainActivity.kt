package com.securebroadcast.sbacompressor

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.support.annotation.RequiresApi
import android.support.v4.content.FileProvider
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import com.livinglifetechway.quickpermissions_kotlin.runWithPermissions

import com.securebroadcast.compressor.SiliCompressor
import kotlinx.android.synthetic.main.activity_main.*

import java.io.File
import java.io.IOException
import java.net.URISyntaxException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    internal lateinit var mCurrentPhotoPath: String
    internal var capturedUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        videoImageView!!.setOnClickListener { methodRequiresPermissions() }
    }

    fun methodRequiresPermissions()  = runWithPermissions(Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE) {
        dispatchTakeVideoIntent()
    }

    @RequiresApi(Build.VERSION_CODES.FROYO)
    @Throws(IOException::class)
    private fun createMediaFile(type: Int): File {
        // Create an image file name
        val timeStamp = SimpleDateFormat("ddMMyyyy_HHmmss", Locale.UK).format(Date())
        val fileName = "VID_$timeStamp"
        val storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
        val file = File.createTempFile(fileName, ".mp4", storageDir)

        // Get the path of the file created
        mCurrentPhotoPath = file.absolutePath
        return file
    }

    private fun dispatchTakeVideoIntent() {
        val takeVideoIntent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
        takeVideoIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        if (takeVideoIntent.resolveActivity(packageManager) != null) {
            try {
                takeVideoIntent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, 10)
                takeVideoIntent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1)
                capturedUri = FileProvider.getUriForFile(this, FILE_PROVIDER_AUTHORITY, createMediaFile(TYPE_VIDEO))

                takeVideoIntent.putExtra(MediaStore.EXTRA_OUTPUT, capturedUri)
                startActivityForResult(takeVideoIntent, REQUEST_TAKE_VIDEO)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    // Method which will process the captured image
    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_TAKE_VIDEO && resultCode == Activity.RESULT_OK) {
            if (data!!.data != null) {
                //create destination directory
                val f = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).toString() + "/Silicompressor/videos")
                if (f.mkdirs() || f.isDirectory)
                //compress and output new video specs
                    VideoCompressAsyncTask(this).execute(mCurrentPhotoPath, f.path)

            }
        }
    }

    internal inner class VideoCompressAsyncTask(var mContext: Context) : AsyncTask<String, String, String>() {

        override fun onPreExecute() {
            super.onPreExecute()
            compressionMsg.visibility = View.VISIBLE
            pic_description.visibility = View.GONE
        }

        override fun doInBackground(vararg paths: String): String? {
            var filePath: String? = null
            try {
                filePath = SiliCompressor.with(mContext).compressVideo(paths[0], paths[1], 300, 300, 1000000)
            } catch (e: URISyntaxException) {
                e.printStackTrace()
            }
            return filePath
        }

        override fun onPostExecute(compressedFilePath: String) {
            super.onPostExecute(compressedFilePath)
            val imageFile = File(compressedFilePath)
            val length = imageFile.length() / 1024f // Size in KB
            val value: String
            if (length >= 1024)
                value = (length / 1024f).toString() + " MB"
            else
                value = "$length KB"
            val text = String.format(Locale.US, "%s\nName: %s\nSize: %s", getString(R.string.video_compression_complete), imageFile.name, value)
            compressionMsg.visibility = View.GONE
            pic_description.visibility = View.VISIBLE
            pic_description.text = text
            Log.i("Silicompressor", "Path: $compressedFilePath")
        }
    }

    companion object {

        val LOG_TAG = MainActivity::class.java.simpleName
        val FILE_PROVIDER_AUTHORITY = "com.securebroadcast.compressor.provider"
        private val REQUEST_TAKE_VIDEO = 200
        private val TYPE_VIDEO = 1
    }
}