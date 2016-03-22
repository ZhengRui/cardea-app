package com.example.jiayu.app_design2;

import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

/**
 * Created by Jiayu on 22/3/16.
 */
public class NetUtils {
    private static final String TAG = "NetUtils";

    /** Post
     *
     * @param key
     * @param value
     * @return
     */
    public static String updateOfPost(String key, String value) {
        HttpURLConnection conn = null;
        try {
            URL mURL = new URL("http://10.89.28.149");
            conn = (HttpURLConnection) mURL.openConnection();

            conn.setRequestMethod("POST");
            conn.setReadTimeout(5000);
            conn.setConnectTimeout(10000);
            conn.setDoOutput(true);

            // parameters for post request
            String data = "key=" + key + "&value=" + value;

            OutputStream out = conn.getOutputStream();
            out.write(data.getBytes());
            out.flush();
            out.close();

            int responseCode = conn.getResponseCode(); // if call this method, no need to call conn.connect()

            if (responseCode == 200) {
                InputStream is = conn.getInputStream();
                String state = getStringFromInputStream(is);

                return state;

            } else {
                Log.i(TAG, "http URL conntection failed...");
            }

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }

        return null;
    }

    /** Get
     *
     * @param value
     * @return
     */

    public static String uploadOfGet(String value) {
        HttpURLConnection conn = null;
        String data = "address=" + URLEncoder.encode(value) + "&sensor=false";
        String url  = "http://maps.google.com/maps/api/geocode/json?" + data;

        try {
            URL mURL = new URL(url);
            conn = (HttpURLConnection) mURL.openConnection();

            conn.setRequestMethod("GET");
            conn.setReadTimeout(5000);
            conn.setConnectTimeout(10000);

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                InputStream is = conn.getInputStream();
                String result = getStringFromInputStream(is);

                return result;
            } else {
                Log.i(TAG, "http URL connection failed..." );
            }

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }

        return null;
    }

    /** return string from inputstream
     *
     * @param is
     * @return
     * @throws IOException
     */
    private static String getStringFromInputStream(InputStream is) throws IOException{
        ByteArrayOutputStream os = new ByteArrayOutputStream();

        byte[] buffer = new byte[1024];
        int len = -1;

        while ((len = is.read(buffer)) != -1) {
            os.write(buffer, 0, len);
        }
        is.close();
        String result = os.toString();
        os.close();

        return result;

    }

}
