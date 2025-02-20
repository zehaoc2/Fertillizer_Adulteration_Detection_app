package edu.illinois.fertilizeradulterationdetection.prediction;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.os.SystemClock;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.lang.GeoLocation;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.exif.GpsDirectory;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.common.TensorProcessor;
import org.tensorflow.lite.support.common.ops.NormalizeOp;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;

import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

import edu.illinois.fertilizeradulterationdetection.R;
import edu.illinois.fertilizeradulterationdetection.entity.Image;
import edu.illinois.fertilizeradulterationdetection.entity.Store;

public class PredictActivity extends AppCompatActivity {

    private static final int WRITE_EXTERNAL_STORAGE_CODE = 1;

    private Bitmap bitmap;
    private File image;
    private Uri uri;
    private String imagePath;

    private String id;
    private DatabaseReference databaseRef;
    private String prediction;

    private String store, district, village;

    private Interpreter tflite;
    private final Interpreter.Options tfliteOptions = new Interpreter.Options();
    private ByteBuffer imgData;
    private final int X = 100, Y = 100;
    private int[] intValues = new int[X * Y];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_predict);

        initialization();

        // model preprocessing, first resize the image into 300 * 400, then slice it into twelve 100 * 100 sub-images.
        bitmap = Bitmap.createScaledBitmap(bitmap, 300, 400 ,true ) ;
        final ArrayList<Bitmap> images = splitImage();

        // initialize model & predict
        initializeModel();

        // Our criteria of determining adulteration: if more than 8 sub-images is predicted as pure, then the image is predicted as pure
        int pureCount = 0;
        for (Bitmap image : images) {
            if (!predict(image)) {
                pureCount++;
            }
        }
        prediction = pureCount >= 8 ? "Pure" : "Adulterated";

        TextView predView = findViewById(R.id.prediction);
        predView.setText(prediction);
    }

    /** ======================================= Initialization ================================================ **/

    private void initialization() {
        // retrieve data from previous activity
        Intent intent = getIntent();
        store = intent.getStringExtra("Store");
        district = intent.getStringExtra("District");
        village = intent.getStringExtra("Village");
        uri = Uri.parse(intent.getStringExtra("uri"));

        // background image
        try {
            bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
            ImageView imageView = findViewById(R.id.image);
            imageView.setImageBitmap(bitmap);
        } catch (Exception e) {
            Toast.makeText(this, "Failed to load image", Toast.LENGTH_LONG).show();
        }

        // get image path of the photo in the phone
        boolean isFromStorage = getIntent().getBooleanExtra("isFromStorage", false);
        if (isFromStorage) {
            imagePath = getRealPathFromURI(uri);
        } else {
            imagePath = uri.getPath();
        }

        // button initialization
        Button save = findViewById(R.id.save);
        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveImageAttr();
            }
        });

        Button report = findViewById(R.id.report);
        report.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ContextCompat.checkSelfPermission(PredictActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(PredictActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, WRITE_EXTERNAL_STORAGE_CODE);
                } else {
                    generateReport();
                }
            }
        });

        // firebase initialize
        id = FirebaseAuth.getInstance().getCurrentUser().getUid();
        databaseRef = FirebaseDatabase.getInstance().getReference();
    }

    //code from stackOverflow: https://stackoverflow.com/a/23920731/12234267
    private String getRealPathFromURI(Uri uri) {

        String filePath = "";
        String wholeID = DocumentsContract.getDocumentId(uri);

        String id = wholeID.split(":")[1];
        String[] column = {MediaStore.Images.Media.DATA};

        String sel = MediaStore.Images.Media._ID + "=?";
        Cursor cursor = this.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                column, sel, new String[]{id}, null);

        assert cursor != null;
        int columnIndex = cursor.getColumnIndex(column[0]);

        if (cursor.moveToFirst()) {
            filePath = cursor.getString(columnIndex);
        }
        cursor.close();
        return filePath;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == WRITE_EXTERNAL_STORAGE_CODE) {
            generateReport();
        }
    }

    /** ======================================= Model ================================================ **/

    // Split each image into 12 sub-images to increase robustness
    private ArrayList<Bitmap> splitImage() {
        final int rows = 4;
        final int columns = 3;
        final int chunks = rows * columns;
        final int height = bitmap.getHeight() / rows;
        final int width = bitmap.getWidth() / columns;

        ArrayList<Bitmap> images = new ArrayList<>(chunks);

        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, bitmap.getWidth(), bitmap.getHeight(), true);

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                int xCoord = j * width, yCoord = i * height;
                Bitmap image = Bitmap.createBitmap(scaledBitmap, xCoord, yCoord, width, height);
                images.add(image);
            }
        }

        return images;
    }

    // Please refer to the demo from TensorFlow official github site:
    // run "git clone https://www.github.com/tensorflow/tensorflow" and find its demo at /tensorflow/lite/java/demo
    // Useful link: https://medium.com/tensorflow/using-tensorflow-lite-on-android-9bbc9cb7d69d
    private void initializeModel() {
        final int DIM_BATCH_SIZE = 1, DIM_PIXEL_SIZE = 3, NumBytesPerChannel = 4;

        imgData = ByteBuffer.allocateDirect(
                        DIM_BATCH_SIZE
                                * X
                                * Y
                                * DIM_PIXEL_SIZE
                                * NumBytesPerChannel);
        imgData.order(ByteOrder.nativeOrder());

        try {
            tflite = new Interpreter(FileUtil.loadMappedFile(this,"model.tflite"), tfliteOptions);
        } catch (IOException e) {
            Log.e("tfliteSupport", "Error reading model", e);
        }
    }

    // convert bitmap of each sub-image to ByteBuffer to feed into the TensorFlowLite.
    private void convertBitmapToByteBuffer(Bitmap bitmap) {
        if (imgData == null) {
            return;
        }
        imgData.rewind();
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        int pixel = 0;
        for (int i = 0; i < X; ++i) {
            for (int j = 0; j < Y; ++j) {
                final int val = intValues[pixel++];

                imgData.putFloat(((val>> 16) & 0xFF) / 255.f);
                imgData.putFloat(((val>> 8) & 0xFF) / 255.f);
                imgData.putFloat((val & 0xFF) / 255.f);
            }
        }
    }

    // return true if adulterated (probability < 0.5), false if pure (probability >= 0.5)
    private boolean predict(Bitmap image) {
        convertBitmapToByteBuffer(image);

        float[][] result = new float[1][1];
        if (tflite != null) {
            tflite.run(imgData, result);
        }

        return result[0][0] < 0.5;
    }

    /** ====================================== Save to Database ================================================= **/

    private void saveImageAttr() {

        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Uploading Image...");
        progressDialog.show();

        // Create info object
        Image img = new Image();
        img.setPrediction(prediction);
        img.setNote(getIntent().getStringExtra("note"));

        // Retrieve image metadata
        image = new File(imagePath);
        Metadata metadata = null;
        try {
            metadata = ImageMetadataReader.readMetadata(image);
        } catch (ImageProcessingException | IOException e) {
            e.printStackTrace();
        }
        assert metadata != null;

        // date
        ExifSubIFDDirectory exifSubIFDDirectory = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
        if (exifSubIFDDirectory != null) {
            String date = exifSubIFDDirectory.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL).toString();
            img.setDate(date);
        }

        // longitude & latitude
        GpsDirectory gpsDirectory = metadata.getFirstDirectoryOfType(GpsDirectory.class);
        if (gpsDirectory != null) {
            GeoLocation location = gpsDirectory.getGeoLocation();
            img.setLatitude(location.getLatitude());
            img.setLongitude(location.getLongitude());
        }

        // ========== Save Image internally ========== //
        boolean newStore = getIntent().getBooleanExtra("newStore", true);
        Gson gson = new GsonBuilder().setPrettyPrinting().create(); // pretty print JSON
        SharedPreferences mPrefs = getSharedPreferences("fertilizer_test_data", MODE_PRIVATE);
        Store str;

        //new Store
        if(newStore){
            str = new Store(store, district);
        }
        //old store
        else{
            String json = mPrefs.getString(store, null);
            Type type = new TypeToken<Store>() {}.getType();
            str = gson.fromJson(json, type);
        }
        str.addImage(img);

        String jsonString = gson.toJson(str, Store.class);
        //save to SharedPreferences
        SharedPreferences.Editor prefsEditor = mPrefs.edit();
        prefsEditor.putString(store, jsonString);
        System.out.println(jsonString);
        prefsEditor.commit();

        // ========== Save to Firebase ========== //
        // save metadata to database
        DatabaseReference images = databaseRef.child(id).child("images").child(store);

        if (getIntent().hasExtra("District")) {
            images.child("district").setValue(district);
            images.child("village").setValue(village);
        }

        String imageId = images.push().getKey();
        images.child(Objects.requireNonNull(imageId)).setValue(img);

        // save image to storage
        StorageReference storageRef = FirebaseStorage.getInstance().getReference().child(imageId);
        storageRef.putFile(uri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                Toast.makeText(getApplicationContext(), "Upload Success!", Toast.LENGTH_SHORT).show();
                progressDialog.dismiss();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(getApplicationContext(), "Upload Failed!", Toast.LENGTH_SHORT).show();
                progressDialog.dismiss();
                Log.e("my", e.toString());
                //save to local!
                try {
                    String imgSaved = MediaStore.Images.Media.insertImage(getContentResolver(), uri.getPath(), image.getAbsolutePath() + ".png", "drawing");
                } catch (FileNotFoundException x) {
                    x.printStackTrace();
                }
            }
        });

    }

    /** ====================================== Generate Report ================================================= **/

    private void generateReport() {

        Bitmap src = BitmapFactory.decodeFile(imagePath);
        Bitmap dest = Bitmap.createBitmap(src.getWidth(), src.getHeight(), Bitmap.Config.ARGB_8888);


        Canvas cs = new Canvas(dest);
        Paint tPaint = new Paint();

        //draw background image
        cs.drawBitmap(src, 0f, 0f,  null);

        //draw black background
        tPaint.setTextSize(150f);
        tPaint.setStyle(Paint.Style.FILL);
        float height = tPaint.measureText("yY");
        tPaint.setColor(Color.BLACK);
        cs.drawRect(0, src.getHeight() - height*5, src.getWidth(),src.getHeight(), tPaint);

        //write prediction
        tPaint.setColor(Color.WHITE);
        cs.drawText("prediction: " + prediction, (src.getWidth() - tPaint.measureText("prediction: " + prediction))/2, src.getHeight()-4*height, tPaint);

        //write store
        tPaint.setTextSize(100f);
        tPaint.setStyle(Paint.Style.FILL);
        height = tPaint.measureText("yY");
        tPaint.setColor(Color.WHITE);
        cs.drawText("district: " + district + "   store: " + store, (src.getWidth() - tPaint.measureText("district: " + district + "   store: " + store))/2, src.getHeight()-3*150, tPaint);

        //write date
        tPaint.setStyle(Paint.Style.FILL);
        tPaint.setColor(Color.WHITE);
        String date = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()).format(new Date());
        cs.drawText("date generated: " + date, (src.getWidth() - tPaint.measureText("date generated: " + date))/2, src.getHeight()-2*150, tPaint);

        try {

            dest.compress(Bitmap.CompressFormat.JPEG, 100, new FileOutputStream(new File(imagePath)));

            String imgSaved = MediaStore.Images.Media.insertImage(getContentResolver(), dest,imagePath+".png", "drawing");
            Toast.makeText(this, "Report Saved",Toast.LENGTH_LONG).show();

//            Intent intent = new Intent(this, MainActivity.class);
//            startActivity(intent);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to generate report",Toast.LENGTH_LONG).show();
        }

    }
}


/** code for access gps location
 *
 * if (ActivityCompat.checkSelfPermission(PredictActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(PredictActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
 * if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
 *    locationManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, locationListener, null);
 *     locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
 *     }
 * }
 *
 *
 *         locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
 *         locationListener = new LocationListener() {
 *             @Override
 *             public void onLocationChanged(Location location) {
 *                 latitude = location.getLatitude();
 *                 longitude = location.getLongitude();
 *
 *                 //locationManager.removeUpdates(this);
 *             }
 *
 *             @Override
 *             public void onStatusChanged(String s, int i, Bundle bundle) {
 *
 *             }
 *
 *             @Override
 *             public void onProviderEnabled(String s) {
 *
 *             }
 *
 *             @Override
 *             public void onProviderDisabled(String s) {
 *                 //Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
 *                 //startActivity(intent);
 *             }
 *         };
 *
 *
 **/