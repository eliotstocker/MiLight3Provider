package tv.piratemedia.milight3provider;

/**
 * Created by eliotstocker on 15/01/2017.
 */

public enum RGBWCommands {
    ON(new byte[] {0x31, 0, 0, 0x07, 0x03, 0x01, 0, 0, 0}),
    OFF(new byte[] {0x31, 0, 0, 0x07, 0x03, 0x02, 0, 0, 0});

    private byte[] data;

    RGBWCommands(byte[] data) {
        this.data = data;
    }

    public byte[] commandBytes() {
        return this.data;
    }
}
