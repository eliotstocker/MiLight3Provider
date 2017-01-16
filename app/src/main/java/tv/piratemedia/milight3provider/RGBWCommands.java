package tv.piratemedia.milight3provider;

/**
 * Created by eliotstocker on 15/01/2017.
 */

public enum RGBWCommands {
    ON(new         byte[] {0x31, 0, 0, 0x07, 0x03, 0x01, 0, 0, 0}),
    OFF(new        byte[] {0x31, 0, 0, 0x07, 0x03, 0x02, 0, 0, 0}),
    NIGHT(new      byte[] {0x31, 0, 0, 0x07, 0x03, 0x06, 0, 0, 0}),
    WHITE(new      byte[] {0x31, 0, 0, 0x07, 0x03, 0x05, 0, 0, 0}),
    BRIGHTNESS(new byte[] {0x31, 0, 0, 0x07, 0x02,    0, 0, 0, 0}),
    COLOR(new      byte[] {0x31, 0, 0, 0x07, 0x01,    0, 0, 0, 0}),
    MODE(new       byte[] {0x31, 0, 0, 0x07, 0x04,    0, 0, 0, 0}),
    SPEEDUP(new    byte[] {0x31, 0, 0, 0x07, 0x03, 0x03, 0, 0, 0}),
    SPEEDDOWN(new  byte[] {0x31, 0, 0, 0x07, 0x03, 0x04, 0, 0, 0}),
    SYNC(new       byte[] {0x3D, 0, 0, 0x07,    0,    0, 0, 0, 0}),
    UNSYNC(new     byte[] {0x3E, 0, 0, 0x07,    0,    0, 0, 0, 0});

    private byte[] data;

    RGBWCommands(byte[] data) {
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

    public byte[] processComand(byte val1, byte val2, byte val3, byte val4) {
        byte[] array = this.data;
        array[5] = val1;
        array[6] = val2;
        array[7] = val3;
        array[8] = val4;

        return array;
    }
}
