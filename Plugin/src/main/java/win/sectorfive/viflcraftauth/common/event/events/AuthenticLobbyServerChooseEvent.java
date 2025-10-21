/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package win.sectorfive.viflcraftauth.common.event.events;

import org.jetbrains.annotations.Nullable;
import win.sectorfive.viflcraftauth.api.ViflcraftAuthPlugin;
import win.sectorfive.viflcraftauth.api.database.User;
import win.sectorfive.viflcraftauth.api.event.events.LobbyServerChooseEvent;
import win.sectorfive.viflcraftauth.common.event.AuthenticServerChooseEvent;

public class AuthenticLobbyServerChooseEvent<P, S> extends AuthenticServerChooseEvent<P, S> implements LobbyServerChooseEvent<P, S> {

    private final Boolean fallback;
    private boolean cancelled = false;

    public AuthenticLobbyServerChooseEvent(@Nullable User user, P player, ViflcraftAuthPlugin<P, S> plugin, Boolean fallback) {
        super(user, player, plugin);

        this.fallback = fallback;
    }

    @Override
    public Boolean isFallback() {
        return fallback;
    }


    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }
}
