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
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author lstoll
 */
public class WowzaIceShoutcastAuthModule extends ModuleBase {

    /**
     * Use this method to get the credentials off the client.
     * 
     * @param client
     * @param function
     * @param params
     */
    static public void onConnect(IClient client, RequestFunction function,
            AMFDataList params) {
        getLogger().info("WowzaIceShoutcastAuthModule::onConnect: " + client.getClientId());
        WowzaIceAuthModuleUtils.storeCredentialsInClient(client, params);
        // we process the auth for here when it's played. So just accept for now.
        client.acceptConnection();
    }

    /**
     * This method is where the actual shoutcast url is passed in - this is the point
     * where we retrieve it, parse out the mount point and authenticate.
     * 
     * @param client
     * @param function
     * @param params
     */
    public void play(IClient client, RequestFunction function, AMFDataList params) {
//        if (params.get(PARAM1).getType() == AMFData.DATA_TYPE_STRING) {
//            String playName = params.getString(PARAM1);
//            params.set(PARAM1, new AMFDataItem(playName + "_newname"));
//        }
        getLogger().info("play: " + client.getClientId());
        if (params.get(PARAM1).getType() == AMFData.DATA_TYPE_STRING) {
            String playName = params.getString(PARAM1);
            getLogger().info("play: PLAYNAME(sent in) = " + playName);
            // looks like this - get the mountpoint shoutcast:http://66.220.31.136/aac
            String mountPoint = playName.substring(playName.lastIndexOf("/") + 1);
            getLogger().debug("WowzaIceShoutcastAuthModule::play MountPoint = " + mountPoint);

            //{ :action => "listener_add", :server => "wowzaserver", :port => "8000", :client => "sessionidone",
            //  :mount => "mountpoint", :user => "lstoll", :pass => @working_hash, :ip => "127.0.0.1", :agent => "RSPEC"}

            String userAgent = client.getFlashVer();
            String server = WowzaIceAuthModuleUtils.getServerName();
            String port = "1935"; // TODO - work out how to look this up.
            String clientIdentifier = String.valueOf(client.getClientId());
            String username = client.getProperties().getPropertyStr(WowzaIceAuthModuleUtils.USERNAME_PROPERTY);
            String password = client.getProperties().getPropertyStr(WowzaIceAuthModuleUtils.PASSWORD_PROPERTY);
            String ip = client.getIp();

            // save mountpoint for later
            WowzaIceAuthModuleUtils.storeMountpointInClient(client, mountPoint);

            String postdata;
            try {
                postdata = "action=listener_add&server=" + URLEncoder.encode(server, "UTF-8") +
                        "&port=" + URLEncoder.encode(port, "UTF-8") + "&client=" +
                        URLEncoder.encode(clientIdentifier, "UTF-8") + "&mount=" +
                        URLEncoder.encode(mountPoint, "UTF-8") + "&user=" +
                        URLEncoder.encode(username, "UTF-8") + "&pass=" +
                        URLEncoder.encode(password, "UTF-8") + "&ip=" +
                        URLEncoder.encode(ip, "UTF-8") + "&agent=" +
                        URLEncoder.encode(userAgent, "UTF-8");
            } catch (UnsupportedEncodingException ex) {
                getLogger().debug("WowzaIceShoutcastAuthModule::play - URL encoding", ex);
                postdata = "";
            }

            getLogger().debug("WowzaIceShoutcastAuthModule::play postdata = \"" + postdata + "\"");
            
            getLogger().debug("WowzaIceShoutcastAuthModule::play URL loaded is " +
                    getAppInstance(client).getProperties().getPropertyStr("WowzaIceAuth_StartSessionEndpoint") +
                    " application name: " + getAppInstance(client).getName());

            // here's where we will do the post, and make the descision
            int responsecode = WowzaIceAuthModuleUtils.postDataCheckResponse(
                    getAppInstance(client).getProperties().getPropertyStr("WowzaIceAuth_StartSessionEndpoint"), postdata);

            if (responsecode == 200) {
                getLogger().debug("WowzaIceShoutcastAuthModule::play response code 200, starting");
                // this starts the stream
                com.wowza.wms.module.ModuleCore.play(client, function, params);
            } else {
                getLogger().debug("WowzaIceShoutcastAuthModule::play non-200 response code returned. exact code: " + responsecode);
                client.rejectConnection();
                return; // the reject doesn't seem to do anything - this will ensure play is never called.
            }
        } else {
            getLogger().info("WowzaIceShoutcastAuthModule::play rejecting client " + client.getClientId() +
                    " because no shoutcast URL was passed in");
            client.rejectConnection();
            return; // the reject doesn't seem to do anything - this will ensure play is never called.  
        }
    }

    /**
     * Use this method to close off the session
     * 
     * @param client
     */
    static public void onDisconnect(IClient client) {
        // this is where we'd send the kill stuff

        // action=listener_remove&server=myserver.com&port=8000&client=1&mount=/live&user=&pass=&duration=3600

        getLogger().info("onDisconnect: " + client.getClientId());

        String mountPoint = client.getProperties().getPropertyStr(WowzaIceAuthModuleUtils.MOUNTPOINT_PROPERTY);
        String server = WowzaIceAuthModuleUtils.getServerName();
        String port = "1935";
        String clientIdentifier = String.valueOf(client.getClientId());
        String duration = String.valueOf((long) client.getTimeRunningSeconds()); // connect time to seconds.

        String postdata;
        try {
            postdata = "action=listener_remove&server=" + URLEncoder.encode(server, "UTF-8") +
                    "&port=" + URLEncoder.encode(port, "UTF-8") + "&client=" +
                    URLEncoder.encode(clientIdentifier, "UTF-8") + "&mount=" +
                    URLEncoder.encode(mountPoint, "UTF-8") + "&user=&pass=&duration=" +
                    URLEncoder.encode(duration, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            getLogger().debug("WowzaIceShoutcastAuthModule::play - URL encoding", ex);
            postdata = "";
        }

        getLogger().debug("WowzaIceShoutcastAuthModule::onDisconnect postdata = \"" + postdata + "\"");
        // TODO - call the end.

        int responsecode = WowzaIceAuthModuleUtils.postDataCheckResponse(
                getAppInstance(client).getProperties().getPropertyStr("WowzaIceAuth_EndSessionEndpoint"), postdata);
        if (responsecode != 200) {
            getLogger().error("WowzaIceShoutcastAuthModule::onDisconnect Non-200 code returned on ending session, postdata::" + postdata + "::");
        }
    }
}
