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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author lstoll
 */
public class WowzaIceDirectAuthModule extends ModuleBase {

    static public void onConnect(IClient client, RequestFunction function,
            AMFDataList params) {
        getLogger().info("WowzaIceDirectAuthModule::onConnect: " + client.getClientId());
        WowzaIceAuthModuleUtils.storeCredentialsInClient(client, params);
    // TODO - process credentials here.
    }
    
    // TODO - finish implementation
}
