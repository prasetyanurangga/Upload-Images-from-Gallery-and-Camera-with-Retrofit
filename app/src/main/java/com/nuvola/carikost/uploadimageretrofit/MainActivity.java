package com.nuvola.carikost.uploadimageretrofit;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.nuvola.carikost.uploadimageretrofit.helper.UplodHelper;
import com.nuvola.carikost.uploadimageretrofit.service.RetrofitService;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private final static String TAG_BROWSE_PICTURE = "BROWSE_PICTURE";
    // Used when request action Intent.ACTION_GET_CONTENT
    private final static int REQUEST_CODE_BROWSE_PICTURE = 1;

    private final static int REQUEST_CODE_TAKE_PICTURE = 101;
    // Used when request read external storage permission.
    private final static int REQUEST_PERMISSION_READ_EXTERNAL = 2;

    private final static int REQUEST_PERMISSION_CAMERA = 3;
    // The image view that used to display user selected image.
    private ImageView selectedPictureImageView;
    // Save user selected image uri list.
    private List<Uri> userSelectedImageUriList = null;
    // Currently displayed user selected image index in userSelectedImageUriList.
    private int currentDisplayedUserSelectImageIndex = 0;

    String mCurrentPhotoPath;
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setTitle("dev2qa.com - Android Browse Picture Example.");
        // Get display imageview component.
        selectedPictureImageView = (ImageView)findViewById(R.id.selected_picture_imageview);
        // Get browse image button.
        Button choosePictureButton = (Button)findViewById(R.id.choose_picture_button);
        Button takePictureButton = (Button)findViewById(R.id.take_camera_button);
        final Button kirimButton = (Button)findViewById(R.id.send_button);
        kirimButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                kirim();
            }
        });
        takePictureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int readExternalStoragePermission = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA);
                if(readExternalStoragePermission != PackageManager.PERMISSION_GRANTED) {
                    String requirePermission[] = {Manifest.permission.CAMERA};
                    ActivityCompat.requestPermissions(MainActivity.this, requirePermission, REQUEST_PERMISSION_READ_EXTERNAL);
                }else {
                    Intent a = new Intent();
                    a.setAction(MediaStore.ACTION_IMAGE_CAPTURE);
                    startActivityForResult(a,REQUEST_CODE_TAKE_PICTURE);
                }
            }
        });
        choosePictureButton.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                // Because camera app returned uri value is something like file:///storage/41B7-12F1/DCIM/Camera/IMG_20180211_095139.jpg
                // So if show the camera image in image view, this app require below permission.
                int readExternalStoragePermission = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE);
                if(readExternalStoragePermission != PackageManager.PERMISSION_GRANTED) {
                    String requirePermission[] = {Manifest.permission.READ_EXTERNAL_STORAGE};
                    ActivityCompat.requestPermissions(MainActivity.this, requirePermission, REQUEST_PERMISSION_READ_EXTERNAL);
                }else {
                    openPictureGallery();
                }
            }
        });
        // Get show user selected images button.
        Button showSelectedPictureButton = (Button)findViewById(R.id.show_selected_picture_button);
        // When click this button. It will choose one user selected image to display in image view.
        showSelectedPictureButton.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                if( userSelectedImageUriList != null ) {
                    // Get current display image file uri.
                    Uri currentUri = userSelectedImageUriList.get(currentDisplayedUserSelectImageIndex);
                    ContentResolver contentResolver = getContentResolver();
                    try {
                        // User content resolver to get uri input stream.
                        InputStream inputStream = contentResolver.openInputStream(currentUri);
                        // Get the bitmap.
                        Bitmap imgBitmap = BitmapFactory.decodeStream(inputStream);
                        // Show image bitmap in imageview object.
                        selectedPictureImageView.setImageBitmap(imgBitmap);
                    }catch(FileNotFoundException ex) {
                        Log.e(TAG_BROWSE_PICTURE, ex.getMessage(), ex);
                    }
                    // Get total user selected image count.
                    int size = userSelectedImageUriList.size();
                    if(currentDisplayedUserSelectImageIndex >= (size - 1) ) {
                        // Avoid array index out of boundsexception.
                        currentDisplayedUserSelectImageIndex = 0;
                    }else {
                        currentDisplayedUserSelectImageIndex++;
                    }
                }
            }
        });
    }
    /* Invoke android os system file browser to select images. */
    private void openPictureGallery() {
        // Create an intent.
        Intent openAlbumIntent = new Intent();
        // Only show images in the content chooser.
        // If you want to select all type data then
        openAlbumIntent.setType("image/*");
        // Must set type for the intent, otherwise there will throw android.content.ActivityNotFoundException: No Activity found to handle Intent { act=android.intent.action.GET_CONTENT } openAlbumIntent.setType("image/*");
        // Set action, this action will invoke android os browse content app.
        openAlbumIntent.setAction(Intent.ACTION_PICK);
        // Start the activity.
        startActivityForResult(openAlbumIntent, REQUEST_CODE_BROWSE_PICTURE);
    }
    /* When the action Intent.ACTION_GET_CONTENT invoked app return, this method will be executed. */
    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode==REQUEST_CODE_BROWSE_PICTURE) {
            if(resultCode==RESULT_OK) {
                // Get return image uri. If select the image from camera the uri like file:///storage/41B7-12F1/DCIM/Camera/IMG_20180211_095139.jpg
                // If select the image from gallery the uri like content://media/external/images/media/1316970.
                Uri fileUri = data.getData();
                String filePath = getRealPathFromURIPath(fileUri, MainActivity.this);

                Log.e("data", filePath);

                // Save user choose image file uri in list.
                if(userSelectedImageUriList == null) {
                    userSelectedImageUriList = new ArrayList<Uri>();
                } userSelectedImageUriList.add(Uri.parse(filePath));
                //getUriRealPath(getApplicationContext(), fileUri);
                // Create content resolver.
                ContentResolver contentResolver = getContentResolver();
                try {
                    // Open the file input stream by the uri.
                    InputStream inputStream = contentResolver.openInputStream(fileUri);
                    // Get the bitmap.
                    Bitmap imgBitmap = BitmapFactory.decodeStream(inputStream);
                    Uri tempuri = ambil_alamat(MainActivity.this,imgBitmap);
                    // Show image bitmap in imageview object.
                    selectedPictureImageView.setImageBitmap(imgBitmap);
                    inputStream.close();
                }catch(FileNotFoundException ex) {
                    Log.e(TAG_BROWSE_PICTURE, ex.getMessage(), ex);
                }catch(IOException ex) {
                    Log.e(TAG_BROWSE_PICTURE, ex.getMessage(), ex);
                }
            }
        }
        if(requestCode==REQUEST_CODE_TAKE_PICTURE) {
            if(resultCode==RESULT_OK) {
                Bitmap thumbnail = (Bitmap) data.getExtras().get("data");
                //3
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                thumbnail.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
                //4
                File file = new File(Environment.getExternalStorageDirectory()+File.separator + "image.jpg");
                try {
                    file.createNewFile();
                    FileOutputStream fo = new FileOutputStream(file);
                    //5
                    fo.write(bytes.toByteArray());
                    fo.close();
                    mCurrentPhotoPath = file.getAbsolutePath();
                    if(userSelectedImageUriList == null) {
                        userSelectedImageUriList = new ArrayList<Uri>();
                    } userSelectedImageUriList.add(Uri.parse(mCurrentPhotoPath));
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    } /* After user choose grant read external storage permission or not. */
    public Uri ambil_alamat(Context context, Bitmap bitmap)
    {
        ByteArrayOutputStream byt = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG,100,byt);
        String path = MediaStore.Images.Media.insertImage(context.getContentResolver(),bitmap,"Title",null);
        return Uri.parse(path);
    }
    private String getRealPathFromURIPath(Uri contentURI, Activity activity) {
        Cursor cursor = activity.getContentResolver().query(contentURI, null, null, null, null);
        if (cursor == null) {
            return contentURI.getPath();
        } else {
            cursor.moveToFirst();
            int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
            return cursor.getString(idx);
        }
    }
    private File createImageFile() throws IOException{
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_"+ timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(imageFileName,".jpg",storageDir);
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    @Override public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode==REQUEST_PERMISSION_READ_EXTERNAL) {
            if(grantResults.length > 0) {
                int grantResult = grantResults[0];
                if(grantResult == PackageManager.PERMISSION_GRANTED) {
                    // If user grant the permission then open choose image popup dialog.
                    openPictureGallery();
                }else {
                    Toast.makeText(getApplicationContext(), "You denied read external storage permission.", Toast.LENGTH_LONG).show();
                }
            }
        }
        if(requestCode==REQUEST_PERMISSION_CAMERA) {
            if(grantResults.length > 0) {
                int grantResult = grantResults[0];
                if(grantResult == PackageManager.PERMISSION_GRANTED) {
                    // If user grant the permission then open choose image popup dialog.
                    Intent a = new Intent();
                    a.setAction(MediaStore.ACTION_IMAGE_CAPTURE);
                    startActivityForResult(a,REQUEST_CODE_TAKE_PICTURE);
                }else {
                    Toast.makeText(getApplicationContext(), "You denied read external storage permission.", Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    public void kirim()
    {
        RetrofitService service = UplodHelper.createService(RetrofitService.class);

        MultipartBody.Builder builder = new MultipartBody.Builder();
        builder.setType(MultipartBody.FORM);


        builder.addFormDataPart("event_name", "xyz");
        builder.addFormDataPart("desc", "Lorem ipsum");
        // Multiple Images
        for (int i = 0; i <userSelectedImageUriList.size() ; i++) {
            File file = new File(String.valueOf(userSelectedImageUriList.get(i)));
            RequestBody requestImage = RequestBody.create(MediaType.parse("multipart/form-data"), file);
            builder.addFormDataPart("event_images[]", file.getName(), RequestBody.create(MediaType.parse("multipart/form-data"), file));
        }


        MultipartBody requestBody = builder.build();
        Call<ResponseBody> call = service.event_store(requestBody);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    Toast.makeText(getBaseContext(),"All fine"+response.body().string(),Toast.LENGTH_SHORT).show();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e("error",t.getMessage());
            }
        });
    }
}
