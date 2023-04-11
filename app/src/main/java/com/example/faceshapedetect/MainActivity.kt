package com.example.faceshapedetect
import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import android.content.Intent as Intent1

class MainActivity : AppCompatActivity() {

    lateinit var select_image_button : Button
    lateinit var make_prediction : Button
    lateinit var img_view : ImageView
    lateinit var text_view : TextView
    var bitmap:Bitmap? = null
    lateinit var camerabtn : Button
    var uri:String = ""

    @RequiresApi(Build.VERSION_CODES.M)
    public fun checkandGetpermissions(){
        if(checkSelfPermission(android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED){
            requestPermissions(arrayOf(android.Manifest.permission.CAMERA), 100)
        }
        else{
            Toast.makeText(this, "Camera permission granted", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == 100){
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
                Toast.makeText(this, "Camera permission granted", Toast.LENGTH_SHORT).show()
            }
            else{
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)



        select_image_button = findViewById(R.id.button)
        make_prediction = findViewById(R.id.button2)
        img_view = findViewById(R.id.imageView2)
        text_view = findViewById(R.id.textView)
        camerabtn = findViewById<Button>(R.id.camerabtn)
        checkandGetpermissions()


        select_image_button.setOnClickListener(View.OnClickListener {
            Log.d("mssg", "button pressed")
            var intent : Intent1 = Intent1(Intent1.ACTION_GET_CONTENT)
            intent.type = "image/*"
            startActivityForResult(intent, 250)
        })
        camerabtn.setOnClickListener(View.OnClickListener {
            var camera : Intent1 = Intent1(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)
            startActivityForResult(camera, 200)
        })

    }


    @SuppressLint("SuspiciousIndentation")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent1?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(requestCode == 250){
            img_view.setImageURI(data?.data)

            var uuri : Uri ?= data?.data
            uri = uuri.toString()
            bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, uuri)
        }
        else if(requestCode == 200 && resultCode == Activity.RESULT_OK){
            bitmap = data?.extras?.get("data") as Bitmap
            img_view.setImageBitmap(bitmap)
        }
        fun ARGBBitmap(img: Bitmap): Bitmap {
            return img.copy(Bitmap.Config.ARGB_8888, true)
        }



        make_prediction.setOnClickListener(View.OnClickListener {
            val image = TensorImage.fromBitmap(ARGBBitmap(bitmap!!)).bitmap
            val inputWidth = 250
            val inputHeight = 190
            val inputBuffer = TensorBuffer.createFixedSize(intArrayOf(1, inputHeight, inputWidth, 1), DataType.FLOAT32)
            val resizedBitmap = Bitmap.createScaledBitmap(image, inputWidth, inputHeight, true)
            val pixelValues = convertBitmapToFloatArray(resizedBitmap)
            inputBuffer.loadArray(pixelValues)
            val inputTensorBuffer = inputBuffer.buffer
            val tfliteModel = FileUtil.loadMappedFile(applicationContext, "face_shape_recognizer.tflite")
            val tflite: Interpreter = Interpreter(tfliteModel)
            val outputBuffer = TensorBuffer.createFixedSize(intArrayOf(1, 5), DataType.FLOAT32)
            tflite.run(inputTensorBuffer, outputBuffer.buffer)
            val labels = listOf("Heart", "Oblong", "Oval", "Round", "Square")
            val outputArray = outputBuffer.floatArray
            val maxIndex = outputArray.indices.maxByOrNull { outputArray[it] } ?: -1
            val predictedLabel = labels[maxIndex]
            text_view.text = predictedLabel
            tflite.close()
        }   )

    }
    fun convertBitmapToFloatArray(bitmap: Bitmap): FloatArray {
        val numPixels = bitmap.width * bitmap.height
        val pixels = IntArray(numPixels)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        val floatValues = FloatArray(numPixels)
        for (i in 0 until numPixels) {
            floatValues[i] = (pixels[i] and 0xFF) / 255.0f
        }
        return floatValues
    }


}