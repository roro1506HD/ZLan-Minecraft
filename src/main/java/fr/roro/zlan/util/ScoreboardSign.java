package fr.roro.zlan.util;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import net.minecraft.server.v1_8_R3.*;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;

/**
 * @author zyuiop (Initial version, https://gist.github.com/zyuiop/8fcf2ca47794b92d7caa)
 * @author roro1506_HD (Modified version)
 */
public class ScoreboardSign {

    private final VirtualTeam[] lines   = new VirtualTeam[15];
    private final Player        player;
    private       boolean       created = false;
    private       String        objectiveName;

    /**
     * Create a scoreboard sign for a given player and using a specific objective name
     *
     * @param player the player viewing the scoreboard sign
     * @param objectiveName the name of the scoreboard sign (displayed at the top of the scoreboard)
     */
    public ScoreboardSign(Player player, String objectiveName) {
        this.player = player;
        this.objectiveName = objectiveName;
    }

    /**
     * Send the initial creation packets for this scoreboard sign. Must be called at least once.
     */
    public void create() {
        if (this.created)
            return;

        PlayerConnection connection = getPlayerConnection();

        connection.sendPacket(createObjectivePacket(0, this.objectiveName));
        connection.sendPacket(setObjectiveSlot());

        int i = 0;
        while (i < this.lines.length)
            sendLine(i++);

        this.created = true;
    }

    /**
     * Send the packets to remove this scoreboard sign. A destroyed scoreboard sign must be recreated using {@link
     * ScoreboardSign#create()} in order
     * to be used again
     */
    public void destroy() {
        if (!this.created)
            return;

        getPlayerConnection().sendPacket(createObjectivePacket(1, null));
        for (VirtualTeam team : this.lines)
            if (team != null)
                getPlayerConnection().sendPacket(team.removeTeam());

        this.created = false;
    }

    /**
     * Change the name of the objective. The name is displayed at the top of the scoreboard.
     *
     * @param name the name of the objective, max 32 char
     */
    public void setObjectiveName(String name) {
        this.objectiveName = name;
        if (this.created)
            getPlayerConnection().sendPacket(createObjectivePacket(2, name));
    }

    /**
     * Change a scoreboard line and send the packets to the player. Can be called async.
     *
     * @param line the number of the line (0 <= line < 15)
     * @param value the new value for the scoreboard line
     */
    public void setLine(int line, String value) {
        getOrCreateTeam(line).setValue(value);
        sendLine(line);
    }

    /**
     * Removes all the lines
     */
    public void clearLines() {
        IntStream.range(0, 15)
                .filter(this::hasLine)
                .forEach(this::removeLine);
    }

    /**
     * Get if this line exists
     *
     * @param line the line to check
     * @return if the line exists
     */
    public boolean hasLine(int line) {
        if (line > 14 || line < 0)
            return false;

        return this.lines[line] != null;
    }

    /**
     * Remove a given scoreboard line
     *
     * @param line the line to remove
     */
    public void removeLine(int line) {
        VirtualTeam team = getOrCreateTeam(line);
        String old = team.getCurrentPlayer();

        if (old != null && this.created) {
            getPlayerConnection().sendPacket(removeLine(old));
            getPlayerConnection().sendPacket(team.removeTeam());
        }

        this.lines[line] = null;
    }

    /**
     * Get the current value for a line
     *
     * @param line the line
     * @return the content of the line
     */
    public String getLine(int line) {
        if (line > 14 || line < 0)
            return null;
        return getOrCreateTeam(line).getValue();
    }

    /**
     * Get the team assigned to a line
     *
     * @return the {@link VirtualTeam} used to display this line
     */
    public VirtualTeam getTeam(int line) {
        if (line > 14 || line < 0)
            return null;
        return getOrCreateTeam(line);
    }

    private PlayerConnection getPlayerConnection() {
        return ((CraftPlayer) this.player).getHandle().playerConnection;
    }

    private void sendLine(int line) {
        if (line > 14 || line < 0)
            return;
        if (!this.created)
            return;

        VirtualTeam team = getOrCreateTeam(line);

        for (Packet packet : team.sendLine())
            getPlayerConnection().sendPacket(packet);

        getPlayerConnection().sendPacket(sendScore(team.getCurrentPlayer(), line));
        team.reset();
    }

    private VirtualTeam getOrCreateTeam(int line) {
        if (this.lines[line] == null)
            this.lines[line] = new VirtualTeam("__fakeScore" + line, line);

        return this.lines[line];
    }

    private PacketPlayOutScoreboardObjective createObjectivePacket(int mode, String displayName) {
        PacketPlayOutScoreboardObjective packet = new PacketPlayOutScoreboardObjective();

        setField(packet, "a", this.player.getName());
        setField(packet, "d", mode);

        if (mode == 0 || mode == 2) {
            setField(packet, "b", displayName);
            setField(packet, "c", IScoreboardCriteria.EnumScoreboardHealthDisplay.INTEGER);
        }

        return packet;
    }

    private PacketPlayOutScoreboardDisplayObjective setObjectiveSlot() {
        PacketPlayOutScoreboardDisplayObjective packet = new PacketPlayOutScoreboardDisplayObjective();

        setField(packet, "a", 1);
        setField(packet, "b", this.player.getName());

        return packet;
    }

