package tv.piratemedia.milight3provider;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.UnsupportedEncodingException;

import tv.piratemedia.lightcontroler.api.ControlProviderReciever;
import tv.piratemedia.lightcontroler.api.appPermission;

import static tv.piratemedia.milight3provider.UDPConnection.byteArrayToHex;
import static tv.piratemedia.milight3provider.UDPConnection.hexStringToByteArray;

/**
 * Created by eliotstocker on 14/01/2017.
 */

public class Provider extends ControlProviderReciever {
    private static appPermission[] ap = {new appPermission("tv.piratemedia.lightcontroler", -448508482)};

    private UDPConnection connection = null;
    private Handler handler = null;
    private String DiscoveredIP = null;
    private String DiscoveredMac = null;
    private SharedPreferences prefs;
    private byte[] deviceSig = null;

    private int SequenceNumberIndex = 0;

    public Provider() {
        super(ap);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if(DiscoveredIP == null) {
            DiscoveredIP = prefs.getString("pref_light_controller_ip", null);
            Log.d("IP", DiscoveredIP);
        }
        if(DiscoveredMac == null) {
            DiscoveredMac = prefs.getString("pref_light_controller_mac", null);
        }
        if(deviceSig == null) {
            String deviceSigString = prefs.getString("pref_light_controller_sig", null);
            if(deviceSigString != null) {
                deviceSig = hexStringToByteArray(deviceSigString);
            }
        }
        SequenceNumberIndex = prefs.getInt("pref_light_controller_seq", 0);

        if(handler == null) {
            handler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    super.handleMessage(msg);
                    switch(msg.what) {
                        case UDPConnection.DISCOVERED_DEVICE:
                            String[] ipMac = (String[])msg.obj;
                            DiscoveredIP = ipMac[0];
                            DiscoveredMac = ipMac[1];

                            prefs.edit().putString("pref_light_controller_ip", DiscoveredIP).commit();
                            prefs.edit().putString("pref_light_controller_mac", DiscoveredMac).commit();

                            getSessionID();
                            break;
                        case UDPConnection.SIGNATURE:
                            deviceSig = (byte[])msg.obj;
                            prefs.edit().putString("pref_light_controller_sig", byteArrayToHex(deviceSig)).commit();
                            prefs.edit().putInt("pref_light_controller_seq", 0).commit();
                            SequenceNumberIndex = 0;
                            Log.d("Provider", "Got Session ID:" + byteArrayToHex(deviceSig));
                            break;
                    }
                }
            };
        }
        if(connection == null) {
            connection = UDPConnection.getInstance(context, handler);
            /*if(DiscoveredIP != null) {
                getSessionID();
            }*/
        }
        super.onReceive(context, intent);
    }

    public void getSessionID() {
        byte[] bytes = new byte[]{
                0x20,
                0x00,
                0x00,
                0x00,
                0x16,
                0x02,
                0x62,
                0x3A,
                (byte)0xD5,
                (byte)0xED,
                (byte)0xA3,
                0x01,
                (byte)0xAE,
                0x08,
                0x2D,
                0x46,
                0x61,
                0x41,
                (byte)0xA7,
                (byte)0xF6,
                (byte)0xDC,
                (byte)0xAF,
                (byte)0xD3,
                (byte)0xE6,
                0x00,
                0x00,
                0x1E
        };
        connection.sendMessage(bytes, true, 5987);
    }

    public byte[] constructCommand(byte[] command, int zone) {
        byte zoneByte = 0x00;
        switch(zone) {
            case 0: zoneByte = 0x00; break;
            case 1: zoneByte = 0x01; break;
            case 2: zoneByte = 0x02; break;
            case 3: zoneByte = 0x03; break;
            case 4: zoneByte = 0x04; break;
        }

        if(deviceSig == null) {
            Log.d("Provider", "We have no signature :(");
            return null;
        }

        byte[] header = new byte[] {
                (byte)0x80,
                0x00,
                0x00,
                0x00,
                0x11,
                deviceSig[0],
                deviceSig[1],
                0x00,
                (byte)SequenceNumberIndex,
                0x00
        };

        byte[] zoneInfo = new byte[] {
                zoneByte,
                0x00
        };

        int checksum = 0;
        for (byte aCommand : command) {
            checksum += aCommand;
        }
        for (byte aZoneInfo : zoneInfo) {
            checksum += aZoneInfo;
        }


        Log.d("provider", "Checksum value: "+checksum+", hex: "+byteArrayToHex(new byte[] {(byte)checksum}));

        byte[] c = concatenateByteArrays(header, command);
        c = concatenateByteArrays(c, zoneInfo);
        c = concatenateByteArrays(c, new byte[] {(byte)checksum});

        SequenceNumberIndex ++;
        if(SequenceNumberIndex > 255) {
            SequenceNumberIndex = 0;
        }
        prefs.edit().putInt("pref_light_controller_seq", SequenceNumberIndex).commit();

        return c;
    }

    byte[] concatenateByteArrays(byte[] a, byte[] b) {
        byte[] result = new byte[a.length + b.length];
        System.arraycopy(a, 0, result, 0, a.length);
        System.arraycopy(b, 0, result, a.length, b.length);
        return result;
    }

    @Override
    public void onDiscovery(Context context) {
        Log.d("Provider", "Starting Discovery of Provider 3.0");
        try {
            connection.sendMessage("HF-A11ASSISTHREAD".getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onLightsOn(int Type, int Zone, Context context) {
        if(Type == ControlProviderReciever.ZONE_TYPE_COLOR) {
            try {
                byte[] data = constructCommand(RGBWCommands.ON.commandBytes(), Zone);
                connection.sendMessage(data, true, 5987);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if(Type == ControlProviderReciever.ZONE_TYPE_WHITE) {
            try {
                byte[] data = constructCommand(WhiteCommands.ON.commandBytes(), Zone);
                connection.sendMessage(data, true, 5987);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onLightsOff(int Type, int Zone, Context context) {
        if(Type == ControlProviderReciever.ZONE_TYPE_COLOR) {
            try {
                byte[] data = constructCommand(RGBWCommands.OFF.commandBytes(), Zone);
                connection.sendMessage(data, true, 5987);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if(Type == ControlProviderReciever.ZONE_TYPE_WHITE) {
            try {
                byte[] data = constructCommand(WhiteCommands.OFF.commandBytes(), Zone);
                connection.sendMessage(data, true, 5987);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onGlobalOn(Context context) {
        try {
            byte[] data = constructCommand(RGBWCommands.ON.commandBytes(), 0);
            connection.sendMessage(data, true, 5987);
        } catch(Exception e) {
            e.printStackTrace();
        }

        try {
            byte[] data = constructCommand(WhiteCommands.ON.commandBytes(), 0);
            connection.sendMessage(data, true, 5987);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onGlobalOff(Context context) {
        try {
            byte[] data = constructCommand(RGBWCommands.OFF.commandBytes(), 0);
            connection.sendMessage(data, true, 5987);
        } catch(Exception e) {
            e.printStackTrace();
        }

        try {
            byte[] data = constructCommand(WhiteCommands.OFF.commandBytes(), 0);
            connection.sendMessage(data, true, 5987);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onSetBrightness(int Type, int Zone, float Brightness, Context context) {
        if(Type == ControlProviderReciever.ZONE_TYPE_WHITE) {
            Log.e("MiLight3.0Provider", "Attempt to control non stateful lights with stateful command");
        } else {
            int val = Math.round(100f * Brightness);
            try {
                byte[] data = constructCommand(RGBWCommands.BRIGHTNESS.processComand((byte) val), Zone);
                connection.sendMessage(data, true, 5987);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onIncreaseBrightness(int Type, int Zone, Context context) {
        if(Type == ControlProviderReciever.ZONE_TYPE_COLOR) {
            Log.e("MiLight3.0Provider", "Attempt to control stateful lights with non stateful command");
        } else {
            try {
                byte[] data = constructCommand(WhiteCommands.BRIGHTNESSUP.commandBytes(), Zone);
                connection.sendMessage(data, true, 5987);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onDecreaseBrightness(int Type, int Zone, Context context) {
        if(Type == ControlProviderReciever.ZONE_TYPE_COLOR) {
            Log.e("MiLight3.0Provider", "Attempt to control stateful lights with non stateful command");
        } else {
            try {
                byte[] data = constructCommand(WhiteCommands.BRIGHTNESSDOWN.commandBytes(), Zone);
                connection.sendMessage(data, true, 5987);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onSetColor(int Zone, int color, Context context) {
        float[] colors = new float[3];
        Color.colorToHSV(color, colors);
        Float deg = (float) Math.toRadians(-colors[0]);
        Float dec = (deg/((float)Math.PI*2f))*255f;
        //rotation compensation
        dec -= 10;
        if(dec > 255) {
            dec = dec - 255;
        }
        if(dec < 0) {
            dec = dec + 255;
        }
        dec = 255 - dec;
        try {
            byte[] data = constructCommand(RGBWCommands.COLOR.processComand((byte)dec.intValue(), (byte)dec.intValue(), (byte)dec.intValue(), (byte)dec.intValue()), Zone);
            connection.sendMessage(data, true, 5987);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onSetTemperature(int Type, int Zone, float Temp, Context context) {
        Log.d("MiLight3.0Provider", "not implemented in this provider, should never be called");
    }

    @Override
    public void onIncreaseTemperature(int Type, int Zone, Context context) {
        if(Type == ControlProviderReciever.ZONE_TYPE_COLOR) {
            Log.e("MiLight3.0Provider", "Attempt to control stateful lights with non stateful command");
        } else {
            try {
                byte[] data = constructCommand(WhiteCommands.TEMPUP.commandBytes(), Zone);
                connection.sendMessage(data, true, 5987);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onDecreaseTemperature(int Type, int Zone, Context context) {
        if(Type == ControlProviderReciever.ZONE_TYPE_COLOR) {
            Log.e("MiLight3.0Provider", "Attempt to control stateful lights with non stateful command");
        } else {
            try {
                byte[] data = constructCommand(WhiteCommands.TEMPDOWN.commandBytes(), Zone);
                connection.sendMessage(data, true, 5987);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onSetNight(int Type, int Zone, Context context) {
        if(Type == ControlProviderReciever.ZONE_TYPE_COLOR) {
            try {
                byte[] data = constructCommand(RGBWCommands.NIGHT.commandBytes(), Zone);
                connection.sendMessage(data, true, 5987);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if(Type == ControlProviderReciever.ZONE_TYPE_WHITE) {
            try {
                byte[] data = constructCommand(WhiteCommands.NIGHT.commandBytes(), Zone);
                connection.sendMessage(data, true, 5987);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onSetFull(int Type, int Zone, Context context) {
        if(Type == ControlProviderReciever.ZONE_TYPE_COLOR) {
            try {
                byte[] data = constructCommand(RGBWCommands.BRIGHTNESS.processComand((byte) 100), Zone);
                connection.sendMessage(data, true, 5987);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if(Type == ControlProviderReciever.ZONE_TYPE_WHITE) {
            try {
                byte[] data = constructCommand(WhiteCommands.NIGHT.commandBytes(), Zone);
                connection.sendMessage(data, true, 5987);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onSetWhite(int Zone, Context context) {
        try {
            byte[] data = constructCommand(RGBWCommands.WHITE.commandBytes(), Zone);
            connection.sendMessage(data, true, 5987);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}
