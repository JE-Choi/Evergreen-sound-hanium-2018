//
// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license.
//
// Microsoft Cognitive Services (formerly Project Oxford): https://www.microsoft.com/cognitive-services
//
// Microsoft Cognitive Services (formerly Project Oxford) GitHub:
// https://github.com/Microsoft/Cognitive-Face-Android
//
// Copyright (c) Microsoft Corporation
// All rights reserved.
//
// MIT License:
// Permission is hereby granted, free of charge, to any person obtaining
// a copy of this software and associated documentation files (the
// "Software"), to deal in the Software without restriction, including
// without limitation the rights to use, copy, modify, merge, publish,
// distribute, sublicense, and/or sell copies of the Software, and to
// permit persons to whom the Software is furnished to do so, subject to
// the following conditions:
//
// The above copyright notice and this permission notice shall be
// included in all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED ""AS IS"", WITHOUT WARRANTY OF ANY KIND,
// EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
// MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
// NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
// LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
// OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
// WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
//
package com.microsoft.projectoxford.face.samples.persongroupmanagement;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


import com.microsoft.projectoxford.face.FaceServiceClient;
import com.microsoft.projectoxford.face.contract.AddPersistedFaceResult;
import com.microsoft.projectoxford.face.contract.Face;
import com.microsoft.projectoxford.face.contract.FaceRectangle;
import com.microsoft.projectoxford.face.samples.R;
import com.microsoft.projectoxford.face.samples.helper.ImageHelper;
import com.microsoft.projectoxford.face.samples.helper.LogHelper;
import com.microsoft.projectoxford.face.samples.helper.SampleApp;
import com.microsoft.projectoxford.face.samples.helper.StorageHelper;
import com.microsoft.projectoxford.face.samples.ui.MainActivity;
import com.microsoft.projectoxford.face.samples.ui.PersonSelectImage;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class AddFaceToPersonActivity extends AppCompatActivity {
    //등록된 인물 사진 저장경로 - 211번째줄에서 처리
    // 유아이 : 소희
    private Uri personImageUri;
    private static final int REQUEST_SELECT_IMAGE = 0;

    //*/ DB
    private static String IP_ADDRESS = "14.63.195.105"; // 한이음 서버 IP
    private static String TAG = "php";
    String userName="B_tester";
    String userPass="1111";
    String DatabaseName ="B_db";

    // Background task of adding a face to person.
    class AddFaceTask extends AsyncTask<Void, String, Boolean> {
        List<Integer> mFaceIndices;
        AddFaceTask(List<Integer> faceIndices) {
            mFaceIndices = faceIndices;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            // Get an instance of face service client to detect faces in image.
            FaceServiceClient faceServiceClient = SampleApp.getFaceServiceClient();
            try{
                publishProgress("Adding face...");
                UUID personId = UUID.fromString(mPersonId);

                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                mBitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                InputStream imageInputStream = new ByteArrayInputStream(stream.toByteArray());

                for (Integer index: mFaceIndices) {
                    FaceRectangle faceRect = mFaceGridViewAdapter.faceRectList.get(index);
                    addLog("Request: Adding face to person " + mPersonId);
                    // Start the request to add face.
                    AddPersistedFaceResult result = faceServiceClient.addPersonFaceInLargePersonGroup(
                            mPersonGroupId,
                            personId,
                            imageInputStream,
                            "User data",
                            faceRect);

                    mFaceGridViewAdapter.faceIdList.set(index, result.persistedFaceId);
                }
                return true;
            } catch (Exception e) {
                publishProgress(e.getMessage());
                addLog(e.getMessage());
                return false;
            }
        }

        @Override
        protected void onPreExecute() {
            setUiBeforeBackgroundTask();
        }

        @Override
        protected void onProgressUpdate(String... progress) {
            setUiDuringBackgroundTask(progress[0]);
        }

        @Override
        protected void onPostExecute(Boolean result) {
            setUiAfterAddingFace(result, mFaceIndices);
        }
    }

    // Background task of face detection.
    private class DetectionTask extends AsyncTask<InputStream, String, Face[]> {
        private boolean mSucceed = true;

        @Override
        protected Face[] doInBackground(InputStream... params) {
            // Get an instance of face service client to detect faces in image.
            FaceServiceClient faceServiceClient = SampleApp.getFaceServiceClient();
            try{
                publishProgress("Detecting...");

                // Start detection.
                return faceServiceClient.detect(
                        params[0],  /* Input stream of image to detect */
                        true,       /* Whether to return face ID */
                        false,       /* Whether to return face landmarks */
                        /* Which face attributes to analyze, currently we support:
                           age,gender,headPose,smile,facialHair */
                        null);
            }  catch (Exception e) {
                mSucceed = false;
                publishProgress(e.getMessage());
                addLog(e.getMessage());
                return null;
            }
        }

        @Override
        protected void onPreExecute() {
            setUiBeforeBackgroundTask();
        }

        @Override
        protected void onProgressUpdate(String... progress) {
            setUiDuringBackgroundTask(progress[0]);
        }

        @Override
        protected void onPostExecute(Face[] faces) {
            if (mSucceed) {
                addLog("Response: Success. Detected " + (faces == null ? 0 : faces.length)
                        + " Face(s)");
            }

            // Show the result on screen when detection is done.
            setUiAfterDetection(faces, mSucceed);
        }
    }

    private void setUiBeforeBackgroundTask() {
        mProgressDialog.show();
    }

    // Show the status of background detection task on screen.
    private void setUiDuringBackgroundTask(String progress) {
        mProgressDialog.setMessage(progress);
        setInfo(progress);
    }

    private void setUiAfterAddingFace(boolean succeed, List<Integer> faceIndices) {
        mProgressDialog.dismiss();
        if (succeed) {
            String faceIds = "";
            for (Integer index : faceIndices) {
                String faceId = mFaceGridViewAdapter.faceIdList.get(index).toString();
                faceIds += faceId + ", ";
                FileOutputStream fileOutputStream = null;
                try {
                    File file = new File(getApplicationContext().getFilesDir(), faceId);
                    fileOutputStream = new FileOutputStream(file);
                    mFaceGridViewAdapter.faceThumbnails.get(index)
                            .compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream);
                    fileOutputStream.flush();

                    Uri uri = Uri.fromFile(file);
                    personImageUri = Uri.fromFile(file);//등록된 인물 사진 저장경로 받아오기
                    StorageHelper.setFaceUri(
                            faceId, uri.toString(), mPersonId, AddFaceToPersonActivity.this);

                    //*/지은: DB registered_person_tb 삽입
                    SharedPreferences insert = getSharedPreferences("RegisteredTB_Pref", MODE_PRIVATE);
                    String name = insert.getString("name","null");

                    SharedPreferences sharedPreferences = getSharedPreferences("USER",MODE_PRIVATE);
                    if(!(sharedPreferences.getString("ID","").equals(""))){
                        userName = sharedPreferences.getString("ID","");
                        DatabaseName = userName+"_db";
                        insert_registered_person_tb task = new insert_registered_person_tb();
                        task.execute("http://" + IP_ADDRESS + "/insert_registered_person_tb.php",userName,userPass,DatabaseName,name,personImageUri+"");
                    }

                } catch (IOException e) {
                    setInfo(e.getMessage());
                } finally {
                    if (fileOutputStream != null) {
                        try {
                            fileOutputStream.close();
                        } catch (IOException e) {
                            setInfo(e.getMessage());
                        }
                    }
                }
            }
            addLog("Response: Success. Face(s) " + faceIds + "added to person " + mPersonId);
            finish();
        }
    }

    // Show the result on screen when detection is done.
    private void setUiAfterDetection(Face[] result, boolean succeed) {
        mProgressDialog.dismiss();

        if (succeed) {
            // Set the information about the detection result.
            if (result != null) {
                setInfo(result.length + " face"
                        + (result.length != 1 ? "s" : "") + " detected");
            } else {
                setInfo("0 face detected");
            }

            // Set the adapter of the ListView which contains the details of the detected faces.
            mFaceGridViewAdapter = new FaceGridViewAdapter(result);

            // Show the detailed list of detected faces.
            GridView gridView = (GridView) findViewById(R.id.gridView_faces_to_select);
            gridView.setAdapter(mFaceGridViewAdapter);

            SharedPreferences pref = getSharedPreferences("machine",MODE_PRIVATE);
            if(pref.getBoolean("input",false) == false){
                // item 개수, 인식된 얼굴 개수
                int count = mFaceGridViewAdapter.faceThumbnails.size();
                if(count==0){
                    check("인물이 인식되지 않았습니다. 다시 촬영해주세요.");
                }else if(count == 1){
                    doneAndSave();
                }else{
                    check("여러명이 인식되었습니다. 한명의 인물만을 다시 촬영해주세요.");
                }
            }else{
                SharedPreferences insert = getSharedPreferences("machine", MODE_PRIVATE);
                SharedPreferences.Editor editor = insert.edit();
                editor.putBoolean("end", true);
                editor.putBoolean("group", false);
                editor.commit();
                doneAndSave();
            }


            //*/0825
            // test();
        }
    }

    String mPersonGroupId;
    String mPersonId;
    String mImageUriStr;
    Bitmap mBitmap;
    FaceGridViewAdapter mFaceGridViewAdapter;

    // Progress dialog popped up when communicating with server.
    ProgressDialog mProgressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_face_to_person);
        super.setTitle("인물 추가 화면");
        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            mPersonId = bundle.getString("PersonId");
            mPersonGroupId = bundle.getString("PersonGroupId");
            mImageUriStr = bundle.getString("ImageUriStr");
        }

        mProgressDialog = new ProgressDialog(this);
        //mProgressDialog.setTitle(getString(R.string.progress_dialog_title));
        mProgressDialog.setTitle("잠시 기다려 주세요.");

        //*/
        /*SharedPreferences insert = getSharedPreferences("test", MODE_PRIVATE);
        SharedPreferences.Editor editor = insert.edit();
        editor.putBoolean("end", true);
        editor.putBoolean("group", false);
        editor.commit(); //완료한다.*/
        //*/

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString("PersonId", mPersonId);
        outState.putString("PersonGroupId", mPersonGroupId);
        outState.putString("ImageUriStr", mImageUriStr);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        mPersonId = savedInstanceState.getString("PersonId");
        mPersonGroupId = savedInstanceState.getString("PersonGroupId");
        mImageUriStr = savedInstanceState.getString("ImageUriStr");
    }

    @Override
    protected void onResume() {
        super.onResume();

        Uri imageUri = Uri.parse(mImageUriStr);
        mBitmap = ImageHelper.loadSizeLimitedBitmapFromUri(
                imageUri, getContentResolver());
        if (mBitmap != null) {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            mBitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
            InputStream imageInputStream = new ByteArrayInputStream(stream.toByteArray());
            addLog("Request: Detecting " + mImageUriStr);
            new DetectionTask().execute(imageInputStream);
        }
    }

    public void doneAndSave(View view) {
        if (mFaceGridViewAdapter != null) {
            List<Integer> faceIndices = new ArrayList<>();

            for (int i = 0; i < mFaceGridViewAdapter.faceRectList.size(); ++i) {
                if (mFaceGridViewAdapter.faceChecked.get(i)) {
                    faceIndices.add(i);
                }
            }

            if (faceIndices.size() > 0) {
                new AddFaceTask(faceIndices).execute();
            } else {
                finish();
            }
        }
    }
    public void doneAndSave(){
        if (mFaceGridViewAdapter != null) {
            List<Integer> faceIndices = new ArrayList<>();

            for (int i = 0; i < mFaceGridViewAdapter.faceRectList.size(); ++i) {
                if (mFaceGridViewAdapter.faceChecked.get(i)) {
                    faceIndices.add(i);
                }
            }

            if (faceIndices.size() > 0) {
                new AddFaceTask(faceIndices).execute();
            } else {
                finish();
            }
        }
    } // doneAndSave() end.
    // Add a log item.
    private void addLog(String log) {
        LogHelper.addIdentificationLog(log);
    }

    // Set the information panel on screen.
    private void setInfo(String info) {
        TextView textView = (TextView) findViewById(R.id.info);
        textView.setText(info);
    }

    private class FaceGridViewAdapter extends BaseAdapter {
        List<UUID> faceIdList;
        List<FaceRectangle> faceRectList;
        List<Bitmap> faceThumbnails;
        List<Boolean> faceChecked;

        FaceGridViewAdapter(Face[] detectionResult) {
            faceIdList = new ArrayList<>();
            faceRectList = new ArrayList<>();
            faceThumbnails = new ArrayList<>();
            faceChecked = new ArrayList<>();

            if (detectionResult != null) {
                List<Face> faces = Arrays.asList(detectionResult);
                for (Face face : faces) {
                    try {
                        // Crop face thumbnail with five main landmarks drawn from original image.
                        faceThumbnails.add(ImageHelper.generateFaceThumbnail(
                                mBitmap, face.faceRectangle));

                        faceIdList.add(null);
                        faceRectList.add(face.faceRectangle);

                        faceChecked.add(true);
                    } catch (IOException e) {
                        // Show the exception when generating face thumbnail fails.
                        setInfo(e.getMessage());
                    }
                }
            }
        }

        @Override
        public int getCount() {
            return faceRectList.size();
        }

        @Override
        public Object getItem(int position) {
            return faceRectList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            // set the item view
            if (convertView == null) {
                LayoutInflater layoutInflater =
                        (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView =
                        layoutInflater.inflate(R.layout.item_face_with_checkbox, parent, false);
            }
            convertView.setId(position);

            ((ImageView)convertView.findViewById(R.id.image_face))
                    .setImageBitmap(faceThumbnails.get(position));

            // set the checked status of the item
            CheckBox checkBox = (CheckBox) convertView.findViewById(R.id.checkbox_face);
            checkBox.setChecked(faceChecked.get(position));
            checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    faceChecked.set(position, isChecked);
                }
            });

            return convertView;
        }
    }

    class insert_registered_person_tb extends AsyncTask<String, Void, String> {
        ProgressDialog progressDialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            progressDialog = ProgressDialog.show(AddFaceToPersonActivity.this,
                    "잠시 기다려 주세요.", null, true, true);
        }


        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            progressDialog.dismiss();
        }


        @Override
        protected String doInBackground(String... params) {

            String serverURL = (String)params[0];
            String userName = (String)params[1];
            String userPass = (String)params[2];
            String databaseName = (String)params[3];
            String name = (String)params[4];
            String person_img_path = (String)params[5];


            String postParameters = "&userName=" + userName
                    +"&userPass=" + userPass
                    +"&databaseName=" + databaseName
                    +"&name=" + name
                    +"&person_img_path=" + person_img_path; // php에 보낼값.

            try {
                // php 가져오기.
                URL url = new URL(serverURL+"");
                HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();


                httpURLConnection.setReadTimeout(5000);
                httpURLConnection.setConnectTimeout(5000);
                httpURLConnection.setRequestMethod("POST");
                httpURLConnection.connect();


                OutputStream outputStream = httpURLConnection.getOutputStream();
                outputStream.write(postParameters.getBytes("UTF-8"));
                outputStream.flush();
                outputStream.close();


                int responseStatusCode = httpURLConnection.getResponseCode();

                InputStream inputStream;
                if(responseStatusCode == HttpURLConnection.HTTP_OK) {
                    inputStream = httpURLConnection.getInputStream();
                }
                else{
                    inputStream = httpURLConnection.getErrorStream();
                }


                InputStreamReader inputStreamReader = new InputStreamReader(inputStream, "UTF-8");
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

                StringBuilder sb = new StringBuilder();
                String line = null;

                while((line = bufferedReader.readLine()) != null){
                    sb.append(line);
                }


                bufferedReader.close();



                return sb.toString();


            } catch (Exception e) {

                Log.d(TAG, "createDB: Error ", e);
                return new String("Error: " + e.getMessage());
            }

        }
    } //insert_registered_person_tb() end

    //0825
    public void test(){
        try{
            if (mFaceGridViewAdapter != null) {
                List<Integer> faceIndices = new ArrayList<>();

                for (int i = 0; i < mFaceGridViewAdapter.faceRectList.size(); ++i) {
                    if (mFaceGridViewAdapter.faceChecked.get(i)) {
                        faceIndices.add(i);
                    }
                }

                if (faceIndices.size() > 0) {
                    new AddFaceTask(faceIndices).execute();
                } else {
                    finish();
                }
            }
        }catch (Exception e){
            finish();
        }

    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode)
        {
            case REQUEST_SELECT_IMAGE:
                if (resultCode == RESULT_OK) {
                    Uri uriImagePicked = data.getData();
                    Intent intent = new Intent(this, AddFaceToPersonActivity.class);
                    intent.putExtra("PersonId", mPersonId);
                    intent.putExtra("PersonGroupId", mPersonGroupId);
                    intent.putExtra("ImageUriStr", uriImagePicked.toString());
                    startActivity(intent);
                    finish();
                }
                break;
            default:
                break;
        }
    }
    // 인물등록 재촬영 확인 다이얼로그
    public void check(String result){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        String message="";

        message = result;
        builder.setCancelable(false);
        builder.setMessage(message)
                .setPositiveButton("네", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        //긍정 버튼을 클릭했을 때, 실행할 동작
                        picture();
                    }
                });
        builder.setCancelable(false);
        builder.show();
    } //checkInput () end.

    private void picture(){
        Intent intent = new Intent(this, PersonSelectImage.class);
        startActivityForResult(intent, REQUEST_SELECT_IMAGE);
    }


}
