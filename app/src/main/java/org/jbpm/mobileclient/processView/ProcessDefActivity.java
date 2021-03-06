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
package org.jbpm.mobileclient.processView;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.jbpm.mobileclient.MenuActivity;
import org.jbpm.mobileclient.R;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;


public class ProcessDefActivity extends ListActivity implements View.OnClickListener {

    ProcessDefAdapter process_adapter;
    TextView t;
    String usrname,serverAddress="";
    String authHeader;
    Button menuButton;
    Intent mIntent;
    // declare class variables
    private ArrayList<ProcessObject> process_list = new ArrayList<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_process);

        /**
         * get user details, authentication header , server address from previous intent
         **/
        Intent intent = getIntent();
        usrname = intent.getExtras().getString("username");
        authHeader = intent.getExtras().getString("AuthHeader");
        serverAddress+=intent.getExtras().getString("ServerAddress");

        /**
         * set user details
         **/
        t = (TextView) findViewById(R.id.username);
        t.setText(usrname);

        /**
         * get process list from server
         **/
        GetProcessList getProcessList = new GetProcessList(usrname, authHeader);
        getProcessList.execute((Void) null);

        /**
         * set adaptor for details
         **/
        process_adapter = new ProcessDefAdapter(this, R.layout.list_process, process_list);
        setListAdapter(process_adapter);

        /**
         * set UI elements
         **/
        menuButton = (Button) findViewById(R.id.menubutton);
        menuButton.setOnClickListener(this);


    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {

        /**
         * get touched process details and send data
         **/
        ProcessObject processObject = process_list.get(position);

        mIntent = new Intent(this, ProcessDefViewActivity.class);
        Bundle mBundle = new Bundle();
        mBundle.putSerializable("processObject", processObject);
        mIntent.putExtra("username", usrname);
        mIntent.putExtra("AuthHeader", authHeader);
        mIntent.putExtra("ServerAddress",serverAddress);
        mIntent.putExtras(mBundle);
        startActivity(mIntent);
    }

    @Override
    public void onClick(View v) {
        /**
         * close button
         **/
        mIntent = new Intent(this, MenuActivity.class);
        mIntent.putExtra("username", usrname);
        mIntent.putExtra("AuthHeader", authHeader);
        mIntent.putExtra("ServerAddress",serverAddress);

        finish();
        startActivity(mIntent);
    }


    private class GetProcessList extends AsyncTask<Void, Void, Boolean> {
        private final String userName;
        private final String authHeader;

        HttpURLConnection conn;

        GetProcessList(String userNme, String authHeader) {
            userName = userNme;
            this.authHeader = authHeader;
        }

        @Override
        protected Boolean doInBackground(Void... params) {

            if (isNetworkAvailable()) {
                String response;
                URL url;
                try {
                    /**
                     * get process details from server by HttpURLConnection
                     **/
                    url = new URL(serverAddress+"/rest/deployment/processes?p=0&s=100");
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setRequestProperty("Authorization", authHeader);
                    InputStreamReader inputStreamReader = new InputStreamReader((conn.getInputStream()));
                    BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                    response = bufferedReader.readLine();
                    process_list = getProcessList(response);
                    conn.disconnect();
                    inputStreamReader.close();
                    bufferedReader.close();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(),
                            "Network Connection is not available ", Toast.LENGTH_LONG)
                            .show();
                }
            });

            if (process_list.isEmpty()) {

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(),
                                "Couldn't get the process Definitions list! Check your Connection ", Toast.LENGTH_LONG)
                                .show();
                    }
                });

            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    process_adapter = new ProcessDefAdapter(ProcessDefActivity.this, R.layout.custom_list_view, process_list);
                    // display the list.
                    setListAdapter(process_adapter);
                }
            });
            return true;
        }

        /**
         * check whether network available
         **/
        public boolean isNetworkAvailable() {
            ConnectivityManager cm = (ConnectivityManager)
                    getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = cm.getActiveNetworkInfo();
            return networkInfo != null && networkInfo.isConnected();
        }

        /**
         * get touched process details list from sever
         **/
        private ArrayList<ProcessObject> getProcessList(String response) {


            ArrayList<ProcessObject> process_list = new ArrayList<>();

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder;

            String processId="";
            String name="";
            String deployment="";
            String[] processVariablesArray = new String[0];
            String version="";


            try {
                builder = factory.newDocumentBuilder();

                Document document = builder.parse(new InputSource(new StringReader(
                        response)));

                NodeList processList = document.getElementsByTagName("process-definition");

                for (int i = 0; i < processList.getLength(); i++) {

                    NodeList childList = processList.item(i).getChildNodes();
                    for (int j = 0; j < childList.getLength(); j++) {
                        Node childNode = childList.item(j);
                        if ("id".equals(childNode.getNodeName())) {
                            processId=childNode.getTextContent()
                                    .trim();
                        }
                        if ("name".equals(childNode.getNodeName())) {
                            name=childNode.getTextContent()
                                    .trim();
                        }
                        if ("deployment-id".equals(childNode.getNodeName())) {
                            deployment=childNode.getTextContent()
                                    .trim();
                        }
                        if ("version".equals(childNode.getNodeName())) {
                            version=childNode.getTextContent()
                                    .trim();
                        }
                        if ("variables".equals(childNode.getNodeName())) {
                            NodeList Variables = childNode.getChildNodes();
                             processVariablesArray =new String[Variables.getLength()];
                            for (int k = 0; k < Variables.getLength(); k++) {
                                NodeList variables =Variables.item(k).getChildNodes();
                                processVariablesArray[k] =variables.item(0).getTextContent()+" - "+
                                        variables.item(1).getTextContent();
                            }
                        }
                    }
                    ProcessObject processObject = new ProcessObject(processId, name, deployment, processVariablesArray ,version);
                    processObject.setProcessSummery(childList.toString());
                    process_list.add(processObject);
                }

            } catch (ParserConfigurationException | IOException | SAXException e) {
                e.printStackTrace();
            }

            return process_list;
        }
    }


}




