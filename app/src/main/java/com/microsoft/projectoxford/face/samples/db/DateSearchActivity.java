package com.microsoft.projectoxford.face.samples.db;

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.microsoft.projectoxford.face.samples.R;

public class DateSearchActivity extends AppCompatActivity {

    private EditText dateInput;
    private final int REQ_CODE_SPEECH_INPUT = 100;
    //  private TextToSpeech tts;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_date_search);
        setTitle("날짜 검색 화면");
        dateInput = (EditText)findViewById(R.id.dateInput);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        dateInput = (EditText)findViewById(R.id.dateInput);
        // TTS를 생성하고 OnInitListener로 초기화 한다.
    /*    tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status != ERROR) {
                    // 언어를 선택한다.
                    tts.setLanguage(Locale.KOREAN);
                }
            }
        });*/

    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void OnClickedfinsh(View view) {
        SharedPreferences search = getSharedPreferences("searchSource", MODE_PRIVATE);
        SharedPreferences.Editor editor = search.edit();
        editor.putString("date",dateInput.getText().toString());
        editor.commit();
        Intent intent = new Intent(this,SearchResultctivity.class);
        startActivity(intent);
    }
}
