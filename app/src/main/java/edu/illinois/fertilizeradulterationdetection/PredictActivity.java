package edu.illinois.fertilizeradulterationdetection;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.lang.GeoLocation;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.exif.GpsDirectory;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;

import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.Calendar;

public class PredictActivity extends AppCompatActivity {

    private static final int WRITE_EXTERNAL_STORAGE_CODE = 1;

    private Bitmap bitmap;
    private File image;
    private Uri uri;
    private String imagePath;

    private ProgressDialog progressDialog;
    private String id;
    private DatabaseReference databaseRef;
    private String prediction;

    private String storeName, district;

    private Interpreter tflite;
    private TensorImage tImage;
    private TensorBuffer probabilityBuffer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_predict);

        progressDialog = new ProgressDialog(this);

        Intent intent = getIntent();

        storeName = intent.getStringExtra("storeName");
        district = intent.getStringExtra("district");

        // uri & image bitmap
        uri = Uri.parse(getIntent().getStringExtra("uri"));
        try {
            bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
            ImageView imageView = findViewById(R.id.image);
            imageView.setImageBitmap(bitmap);
        } catch (Exception e) {
            Toast.makeText(this, "Failed to load image", Toast.LENGTH_LONG).show();
        }

        // get image path
        boolean isFromStorage = getIntent().getBooleanExtra("isFromStorage", false);
        if (isFromStorage) {
            imagePath = getRealPathFromURI(uri);
        } else {
            imagePath = uri.getPath();
        }

        // save button
        Button save = findViewById(R.id.save);
        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveImageAttr();
            }
        });


        // report button
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

        //Initialize firebase & retrieve data
        id = FirebaseAuth.getInstance().getCurrentUser().getUid();
//        id = "q";
        databaseRef = FirebaseDatabase.getInstance().getReference();

        // model preprocessing
        final ArrayList<Bitmap> images = splitImage();

        // TODO: test split image
//        Button button = findViewById(R.id.button);
//        button.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Bitmap image = images.get(count);
//                ImageView imageView = findViewById(R.id.image);
//                imageView.setImageBitmap(image);
//                count = (count + 1) % 12;
//            }
//        });

        // run model & predict
        initializeModel();

        prediction = predict() ? "Adulterated" : "Pure";

        TextView predView = findViewById(R.id.prediction);
        predView.setText(prediction);
    }

    /** ======================================= Model ================================================ **/

    private ArrayList<Bitmap> splitImage() {
        // rescale to 300 * 400
        // 3 * 4

        final int rows = 3;
        final int columns = 4;
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

    private void initializeModel() {
        ImageProcessor imageProcessor =
                new ImageProcessor.Builder()
                        .add(new ResizeOp(224, 224, ResizeOp.ResizeMethod.BILINEAR))
                        .build();

        tImage = new TensorImage(DataType.UINT8);
        tImage.load(bitmap);
        tImage = imageProcessor.process(tImage);

        probabilityBuffer =
                TensorBuffer.createFixedSize(new int[]{1, 1001}, DataType.UINT8);

        // Initialise the model
        try {
            MappedByteBuffer tfliteModel
                    = FileUtil.loadMappedFile(this,
                    "mobilenet_v1_1.0_224_quant.tflite");
            tflite = new Interpreter(tfliteModel);
        } catch (IOException e) {
            Log.e("tfliteSupport", "Error reading model", e);
        }
    }

    private boolean predict() {
        if (tflite != null) {
            tflite.run(tImage.getBuffer(), probabilityBuffer.getBuffer());
        }

        float[] result = probabilityBuffer.getFloatArray();
//        return result[0] <= 0.5;
        return Math.random() <= 0.5;

    }

    /** ====================================== Save to Database ================================================= **/

    private String getRealPathFromURI(Uri uri) {

        String filePath = "";
        String wholeID = DocumentsContract.getDocumentId(uri);

        // Split at colon, use second item in the array
        String id = wholeID.split(":")[1];

        String[] column = {MediaStore.Images.Media.DATA};

        // where id is equal to
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

    private void saveImageAttr() {

        progressDialog.setMessage("Uploading Image...");
        progressDialog.show();

        // Create info object
        ImageInfo info = new ImageInfo();
        info.setPrediction(prediction);
        info.setNote(getIntent().getStringExtra("note"));

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
            info.setDate(date);
        }

        // longitude & latitude
        GpsDirectory gpsDirectory = metadata.getFirstDirectoryOfType(GpsDirectory.class);
        if (gpsDirectory != null) {
            GeoLocation location = gpsDirectory.getGeoLocation();
            info.setLatitude(location.getLatitude());
            info.setLongitude(location.getLongitude());
        }

        // Save info to database
        String storeName = getIntent().getStringExtra("storeName");
//        assert storeName != null;
        DatabaseReference images = databaseRef.child(id).child("images").child(storeName);

        if (getIntent().hasExtra("District")) {
            images.child("district").setValue(getIntent().getStringExtra("District"));
            images.child("village").setValue(getIntent().getStringExtra("Village"));
        }

        String imageId = images.push().getKey();
        images.child(Objects.requireNonNull(imageId)).setValue(info);

        // Save image to storage
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == WRITE_EXTERNAL_STORAGE_CODE) {
            generateReport();
        }
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
        cs.drawText("district: " + district + "   store: " + storeName, (src.getWidth() - tPaint.measureText("district: " + district + "   store: " + storeName))/2, src.getHeight()-3*150, tPaint);

        //write date
        tPaint.setStyle(Paint.Style.FILL);
        tPaint.setColor(Color.WHITE);
        String date = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()).format(new Date());
        cs.drawText("date generated: " + date, (src.getWidth() - tPaint.measureText("date generated: " + date))/2, src.getHeight()-2*150, tPaint);

        try {

            dest.compress(Bitmap.CompressFormat.JPEG, 100, new FileOutputStream(new File(imagePath)));

            String imgSaved = MediaStore.Images.Media.insertImage(getContentResolver(), dest,imagePath+".png", "drawing");
            Toast.makeText(this, "Report Saved",Toast.LENGTH_LONG).show();

            //TODO: Lily - we'll discuss what to do after the prediction
//            Intent intent = new Intent(this, MainActivity.class);
//            startActivity(intent);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to generate report",Toast.LENGTH_LONG).show();
        }

    }
}


/** access gps location code
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