    private PacketPlayOutScoreboardScore sendScore(String line, int score) {
        PacketPlayOutScoreboardScore packet = new PacketPlayOutScoreboardScore(line);

        setField(packet, "b", this.player.getName());
        setField(packet, "c", score);
        setField(packet, "d", PacketPlayOutScoreboardScore.EnumScoreboardAction.CHANGE);

        return packet;
    }

    private PacketPlayOutScoreboardScore removeLine(String line) {
        return new PacketPlayOutScoreboardScore(line);
    }

    private static void setField(Object edit, String fieldName, Object value) {
        try {
            Field field = edit.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(edit, value);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    enum Symbols {
        A(''),
        B(''),
        C(''),
        D(''),
        E(''),
        F(''),
        G(''),
        H(''),
        I(''),
        J(''),
        K(''),
        L(''),
        M(''),
        N(''),
        O('');

        char cha;

        Symbols(char cha) {
            this.cha = cha;
        }

        @Override
        public String toString() {
            return cha + "";
        }
    }

    /**
     * This class is used to manage the content of a line. Advanced users can use it as they want, but they are
     * encouraged to read and understand the
     * code before doing so. Use these methods at your own risk.
     */
    class VirtualTeam {

        private final String name;
        private       String prefix;
        private       String suffix;
        private       String currentPlayer;

        private boolean prefixChanged, suffixChanged = false;
        private boolean first = true;

        private int line;

        private VirtualTeam(String name, String prefix, String suffix, int line) {
            this.name = name;
            this.prefix = prefix;
            this.suffix = suffix;
            this.line = line;
            this.currentPlayer = Symbols.values()[line].toString();
        }

        private VirtualTeam(String name, int line) {
            this(name, "", "", line);
        }

        public String getName() {
            return this.name;
        }

        String getPrefix() {
            return this.prefix;
        }

        void setPrefix(String prefix) {
            if (this.prefix == null || !this.prefix.equals(prefix))
                this.prefixChanged = true;

            this.prefix = prefix;
        }

        public int getLine() {
            return this.line;
        }

        String getSuffix() {
            return this.suffix;
        }

        void setSuffix(String suffix) {
            if (this.suffix == null || !this.suffix.equals(this.prefix))
                this.suffixChanged = true;

            this.suffix = suffix;
        }

        private PacketPlayOutScoreboardTeam createPacket(int mode) {
            PacketPlayOutScoreboardTeam packet = new PacketPlayOutScoreboardTeam();
            setField(packet, "a", this.name);
            setField(packet, "h", mode);
            setField(packet, "b", "");
            setField(packet, "c", this.prefix);
            setField(packet, "d", this.suffix);
            setField(packet, "i", 0);
            setField(packet, "e", "always");
            setField(packet, "f", 0);

            return packet;
        }

        PacketPlayOutScoreboardTeam createTeam() {
            return createPacket(0);
        }

        PacketPlayOutScoreboardTeam updateTeam() {
            return createPacket(2);
        }

        PacketPlayOutScoreboardTeam removeTeam() {
            PacketPlayOutScoreboardTeam packet = new PacketPlayOutScoreboardTeam();
            setField(packet, "a", this.name);
            setField(packet, "h", 1);
            this.first = true;
            return packet;
        }

        Iterable<PacketPlayOutScoreboardTeam> sendLine() {
            List<PacketPlayOutScoreboardTeam> packets = new ArrayList<>();

            if (this.first)
                packets.add(createTeam());
            else if (this.prefixChanged || this.suffixChanged)
                packets.add(updateTeam());

            if (this.first)
                packets.add(changePlayer());

            if (this.first)
                this.first = false;

            return packets;
        }

        void reset() {
            this.prefixChanged = false;
            this.suffixChanged = false;
        }

        PacketPlayOutScoreboardTeam changePlayer() {
            return addOrRemovePlayer(3, this.currentPlayer);
        }

        @SuppressWarnings("unchecked")
        PacketPlayOutScoreboardTeam addOrRemovePlayer(int mode, String playerName) {
            PacketPlayOutScoreboardTeam packet = new PacketPlayOutScoreboardTeam();
            setField(packet, "a", this.name);
            setField(packet, "h", mode);

            try {
                Field f = packet.getClass().getDeclaredField("g");
                f.setAccessible(true);
                ((List<String>) f.get(packet)).add(playerName);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                e.printStackTrace();
            }

            return packet;
        }

        String getCurrentPlayer() {
            return this.currentPlayer;
        }

        String getValue() {
            return getPrefix() + getCurrentPlayer() + getSuffix();
        }

        void setValue(String value) {
            if (value.length() <= 16) {
                setPrefix(value);
                setSuffix("");
            } else if (value.length() <= 32) {
                String first = value.substring(0, 16);
                String second = value.substring(16);
                if (first.endsWith("§")) {
                    first = first.substring(0, 15);
                    second = "§" + second;
                }
                if (second.length() > 16)
                    second = second.substring(16, 32);
                setPrefix(first);
                setSuffix(second);
            } else {
                throw new IllegalArgumentException(
                        "Too long value ! Max 32 characters, value was " + value.length() + " !");
            }
        }
    }

}
