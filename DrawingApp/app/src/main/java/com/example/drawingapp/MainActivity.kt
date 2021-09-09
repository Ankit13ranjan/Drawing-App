import com.example.drawingapp.R

Activity package com.example.drawingapp

import android.app.Activity
import android.app.Dialog
import android.app.Instrumentation
import android.app.ProgressDialog
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.icu.text.SimpleDateFormat
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.AsyncTask
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.get
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.dialog_brush_size.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.util.*


class MainActivity : AppCompatActivity() {

    private var mimageButtomCurrentPath: ImageButton?= null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        drawing_view.setSizeForBrush(20.toFloat())

        mimageButtomCurrentPath = ll_paint_colors[1] as ImageButton
        mimageButtomCurrentPath!!.setImageDrawable(
            ContextCompat.getDrawable(this,R.drawable.pallet_pressed)
        )

        ib_brush.setOnClickListener {
            showBrushSizeChooserDialog()
        }

        ib_gallery.setOnClickListener{
            if(isReadStorageAllowed()){
                //Code to Picking Background Image From Gallery


                val pickPhotoIntent = Intent(Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI)



                startActivityForResult(pickPhotoIntent, GALLERY)

                // Try to implement newer one...but can't done...


//                registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
//                    onActivityResult(result)
//                }.launch(pickPhotoIntent)



            }
            else{
                requestStoragePermission()
            }
        }



        ib_undo.setOnClickListener{
            drawing_view.onClickUndo()
        }



        ib_save.setOnClickListener {

            //First checking if the app is already having the permission
            if (isReadStorageAllowed()) {

                BitmapAsyncTask(getBitmapFromView(fl_drawing_view_container)).execute()
            } else {

                //If the app don't have storage access permission we will ask for it.
                requestStoragePermission()
            }
        }

