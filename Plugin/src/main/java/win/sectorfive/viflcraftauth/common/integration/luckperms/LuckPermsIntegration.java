/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package win.sectorfive.viflcraftauth.common.integration.luckperms;

import net.luckperms.api.LuckPermsProvider;
import win.sectorfive.viflcraftauth.common.AuthenticLibreLogin;

public class LuckPermsIntegration<P, S> {

    private final AuthorizedContext<P> authorizedContext;

    public LuckPermsIntegration(AuthenticLibreLogin<P, S> plugin) {
        authorizedContext = new AuthorizedContext<>(plugin);
        var contextMgr = LuckPermsProvider.get().getContextManager();
        contextMgr.registerCalculator(authorizedContext);
    }

    public void disable() {
        var contextMgr = LuckPermsProvider.get().getContextManager();
        contextMgr.unregisterCalculator(authorizedContext);
    }

}
