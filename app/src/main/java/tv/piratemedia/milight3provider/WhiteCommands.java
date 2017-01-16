package tv.piratemedia.milight3provider;

/**
 * Created by eliotstocker on 15/01/2017.
 */

public enum WhiteCommands {
    ON(new             byte[] {0x31, 0, 0, 0x01, 0x01, 0x07, 0, 0, 0}),
    OFF(new            byte[] {0x31, 0, 0, 0x01, 0x01, 0x08, 0, 0, 0}),
    NIGHT(new          byte[] {0x31, 0, 0, 0x01, 0x01, 0x06, 0, 0, 0}),
    FULL(new           byte[] {0x31, 0, 0, 0x01, (byte)0x81, 0x07, 0, 0, 0}),
    BRIGHTNESSUP(new   byte[] {0x31, 0, 0, 0x01, 0x01, 0x01, 0, 0, 0}),
    BRIGHTNESSDOWN(new byte[] {0x31, 0, 0, 0x01, 0x01, 0x02, 0, 0, 0}),
    TEMPUP(new         byte[] {0x31, 0, 0, 0x01, 0x01, 0x03, 0, 0, 0}),
    TEMPDOWN(new       byte[] {0x31, 0, 0, 0x01, 0x01, 0x04, 0, 0, 0}),
    SYNC(new           byte[] {0x3D, 0, 0, 0x01,    0,    0, 0, 0, 0}),
    UNSYNC(new         byte[] {0x3E, 0, 0, 0x01,    0,    0, 0, 0, 0});

    private byte[] data;

    WhiteCommands(byte[] data) {
        this.data = data;
    }

    public byte[] commandBytes() {
        return this.data;
    }

    public byte[] processComand(byte val) {
        byte[] array = this.data;
        array[5] = val;

        return array;
    }
}
