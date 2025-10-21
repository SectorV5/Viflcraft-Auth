/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package win.sectorfive.viflcraftauth.common.database;

import win.sectorfive.viflcraftauth.api.database.ReadWriteDatabaseProvider;
import win.sectorfive.viflcraftauth.api.database.connector.DatabaseConnector;
import win.sectorfive.viflcraftauth.common.AuthenticViflcraftAuth;

public abstract class AuthenticDatabaseProvider<C extends DatabaseConnector<?, ?>> implements ReadWriteDatabaseProvider {

    protected final C connector;
    protected final AuthenticViflcraftAuth<?, ?> plugin;

    protected AuthenticDatabaseProvider(C connector, AuthenticViflcraftAuth<?, ?> plugin) {
        this.connector = connector;
        this.plugin = plugin;
    }

    public void validateSchema() {
    }

}
