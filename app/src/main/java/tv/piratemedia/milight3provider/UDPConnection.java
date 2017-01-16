/*
*    Light Controller, to Control wifi LED Lighting
*    Copyright (C) 2014  Eliot Stocker
*
*    This program is free software: you can redistribute it and/or modify
*    it under the terms of the GNU General Public License as published by
*    the Free Software Foundation, either version 3 of the License, or
*    (at your option) any later version.
*
*    This program is distributed in the hope that it will be useful,
*    but WITHOUT ANY WARRANTY; without even the implied warranty of
*    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*    GNU General Public License for more details.
*
*    You should have received a copy of the GNU General Public License
*    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package tv.piratemedia.milight3provider;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public class UDPConnection {
    public static final int DISCOVERED_DEVICE = 10;
    public static final int COMMAND_SUCCESS = 20;
    public static final int LIST_WIFI_NETWORKS = 30;
    public static final int SIGNATURE = 40;

    public static String CONTROLLERIP = "";
    public static int CONTROLLERPORT = 0;
    public static int CONTROLLERADMINPORT = 48899;
    private utils Utils;
    private static UDP_Server server = null;
    private SharedPreferences prefs;
    private static List<Handler> handlers = new ArrayList<>();
    private String NetworkBroadCast;
    private Context mCtx;

    private boolean onlineMode = false;

    private static UDPConnection instance = null;

    public UDPConnection(Context context, Handler handler) {
        mCtx = context;
        handlers.add(handler);
        Utils = new utils(context);
        prefs = PreferenceManager.getDefaultSharedPreferences(context);

        server = new UDP_Server();
        server.runUdpServer();
    }

    public static UDPConnection getInstance(Context context, Handler handler) {
        if(instance == null) {
            instance = new UDPConnection(context, handler);
        } else {
            handlers.add(handler);
        }
        return instance;
    }

    public void setOnlineMode(boolean online) {
        onlineMode = online;
    }

    public void sendMessage(byte[] Bytes) {
        sendMessage(Bytes, false, CONTROLLERADMINPORT);
    }
    public void sendMessage(byte[] Bytes, final Boolean Device) {
        sendMessage(Bytes, Device, CONTROLLERADMINPORT);
    }

    public void sendMessage(final byte[] Bytes, final Boolean Device, final int port) {
        if(server == null) {
            Log.e("UDPC", "Server is not initialised!");
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                String NetworkBroadCast = null;
                if (Device) {
                    CONTROLLERIP = prefs.getString("pref_light_controller_ip", "192.168.0.255");
                    NetworkBroadCast = CONTROLLERIP;
                } else {
                    try {
                        NetworkBroadCast = Utils.getWifiIP(utils.BROADCAST_ADDRESS);
                    } catch (ConnectionException e) {
                        e.printStackTrace();
                        return;
                    }
                }
                Log.d("UDPC", "Sending command to: "+NetworkBroadCast);
                try {
                    InetAddress controller = InetAddress.getByName(NetworkBroadCast);
                    server.SendMessage(Bytes, controller, port);
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public static String byteArrayToHex(byte[] a) {
        StringBuilder sb = new StringBuilder(a.length * 2);
        for(byte b: a)
            sb.append(String.format("%02x", b));
        return sb.toString();
    }

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len/2];
        for(int i = 0; i < len; i+=2){
            data[i/2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    private void sendMessageToHandlers(Message m) {
        for(Handler h : handlers) {
            Message send = new Message();
            send.copyFrom(m);
            h.sendMessage(send);
        }
        m.recycle();
    }

    class UDP_Server {
        private AsyncTask<Void, Void, Void> async;
        public boolean Server_aktiv = true;
        private DatagramSocket socket = null;
        private UDP_Server srv = this;

        @SuppressLint("NewApi")
        void runUdpServer() {
            Server_aktiv = true;
            async = new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    byte[] lMsg = new byte[1024];
                    DatagramPacket dp = new DatagramPacket(lMsg, lMsg.length);
                    DatagramSocket ds = null;

                    try {
                        srv.socket = new DatagramSocket();
                        srv.socket.setSoTimeout(1000);
                        while (Server_aktiv) {
                            try {
                                srv.socket.receive(dp);
                                byte[] data = dp.getData();
                                String Data = new String(data);
                                Log.d("Received", byteArrayToHex(data));
                                if(data[0] == 0x28 && data[1] == 0x00 && data[2] == 0x00
                                        && data[3] == 0x00 && data[4] == 0x11
                                        && data[5] == 0x00 && data[6] == 0x02) {
                                    byte[] sig = new byte[] {data[19], data[20]};
                                    Message m = new Message();
                                    m.what = SIGNATURE;
                                    m.obj = sig;
                                    sendMessageToHandlers(m);
                                } else {
                                    if (Data.startsWith("+ok")) {
                                        if (Data.startsWith("+ok=")) {
                                            Message m = new Message();
                                            m.what = LIST_WIFI_NETWORKS;
                                            m.obj = Data;
                                            sendMessageToHandlers(m);
                                        } else {
                                            Message m = new Message();
                                            m.what = COMMAND_SUCCESS;
                                            sendMessageToHandlers(m);
                                        }
                                    } else {
                                        String[] parts = Data.split(",");
                                        if (parts.length > 1) {
                                            if (Utils.validIP(parts[0]) && Utils.validMac(parts[1])) {
                                                Message m = new Message();
                                                m.what = DISCOVERED_DEVICE;
                                                m.obj = parts;
                                                sendMessageToHandlers(m);
                                            }
                                        }
                                    }
                                }
                            } catch(SocketTimeoutException e) {
                                //no problem
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        if (socket != null) {
                            Log.d("Close Socket", "Port: "+socket.getLocalPort());
                            socket.close();
                        }
                    }

                    return null;
                }
            };

            if (Build.VERSION.SDK_INT >= 11)
                async.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            else async.execute();
        }

        void SendMessage(byte[] data, InetAddress host, int port) {
            if(this.socket == null) {
                Log.e("UDPC", "Socket is null :(");
                return;
            }
            DatagramPacket p = new DatagramPacket(data, data.length, host, port);
            try {
                this.socket.send(p);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void destroy() {
            Server_aktiv = false;
        }
    }
}
