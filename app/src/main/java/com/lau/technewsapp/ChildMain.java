package com.lau.technewsapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.StrictMode;
import android.text.Layout;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Array;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChildMain extends AppCompatActivity {

    ListView view = null;
    SQLiteDatabase db = null;
    Cursor c = null;
    ArrayList<String> titles = null;
    SharedPreferences shared = null;
    ArrayAdapter<String> adapter = null;


    public class fetchIds extends AsyncTask<String, Void, String>{

        @Override
        protected String doInBackground(String... strings) {

            String s = null;

            try {

                Log.i("S", "We are here");

                URL url  = new URL(strings[0]);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                InputStream in = connection.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));

                String data = reader.readLine();
                while(data != null){
                    s += data;
                    data = reader.readLine();
                }

                Log.i("S", s);

            } catch (Exception e) {
                e.printStackTrace();
            }
            return s;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            Pattern p = Pattern.compile("null(.*)");
            Matcher m = p.matcher(s);
            m.find();
            s = m.group(1);
            Log.i("S", s);

            try {
                JSONArray ids = new JSONArray(s);
                String HTML = null;
                for (int i = 0; i < 20; i++) {
                    String id = ids.get(i).toString();
                    String mainJson = getHTML("https://hacker-news.firebaseio.com/v0/item/" +id+".json?print=pretty");
                    mainJson = removeNull(mainJson);
                    System.out.println(mainJson);

                    JSONObject jsonObject = new JSONObject(mainJson);
                    String title = jsonObject.getString("title");
                    System.out.println(title);


                    HTML = getHTML(jsonObject.getString("url"));
                    HTML = removeNull(HTML);
                    System.out.println(HTML);

                    ContentValues cv = new ContentValues();
                    cv.put("id", id);
                    cv.put("title", title);
                    cv.put("htmlContent", HTML);
                    db.insert("newss", null, cv);

                }

                c = db.rawQuery("Select * from newss", null);
                int idIndex = c.getColumnIndex("id");
                int titleIndex = c.getColumnIndex("title");
                int contentIndex = c.getColumnIndex("htmlContent");

                c.moveToFirst();
                titles = new ArrayList<>();
                int count=0;
                while(c!= null && count < 15){
                    if (count < 15) {
                        titles.add(c.getString(titleIndex));
                        Log.i("Title", c.getString(idIndex));
                        c.moveToNext();
                        count++;
                    }


                }
                System.out.println(titles.toString());
                Boolean check = shared.edit().putBoolean("saved", true).commit();
                Log.i("check", check.toString());

                adapter = new ArrayAdapter<>(getApplicationContext(), android.R.layout.simple_list_item_1, titles);
                view = (ListView) findViewById(R.id.listView);
                view.setAdapter(adapter);

            }catch (Exception e){
                e.printStackTrace();
            }

        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_child_main);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();

        StrictMode.setThreadPolicy(policy);
        titles = new ArrayList<>();
        adapter = new ArrayAdapter<>(getApplicationContext(), android.R.layout.simple_list_item_1, titles);
        view = (ListView) findViewById(R.id.listView);
        view.setAdapter(adapter);
        shared =  this.getSharedPreferences("com.lau.technewsapp", Context.MODE_PRIVATE);

        try{
            db = this.openOrCreateDatabase("newsdb", MODE_PRIVATE, null);
            db.execSQL("CREATE TABLE IF NOT EXISTS newss (id VARCHAR, title VARCHAR, htmlContent VARCHAR(8000))");
            //db.execSQL("DROP TABLE IF EXISTS newss");

        }catch (Exception e) {
            e.printStackTrace();

        }
        //shared.edit().remove("saved").commit();
        Boolean check = shared.contains("saved");
        Log.i("check", check.toString());
        if(check == false){
            fetchIds fetching = new fetchIds();
            fetching.execute("https://hacker-news.firebaseio.com/v0/topstories.json");
        }else{
            c = db.rawQuery("Select * from newss", null);

            c.moveToFirst();
            int count =0;
            int idIndex = c.getColumnIndex("id");
            int titleIndex = c.getColumnIndex("title");
            int contentIndex = c.getColumnIndex("htmlContent");
            while(c != null && count <16){
                titles.add(c.getString(titleIndex));
                count++;
                c.moveToNext();
            }


        }


        view.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Log.i("test", String.valueOf(i));
                WebView web = (WebView) findViewById(R.id.webView);
                web.setVisibility(View.VISIBLE);
                int contentIndex = c.getColumnIndex("htmlContent");
                Log.i("index", String.valueOf(contentIndex));
                c.moveToFirst();
                int count =0;
                while(c!=null && count < 16){
                    if(count == i){
                        web.loadData(c.getString(contentIndex), "text/html", "UTF-8");

                    }
                    count++;
                    c.moveToNext();
                }
            }
        });
        Log.i("we are directly", "here");
    }


    public String getHTML (String s){

        String html = null;
        try {
            URL url  = new URL(s);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.connect();

            InputStream in = connection.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));

            String data = reader.readLine();
            while(data != null){
                html += data;
                data = reader.readLine();
            }

        }catch (Exception e){
            e.printStackTrace();
        }
        return html;
    }

    public String removeNull(String s){
        Pattern p = Pattern.compile("null(.*)");
        Matcher m = p.matcher(s);
        m.find();
        s = m.group(1);
        return s;
    }


}