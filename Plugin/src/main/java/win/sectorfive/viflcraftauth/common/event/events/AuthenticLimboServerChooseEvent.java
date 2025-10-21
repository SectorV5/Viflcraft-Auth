/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package win.sectorfive.viflcraftauth.common.event.events;

import org.jetbrains.annotations.Nullable;
import win.sectorfive.viflcraftauth.api.ViflcraftAuthPlugin;
import win.sectorfive.viflcraftauth.api.database.User;
import win.sectorfive.viflcraftauth.api.event.events.LimboServerChooseEvent;
import win.sectorfive.viflcraftauth.common.event.AuthenticServerChooseEvent;

public class AuthenticLimboServerChooseEvent<P, S> extends AuthenticServerChooseEvent<P, S> implements LimboServerChooseEvent<P, S> {
    public AuthenticLimboServerChooseEvent(@Nullable User user, P player, ViflcraftAuthPlugin<P, S> plugin) {
        super(user, player, plugin);
    }
}
