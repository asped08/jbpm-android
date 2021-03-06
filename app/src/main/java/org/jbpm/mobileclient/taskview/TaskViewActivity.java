/**
*   Copyright 2015 A.S.P. Athukorala
*
*   Licensed under the Apache License, Version 2.0 (the "License");
*   you may not use this file except in compliance with the License.
*   You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
*   Unless required by applicable law or agreed to in writing, software
*   distributed under the License is distributed on an "AS IS" BASIS,
*   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*   See the License for the specific language governing permissions and
*   limitations under the License.
*
*/
package org.jbpm.mobileclient.taskView;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import org.jbpm.mobileclient.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutionException;

public class TaskViewActivity extends Activity implements View.OnClickListener {

    ToggleButton btnClaim;
    String usrname;
    String authHeader;

    Button btnStart;
    Button btnFail;
    Button btnComplete;

    TextView taskId;
    TextView descriptionData;
    TextView responseId;


    EditText responseData;

    TaskObject taskObject;
    String taskname = " ",serverAddress="";
    Intent intent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_view);

        intent = getIntent();
        usrname = intent.getExtras().getString("username");
        authHeader = intent.getExtras().getString("AuthHeader");
        taskObject = (TaskObject) intent.getSerializableExtra("taskObject");

        serverAddress+=intent.getExtras().getString("ServerAddress");

        // Claim and Release Button
        btnClaim = (ToggleButton) findViewById(R.id.claimRelease);
        btnClaim.setOnClickListener(this);
        if (taskObject.getStatus().equals("Ready")) {
            btnClaim.setChecked(false);
        } else {
            btnClaim.setChecked(true);
        }


        // Task Complete button
        btnStart = (Button) findViewById(R.id.taskStart);
        btnStart.setOnClickListener(this);

        if (taskObject.getStatus().equals("Reserved")) {
            btnStart.setVisibility(View.VISIBLE);
        } else if (taskObject.getStatus().equals("InProgress")) {
            btnStart.setVisibility(View.INVISIBLE);
        }

        // Task Fail button
        btnFail = (Button) findViewById(R.id.taskfail);
        btnFail.setOnClickListener(this);

        if (taskObject.getStatus().equals("InProgress")) {
            btnFail.setVisibility(View.VISIBLE);
        } else btnFail.setVisibility(View.INVISIBLE);


        // Task Delegate button
        btnComplete = (Button) findViewById(R.id.taskComplete);
        btnComplete.setOnClickListener(this);
        if (taskObject.getStatus().equals("InProgress")) {
            btnComplete.setVisibility(View.VISIBLE);
        } else {
            btnComplete.setVisibility(View.INVISIBLE);
        }
        // Task Name Description
        taskId = (TextView) findViewById(R.id.taskviewid);
        // Task Description Data
        descriptionData = (TextView) findViewById(R.id.descriptiondata);

        // Task Response Id
        responseId = (TextView) findViewById(R.id.responseId);
        // Task Response Data
        responseData = (EditText) findViewById(R.id.responsedata);

        taskname = taskObject.getTaskId() + " - " + taskObject.getName();
        taskId.setText(taskname);
        descriptionData.setText(taskObject.getDetails());

        intent = new Intent(this, TaskActivity.class);
        intent.putExtra("username", usrname);
        intent.putExtra("AuthHeader", authHeader);
        intent.putExtra("ServerAddress",serverAddress);
    }

    @Override
    public void onClick(View view) {


        String output = " Not Done";
        String responseMsg;
        switch (view.getId()) {
            case R.id.claimRelease:
                if (btnClaim.isChecked()) {
                    /**
                     * claim the task
                     **/
                    try {
                        output =
                                new getTaskDone()
                                        .execute(serverAddress+"/rest/task/" + taskObject.getTaskId() + "/claim")
                                        .get();
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                    }

                    responseMsg = setResponse(output);

                    if (responseMsg.equals("SUCCESS")) {

                        btnStart.setVisibility(View.VISIBLE);
                    }
                } else {
                    try {
                        /**
                         * release the task
                         **/
                        output =
                                new getTaskDone()
                                        .execute(serverAddress+"/rest/task/" + taskObject.getTaskId() + "/release")
                                        .get();


                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                    }
                    responseMsg = setResponse(output);

                    if (responseMsg.equals("SUCCESS")) {
                        btnStart.setVisibility(View.INVISIBLE);
                        btnComplete.setVisibility(View.INVISIBLE);
                        btnFail.setVisibility(View.INVISIBLE);
                    }

                }

                break;
            case R.id.taskStart:
                /**
                 * start the task
                 **/

                try {
                    output =
                            new getTaskDone()
                                    .execute(serverAddress+"/rest/task/" + taskObject.getTaskId() + "/start")
                                    .get();

                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }

                responseMsg = setResponse(output);

                if (responseMsg.equals("SUCCESS")) {
                    taskObject.setStatus("InProgress");
                    btnComplete.setVisibility(View.VISIBLE);
                    btnFail.setVisibility(View.VISIBLE);
                }

                break;
            case R.id.taskfail:
                /**
                 * fail the task
                 **/

                try {
                    output =
                            new getTaskDone()
                                    .execute(serverAddress+"/rest/task/" + taskObject.getTaskId() + "/fail")
                                    .get();

                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
                responseMsg = setResponse(output);


                finish();
                startActivity(intent);

                break;
            case R.id.taskComplete:
                /**
                 * complete the task
                 **/

                try {
                    output =
                            new getTaskDone()
                                    .execute(serverAddress+"/rest/task/" + taskObject.getTaskId() + "/complete?map_performance=" + responseData.getText().toString())
                                    .get();
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }


                responseMsg = setResponse(output);

                finish();
                startActivity(intent);

                break;
        }
    }

    /**
     * set the task response
     **/
    private String setResponse(String output) {
        String responseMsg = "";
        if (output.length() > 10) {
            responseMsg = output.substring(output.indexOf("<status>") + 8, output.indexOf("</status>"));
            Toast.makeText(getApplicationContext(),
                    "response is " + responseMsg, Toast.LENGTH_LONG)
                    .show();
        }
        return responseMsg;
    }

    /**
     * check whether network is available
     **/
    public boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }

    /**
     * do operations on task
     **/
    public class getTaskDone extends AsyncTask<String, Integer, String> {
        HttpURLConnection conn;
        URL url;
        InputStreamReader inputStreamReader;
        BufferedReader bufferedReader;

        getTaskDone() {

        }

        @Override
        protected String doInBackground(String... params) {
            String line = "";
            if (isNetworkAvailable()) {

                try {

                    url = new URL(params[0]);
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Authorization", authHeader);

                    inputStreamReader = new InputStreamReader((conn.getInputStream()));
                    bufferedReader = new BufferedReader(inputStreamReader);
                    line = bufferedReader.readLine();

                    conn.disconnect();
                    inputStreamReader.close();
                    bufferedReader.close();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else
                Toast.makeText(getApplicationContext(),"Network Connection is not Available",Toast.LENGTH_SHORT).show();
            return line;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
        }
    }
}