        ib_clear.setOnClickListener{
            iv_background.setImageResource(R.drawable.background_drawing_view_layout)
            drawing_view.onClickClear()
        }


    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(resultCode == Activity.RESULT_OK)
        {
            if(requestCode == GALLERY) {
                try {
                    if (data!!.data != null) {
                        iv_background.visibility = View.VISIBLE
                        iv_background.setImageURI(data!!.data)
                    } else {
                        Toast.makeText(
                            this, "Error Occuring in Parsing the image or corrupted file",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }




    private fun showBrushSizeChooserDialog(){
        val brushDialog = Dialog(this)

        brushDialog.setContentView(R.layout.dialog_brush_size)
        brushDialog.setTitle("Brush Size: ")
        val smallbtn = brushDialog.ib_small_brush
        smallbtn.setOnClickListener {
            drawing_view.setSizeForBrush(10.toFloat())
            brushDialog.dismiss()
        }

        val mediumbtn = brushDialog.ib_medium_brush
        mediumbtn.setOnClickListener {
            drawing_view.setSizeForBrush(20.toFloat())
            brushDialog.dismiss()
        }

        val largebtn = brushDialog.ib_large_brush
        largebtn.setOnClickListener {
            drawing_view.setSizeForBrush(30.toFloat())
            brushDialog.dismiss()
        }

        brushDialog.show()
    }


    fun paintClicked(view : View)
    {
        if(view!=mimageButtomCurrentPath)
        {
            val imagebutton = view as ImageButton
            val colorTag = imagebutton.tag.toString()
            drawing_view.setColor(colorTag)
            imagebutton.setImageDrawable(
                ContextCompat.getDrawable(this,R.drawable.pallet_pressed)
            )
            mimageButtomCurrentPath!!.setImageDrawable(
                ContextCompat.getDrawable(this,R.drawable.pallet_normal)
            )

            mimageButtomCurrentPath = view
        }
    }



    companion object{
        private const val STORAGE_PERMISSION_CODE = 1
        private const val GALLERY = 2
    }


    private fun requestStoragePermission(){
        if(ActivityCompat.shouldShowRequestPermissionRationale(this,
                arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE).toString())) {
            Toast.makeText(
                this, "Should allow to access background images",
                Toast.LENGTH_LONG
            ).show()
        }
        ActivityCompat.requestPermissions(this,
            arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE), STORAGE_PERMISSION_CODE)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == STORAGE_PERMISSION_CODE)
        {
            if(grantResults.isNotEmpty() &&
                grantResults[0]==PackageManager.PERMISSION_GRANTED){
                Toast.makeText(this,"Permission Granted Successfully",
                    Toast.LENGTH_LONG).show()
            }
            else{
                Toast.makeText(this,"Permission Denied!!!",
                Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun isReadStorageAllowed():Boolean{

        val res = ContextCompat.checkSelfPermission(
            this,android.Manifest.permission.READ_EXTERNAL_STORAGE)
        return  res == PackageManager.PERMISSION_GRANTED
    }

    private fun getBitmapFromView(view: View): Bitmap {
        val returnedBitmap = Bitmap.createBitmap(view.width,
        view.height,Bitmap.Config.ARGB_8888)
        val canvas = Canvas(returnedBitmap)
        val bgDrawable = view.background
        if(bgDrawable!=null){
            bgDrawable.draw(canvas)
        }
        else{
            canvas.drawColor(Color.WHITE)
        }

        view.draw(canvas)

        return returnedBitmap
    }


    private inner class BitmapAsyncTask(val mBitmap: Bitmap?) :
        AsyncTask<Any, Void, String>() {

        private var mDialog: ProgressDialog? = null
        override fun onPreExecute() {
            super.onPreExecute()
            showProgressDialog()
        }

        @RequiresApi(Build.VERSION_CODES.N)
        override fun doInBackground(vararg params: Any): String {

            return saveToGallery()
        }
        override fun onPostExecute(result: String) {
            super.onPostExecute(result)

            cancelProgressDialog()

            if(result!!.isNotEmpty()){
                Toast.makeText(this@MainActivity,
                    "File saved successfully. : $result",
                    Toast.LENGTH_LONG).show()

            }else{
                Toast.makeText(this@MainActivity,
                    "Error saving file.",
                    Toast.LENGTH_SHORT).show()
            }

            shareImage(Uri.parse(result))
        }

        private fun shareImage(uri: Uri) {
            val intent = Intent(Intent.ACTION_SEND).apply{
                type="image/jpeg"
                putExtra(Intent.EXTRA_STREAM, uri)
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            }

            startActivity(
                Intent.createChooser(
                    intent, "Share"
                )
            )
        }


        @RequiresApi(Build.VERSION_CODES.N)
        private fun saveToGallery(): String {
            var result = ""

            var resolver = this@MainActivity.contentResolver

            val foldername = packageManager.getApplicationLabel(applicationInfo).toString()
                                .replace(" ", "")
            val filename = createFilename(foldername)
            val saveLocation = Environment.DIRECTORY_PICTURES + File.separator + foldername

            if (android.os.Build.VERSION.SDK_INT >= 29) {
                val values = ContentValues()
                values.put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                values.put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000)

                // RELATIVE_PATH and IS_PENDING are introduced in API 29.
                values.put(MediaStore.Images.Media.RELATIVE_PATH, saveLocation)
                values.put(MediaStore.Images.Media.IS_PENDING, true)


                val uri: Uri? = resolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

                if (uri != null) {
                    //val outstream = resolver.openOutputStream(uri)

                    if (mBitmap != null) {
                        saveImageToStream(mBitmap, resolver.openOutputStream(uri))
                    }

                    values.put(MediaStore.Images.Media.IS_PENDING, false)

                    this@MainActivity.contentResolver.update(uri, values, null, null)

                    result = uri.toString()
                }
            }

            return result
        }

        private fun saveImageToStream(bitmap: Bitmap, outputStream: OutputStream?) {
            if (outputStream != null) {
                try {
                    mBitmap!!.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
                    outputStream.close()
                } catch (e: Exception) {
                    Log.e("**Exception", "Could not write to stream" )
                    e.printStackTrace()
                }
            }



        }

        @RequiresApi(Build.VERSION_CODES.N)
        private fun createFilename(filename:String) : String{
            val formatter = SimpleDateFormat("YYYYMMdd-HHmm.ssSSS")
            val dateString = formatter.format(Date()) + "_"

            return dateString + filename + ".jpg"
        }

        
        private fun showProgressDialog() {
            @Suppress("DEPRECATION")
            mDialog = ProgressDialog.show(
                this@MainActivity,
                "",
                "Saving your image..."
            )
        }


        private fun cancelProgressDialog() {
            if (mDialog != null) {
                mDialog!!.dismiss()
                mDialog = null
            }
        }
    }


}