/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.lstoll.wowza.iceauth;

import com.wowza.wms.amf.*;
import com.wowza.wms.application.IApplicationInstance;
import com.wowza.wms.client.*;
import com.wowza.wms.module.*;
import com.wowza.wms.request.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author lstoll
 */
public abstract class WowzaIceAuthModuleUtils extends ModuleBase {

    protected static String USERNAME_PROPERTY = "wowzaiceauth_username";
    protected static String PASSWORD_PROPERTY = "wowzaiceauth_password";
    protected static String MOUNTPOINT_PROPERTY = "wowzaiceauth_mountpoint";

    public void onAppStart(IApplicationInstance appInstance) {
        getLogger().info("onAppStart: " + appInstance.getApplication().getName() + "/" + appInstance.getName());
    }

    static public void storeCredentialsInClient(IClient client, AMFDataList params) {
        // the credentials are passed in like this:
        // var nc:NetConnection = new NetConnection();
        // nc.connect(url, userName, password);
        // basically, the params are additional items on the connect call.

        // Set the provided username and password into the client.

        client.getProperties().setProperty(USERNAME_PROPERTY, getParamString(params, PARAM1));
        client.getProperties().setProperty(PASSWORD_PROPERTY, getParamString(params, PARAM2));

        getLogger().debug("WowzaIceAuthModuleUtils::storeCredentialsInClient provided username: " +
                client.getProperties().getPropertyStr(USERNAME_PROPERTY) +
                " provided password: " + client.getProperties().getPropertyStr(PASSWORD_PROPERTY));

    }

    static public void storeMountpointInClient(IClient client, String mountpoint) {
        client.getProperties().setProperty(MOUNTPOINT_PROPERTY, mountpoint);
    }

    public static String getServerName() {
        String server;
        try {
            server = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException ex) {
            getLogger().error("WowzaIceShoutcastAuthModule::play" + ex);
            server = "UNKNOWN";
        }
        return server;
    }

    /*
     * Not sure what these are for?
    
    static public void onConnectAccept(IClient client) {
    getLogger().info("onConnectAccept: " + client.getClientId());
    }
    
    static public void onConnectReject(IClient client) {
    getLogger().info("onConnectReject: " + client.getClientId());
    }*/    //static abstract void onDisconnect(IClient client);
    
    
    static public int postDataCheckResponse(String urlStr, String postData) {
        OutputStreamWriter wr = null;
        try {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("CONTENT-TYPE", "application/x-www-form-urlencoded");
            wr = new OutputStreamWriter(conn.getOutputStream());

            wr.write(postData);
            wr.flush();

            // Get the response
            //BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            
            return conn.getResponseCode();
        } catch (MalformedURLException ex) {
            getLogger().debug("WowzaIceShoutcastAuthModuleUtils::postDataCheckResponse", ex);
            return -1;
        } catch (IOException ex) {
            getLogger().debug("WowzaIceShoutcastAuthModuleUtils::postDataCheckResponse", ex);
            return -1;

        } finally {
            try {
                wr.close();
            } catch (IOException ex) {
                Logger.getLogger(WowzaIceAuthModuleUtils.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

    }
}
