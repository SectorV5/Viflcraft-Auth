/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package win.sectorfive.viflcraftauth.common.database.connector;

import win.sectorfive.viflcraftauth.api.database.connector.DatabaseConnector;
import win.sectorfive.viflcraftauth.common.AuthenticViflcraftAuth;
import win.sectorfive.viflcraftauth.common.config.key.ConfigurationKey;

public abstract class AuthenticDatabaseConnector<E extends Exception, I> implements DatabaseConnector<E, I> {

    protected final AuthenticViflcraftAuth<?, ?> plugin;
    private final String prefix;
    protected boolean connected = true;

    public AuthenticDatabaseConnector(AuthenticViflcraftAuth<?, ?> plugin, String prefix) {
        this.plugin = plugin;
        this.prefix = prefix;
    }

    @Override
    public boolean connected() {
        return connected;
    }

    public <T> T get(ConfigurationKey<T> key) {
        var value = key.getter().apply(plugin.getConfiguration().getHelper(), prefix + key.key());
        return value == null ? key.defaultValue() : value;
    }

}
