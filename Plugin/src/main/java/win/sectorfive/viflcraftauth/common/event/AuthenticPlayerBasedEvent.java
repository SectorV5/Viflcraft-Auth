/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package win.sectorfive.viflcraftauth.common.event;

import net.kyori.adventure.audience.Audience;
import org.jetbrains.annotations.Nullable;
import win.sectorfive.viflcraftauth.api.LibreLoginPlugin;
import win.sectorfive.viflcraftauth.api.PlatformHandle;
import win.sectorfive.viflcraftauth.api.database.User;
import win.sectorfive.viflcraftauth.api.event.PlayerBasedEvent;

import java.util.UUID;

public class AuthenticPlayerBasedEvent<P, S> implements PlayerBasedEvent<P, S> {

    private final User user;
    private final Audience audience;
    private final UUID uuid;
    private final P player;
    private final LibreLoginPlugin<P, S> plugin;
    private final PlatformHandle<P, S> platformHandle;

    public AuthenticPlayerBasedEvent(@Nullable User user, @Nullable P player, LibreLoginPlugin<P, S> plugin) {
        this.plugin = plugin;
        this.platformHandle = plugin.getPlatformHandle();
        this.user = user;
        this.audience = player == null ? Audience.empty() : platformHandle.getAudienceForPlayer(player);
        this.uuid = player == null ? null : platformHandle.getUUIDForPlayer(player);
        this.player = player;
    }

    @Override
    public User getUser() {
        return user;
    }

    @Override
    public UUID getUUID() {
        return uuid;
    }

    @Override
    public Audience getAudience() {
        return audience;
    }

    @Override
    public P getPlayer() {
        return player;
    }

    @Override
    public LibreLoginPlugin<P, S> getPlugin() {
        return plugin;
    }

    @Override
    public PlatformHandle<P, S> getPlatformHandle() {
        return platformHandle;
    }
}
