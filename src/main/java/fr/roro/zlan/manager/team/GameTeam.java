package fr.roro.zlan.manager.team;

import com.mojang.authlib.GameProfile;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * This file is a part of ZLAN project.
 *
 * @author roro1506_HD
 */
public class GameTeam {

    private final int           docLine;
    private final GameProfile[] members;

    public GameTeam(int docLine, GameProfile... members) {
        this.docLine = docLine;
        this.members = members;
    }

    public GameProfile[] getRawMembers() {
        return this.members;
    }

    public List<GameProfile> getMembers() {
        return Collections.unmodifiableList(Arrays.asList(this.members));
    }

    public boolean isSolo() {
        return (this.members[0] == null && this.members[1] != null) ||
                (this.members[0] != null && this.members[1] == null);
    }

    public boolean isEmpty() {
        return this.members[0] == null && this.members[1] == null;
    }

    public boolean contains(UUID uuid) {
        return Arrays.stream(this.members)
                .filter(Objects::nonNull)
                .map(GameProfile::getId)
                .anyMatch(tempUuid -> tempUuid.equals(uuid));
    }

    public int getDocLine() {
        return this.docLine;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(this.members);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        GameTeam gameTeam = (GameTeam) o;
        return Arrays.equals(this.members, gameTeam.members);
    }
}
