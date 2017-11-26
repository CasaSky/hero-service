package de.haw.heroservice;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;

public class ApacheClient {

    private HttpClient httpClient = HttpClientBuilder.create().build();

    private HttpPost httpPost;
    private HttpGet httpGet;

    private final Header applicationHeader = new BasicHeader("content-type", "application/json");
    private final String AUTHORIZATION = "Authorization";

    public HttpResponse post(String url, String loginToken, String request) throws Exception {

        httpPost = new HttpPost(url);
        httpPost.addHeader(applicationHeader);
        if (loginToken!=null) {
            httpPost.addHeader(AUTHORIZATION, "Token {" + loginToken + "}");
        }

        try {

            httpPost.setEntity(new StringEntity("{" + request + "}"));

            return  httpClient.execute(httpPost);
        } catch (Exception e) {

            try {
                throw e;
            } catch (IOException e1) {
                throw e1;
            } catch (JSONException e2) {
                throw e2;
            }
        }
    }

    public String get(String url, String loginToken, String responseAttribute) throws Exception {

        httpGet = new HttpGet(url);
        httpGet.addHeader(applicationHeader);
        if (loginToken != null) {
            httpGet.addHeader(AUTHORIZATION, "Token {" + loginToken + "}");
        }

        try {

            HttpResponse response = httpClient.execute(httpGet);
            String json = EntityUtils.toString(response.getEntity());

            JSONObject jsonObject = new JSONObject(json);
            return jsonObject.getString(responseAttribute);
        } catch (Exception e) {

            try {
                throw e;
            } catch (IOException e1) {
                throw e1;
            } catch (JSONException e2) {
                throw e2;
            }
        }
    }

    public List<Object> getArray(String url, String loginToken, String responseAttribute) throws Exception {

        httpGet = new HttpGet(url);
        httpGet.addHeader(applicationHeader);
        if (loginToken != null) {
            httpGet.addHeader(AUTHORIZATION, "Token {" + loginToken + "}");
        }

        try {

            HttpResponse response = httpClient.execute(httpGet);
            String json = EntityUtils.toString(response.getEntity());

            JSONObject jsonObject = new JSONObject(json);
            return jsonObject.getJSONArray(responseAttribute).toList();
        } catch (Exception e) {

            try {
                throw e;
            } catch (IOException e1) {
                throw e1;
            } catch (JSONException e2) {
                throw e2;
            }
        }
    }
}