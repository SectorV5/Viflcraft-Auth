/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package win.sectorfive.viflcraftauth.common.event.events;

import org.jetbrains.annotations.Nullable;
import win.sectorfive.viflcraftauth.api.LibreLoginPlugin;
import win.sectorfive.viflcraftauth.api.database.User;
import win.sectorfive.viflcraftauth.api.event.events.PremiumLoginSwitchEvent;
import win.sectorfive.viflcraftauth.common.event.AuthenticPlayerBasedEvent;

public class AuthenticPremiumLoginSwitchEvent<P, S> extends AuthenticPlayerBasedEvent<P, S> implements PremiumLoginSwitchEvent<P, S> {
    public AuthenticPremiumLoginSwitchEvent(@Nullable User user, P player, LibreLoginPlugin<P, S> plugin) {
        super(user, player, plugin);
    }
}
