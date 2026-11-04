package dev.sqwat.vela.rules;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/** The payload Vela reads on the velaclient:disable channel: a VarInt count, then VarInt-prefixed UTF-8 strings. */
final class DisablePacket {
    static final String CHANNEL = "velaclient:disable";
    /** Vela accepts at most this many ids of at most this many characters. */
    static final int MAX_IDS = 256, MAX_LENGTH = 64;

    private DisablePacket() {}

    static byte[] encode(List<String> ids) {
        if (ids.size() > MAX_IDS) throw new IllegalArgumentException("more than " + MAX_IDS + " ids");
        var out = new ByteArrayOutputStream();
        writeVarInt(out, ids.size());
        for (String id : ids) {
            if (id.length() > MAX_LENGTH) throw new IllegalArgumentException("id longer than " + MAX_LENGTH + ": " + id);
            byte[] bytes = id.getBytes(StandardCharsets.UTF_8);
            writeVarInt(out, bytes.length);
            out.writeBytes(bytes);
        }
        return out.toByteArray();
    }

    private static void writeVarInt(ByteArrayOutputStream out, int value) {
        while ((value & ~0x7F) != 0) {
            out.write((value & 0x7F) | 0x80);
            value >>>= 7;
        }
        out.write(value);
    }
}
