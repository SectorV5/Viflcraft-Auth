/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package win.sectorfive.viflcraftauth.bungeecord;

import net.byteflux.libby.BungeeLibraryManager;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;
import win.sectorfive.viflcraftauth.api.provider.LibreLoginProvider;

public class BungeeCordBootstrap extends Plugin implements LibreLoginProvider<ProxiedPlayer, ServerInfo> {

    private BungeeCordLibreLogin libreLogin;

    @Override
    public void onLoad() {
        var libraryManager = new BungeeLibraryManager(this);

        getLogger().info("Loading libraries...");

        libraryManager.configureFromJSON();

        libreLogin = new BungeeCordLibreLogin(this);
    }

    @Override
    public void onEnable() {
        libreLogin.enable();
    }

    @Override
    public void onDisable() {
        libreLogin.disable();
    }

    @Override
    public BungeeCordLibreLogin getLibreLogin() {
        return libreLogin;
    }

}
