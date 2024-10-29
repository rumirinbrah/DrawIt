package com.example.drawingapp

import android.Manifest
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.media.MediaScannerConnection
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.get
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {

    private var drawingView: DrawingView? = null
    private var currentPaintButton: ImageButton? = null
    private var saveButton: ImageButton? = null
    private var overlayImage: ImageView? = null
    private var progressBar: Dialog? = null


    private val galleryImageLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK && result.data != null) {

                overlayImage?.setImageURI(result.data?.data)

            }
        }

    //private var undo : Button? =null
    private val requestPermission: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            permissions.entries.forEach {
                val permissionName = it.key
                val isGranted = it.value
                if (isGranted) {
                    Toast.makeText(this , "$permissionName granted" , Toast.LENGTH_SHORT).show()
                    val pickIntent = Intent(
                        Intent.ACTION_PICK ,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    )
                    galleryImageLauncher.launch(pickIntent)

                } else {
                    Toast.makeText(this , "$permissionName denied" , Toast.LENGTH_SHORT).show()
                }
            }
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        overlayImage = findViewById(R.id.overlay_image)
        saveButton = findViewById(R.id.save_button)


        val brushIcon = findViewById<ImageButton>(R.id.brush_icon)
        val insertImage = findViewById<ImageButton>(R.id.insert_image_button)
        val undo = findViewById<ImageButton>(R.id.undo_button)
        val colorSelectorLayout = findViewById<LinearLayout>(R.id.color_selector_ll)
        currentPaintButton = colorSelectorLayout[1] as ImageButton

        insertImage.setOnClickListener {
            requestStoragePermission()
        }


        drawingView = findViewById(R.id.drawingView)
        drawingView!!.setStroke(12f)
        brushIcon.setOnClickListener {
            showBrushSelector()
        }
        undo.setOnClickListener {
            drawingView?.undo()
        }
        saveButton?.setOnClickListener {
            //TODO
            showProgressBar()
            lifecycleScope.launch {
                val frameLayout = findViewById<FrameLayout>(R.id.frame_layout)
                val bitmap = getBitmapFromView(frameLayout)
                val result =saveBitmap(bitmap)
                shareDrawing(result)

            }


        }


    }


    private fun showBrushSelector() {
        val brushDialog = Dialog(this)
        brushDialog.setContentView(R.layout.size_selector)
        brushDialog.setTitle("Brush Size")

        val smallButton = brushDialog.findViewById<ImageButton>(R.id.ib_small_brush)
        val mediumButton = brushDialog.findViewById<ImageButton>(R.id.ib_medium_brush)
        val largeButton = brushDialog.findViewById<ImageButton>(R.id.ib_large_brush)
        smallButton.setOnClickListener {
            drawingView?.setStroke(7f)
            brushDialog.dismiss()
        }
        mediumButton.setOnClickListener {
            drawingView?.setStroke(11f)
            brushDialog.dismiss()
        }
        largeButton.setOnClickListener {
            drawingView?.setStroke(17f)
            brushDialog.dismiss()
        }
        brushDialog.show()
    }

    fun colorChanged(view: View) {
        if (currentPaintButton != view) {
            currentPaintButton = view as ImageButton
            val imageButton = view as ImageButton
            val color = imageButton.tag.toString()
            drawingView?.setColor(color)
        }
        Toast.makeText(this , "Color Changed" , Toast.LENGTH_SHORT).show()
    }

    //permission denied
    fun showDeniedDialog(title: String , message: String) {

        val builder = AlertDialog.Builder(this)
        builder.setTitle(title)
            .setMessage(message)
            .setPositiveButton("Cancel") { dialog , _ ->
                dialog.dismiss()
            }
        builder.create().show()

    }

    private fun isReadStorageAllowed(): Boolean {
        val result = ContextCompat.checkSelfPermission(
            this ,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
        return result
    }

    private fun requestStoragePermission() {
        if (
            ActivityCompat.shouldShowRequestPermissionRationale(
                this ,
                Manifest.permission.READ_MEDIA_IMAGES
            )
        ) {
            Toast.makeText(this , "IF BLOCK" , Toast.LENGTH_SHORT).show()
            showDeniedDialog("Drawing App" , "Please grant permission")
        } else {
            Toast.makeText(this , "else block" , Toast.LENGTH_SHORT).show()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requestPermission.launch(
                    arrayOf(
                        Manifest.permission.WRITE_EXTERNAL_STORAGE ,
                        Manifest.permission.READ_MEDIA_IMAGES

                    )
                )
            } else {
                requestPermission.launch(
                    arrayOf(
                        Manifest.permission.WRITE_EXTERNAL_STORAGE ,
                        Manifest.permission.READ_MEDIA_IMAGES
                    )
                )
            }
        }
    }

    private fun getBitmapFromView(view: View): Bitmap {
        //Toast.makeText(this , "here" , Toast.LENGTH_SHORT).show()

        val bitmap = Bitmap.createBitmap(
            view.width ,
            view.height ,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        val backgroundImage = view.background
        if (backgroundImage != null) {
            backgroundImage.draw(canvas)
        } else {
            canvas.drawColor(Color.WHITE)
        }

        view.draw(canvas)

        return bitmap

    }

    private suspend fun saveBitmap(bitmap: Bitmap?): String {

        var result = ""
        //Toast.makeText(this , "saving bitmap" , Toast.LENGTH_SHORT).show()
        withContext(Dispatchers.IO) {
            if (bitmap != null) {
                try {
                    val bytes = ByteArrayOutputStream()
                    bitmap.compress(
                        Bitmap.CompressFormat.PNG ,
                        90 ,
                        bytes
                    )

                    //location of file
                    val file = File(
                        externalCacheDir?.absoluteFile.toString()
                                + File.separator + "DrawingApp" + System.currentTimeMillis() / 1000
                                + ".png"
                    )

                    val fileOutput = FileOutputStream(file)
                    fileOutput.write(bytes.toByteArray())
                    fileOutput.close()

                    result = file.absolutePath
                    runOnUiThread {
                        if (result.isNotEmpty()) {
                            Toast.makeText(
                                this@MainActivity ,
                                "Saved successfully" ,
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            Toast.makeText(
                                this@MainActivity ,
                                "An unknown error occurred" ,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

                } catch (e: Exception) {
                    result = ""
                    e.printStackTrace()

                }
            }
        }
        hideProgressBar()
        return result
    }

    private fun showProgressBar() {

        progressBar = Dialog(this@MainActivity)
        progressBar?.setContentView(R.layout.dialog_progress_bar)
        progressBar?.show()

    }

    private fun hideProgressBar() {
        progressBar?.dismiss()
        progressBar = null
    }
    private fun shareDrawing(path:String){
        MediaScannerConnection.scanFile(this, arrayOf( path),null){
            path , uri->
            val shareIntent = Intent()
            //send items
            shareIntent.action = Intent.ACTION_SEND
            shareIntent.putExtra(Intent.EXTRA_STREAM,uri)
            shareIntent.type ="image/png"
            startActivity(Intent.createChooser(shareIntent,"Share"))



        }
    }


}